package me.ryanhamshire.GPFlags;

import me.ryanhamshire.GPFlags.event.PlayerPostClaimBorderEvent;
import me.ryanhamshire.GPFlags.flags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;
import me.ryanhamshire.GriefPrevention.events.TrustChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class FlightManager implements Listener {
    private static final HashSet<Player> fallImmune = new HashSet<>();

    @EventHandler
    public void onClaimTransfer(ClaimTransferEvent event) {
        for (Player player : Util.getPlayersIn(event.getClaim())) {
            manageFlightLater(player, 1, player.getLocation());
        }
    }

    @EventHandler
    public void onTrustChanged(TrustChangedEvent event) {
        String identifier = event.getIdentifier();

        // If everyone, manage flight for everyone in affected claim(s)
        if (identifier.equalsIgnoreCase("public") || identifier.equalsIgnoreCase("all")) {
            Collection<Claim> claims = event.getClaims();
            for (Claim claim : claims) {
                for (Player player : Util.getPlayersIn(claim)) {
                    manageFlightLater(player, 1, player.getLocation());
                }
            }
            return;
        }

        // If a player, manage flight for the player
        try {
            UUID uuid = UUID.fromString(identifier);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                manageFlightLater(player, 1, player.getLocation());
            }
            return;
        } catch (IllegalArgumentException ignored) {}

        // Otherwise, it's a permission
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(identifier)) {
                manageFlightLater(player, 1, player.getLocation());
            }
        }
    }

    @EventHandler
    public void onEnterNewClaim(PlayerPostClaimBorderEvent event) {
        // Get the status at their old location
        Location locFrom = event.getLocFrom();
        Location locTo = event.getLocTo();
        Player player = event.getPlayer();
        Claim fromClaim = event.getClaimFrom();
        Claim toClaim = event.getClaimTo();
        Boolean oldFlightAllowedStatus = gpfAllowsFlight(player, locFrom, fromClaim);

        // A tick later, do a check on their new location and compare the two values
        Bukkit.getScheduler().runTaskLater(GPFlags.getInstance(), () -> {
            Boolean newFlightAllowedStatus = gpfAllowsFlight(player, locTo, toClaim);
            managePlayerFlight(player, oldFlightAllowedStatus, newFlightAllowedStatus);
        }, 1);
    }

    @EventHandler
    private void onFall(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = ((Player) e.getEntity());
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause != EntityDamageEvent.DamageCause.FALL) return;
        if (!fallImmune.contains(p)) return;
        e.setCancelled(true);
        fallImmune.remove(p);
    }

    /**
     * Runs a manage flight operation between the oldLocation now and the location that the player will be in ticks ticks
     * @param player
     * @param ticks Number of ticks to wait before calculating new flight allow status and managing flight.
     * @param oldLocation If provided, will be able to avoid running a manage flight operation if the new status after ticks ticks is the same
     */
    public static void manageFlightLater(@NotNull Player player, int ticks, @Nullable Location oldLocation) {
        if (oldLocation == null) {
            Bukkit.getScheduler().runTaskLater(GPFlags.getInstance(), () -> {
                managePlayerFlight(player, null, player.getLocation());
            }, ticks);
            return;
        }
        // if oldLocation is passed in, we want to calculate that value immediately
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim oldClaim = GriefPrevention.instance.dataStore.getClaimAt(oldLocation, false, playerData.lastClaim);
        Boolean oldFlightAllowedStatus = gpfAllowsFlight(player, oldLocation, oldClaim);
        Bukkit.getScheduler().runTaskLater(GPFlags.getInstance(), () -> {
            Claim newClaim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
            Boolean newFlightAllowedStatus = gpfAllowsFlight(player, player.getLocation(), newClaim);
            managePlayerFlight(player, oldFlightAllowedStatus, newFlightAllowedStatus);
        }, ticks);
    }

    /**
     * Compares the flight permission of the player at the two locations and manages flight if different
     * @param player
     * @param oldLocation
     * @param newLocation
     */
    public static void managePlayerFlight(@NotNull Player player, @Nullable Location oldLocation, @NotNull Location newLocation) {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(newLocation, false, playerData.lastClaim);

        Boolean flightAllowedAtNewLocation = gpfAllowsFlight(player, newLocation, claim);
        if (oldLocation == null) {
            if (flightAllowedAtNewLocation == null) {
                if (gpfManagesFlight(player)) {
                    turnOffFlight(player);
                }
                return;
            }
            if (flightAllowedAtNewLocation) {
                turnOnFlight(player);
            } else {
                turnOffFlight(player);
            }
            return;
        }

        Boolean flightAllowedAtOldLocation = gpfAllowsFlight(player, oldLocation, claim);
        managePlayerFlight(player, flightAllowedAtOldLocation, flightAllowedAtNewLocation);
    }


    /**
     * Checks if a flag is the reason that the player allows flight at a location
     * @param player
     * @param location
     * @param claim Claim at location. Helpful in case a claim is being deleted
     * @return
     */
    public static Boolean gpfAllowsFlight(Player player, Location location, Claim claim) {
        boolean manageFlight = gpfManagesFlight(player);
        if (manageFlight) {
            if (FlagDef_OwnerMemberFly.letPlayerFly(player, location, claim)) {
                return true;
            }
            if (FlagDef_OwnerFly.letPlayerFly(player, location, claim)) {
                return true;
            }
        }
        if (!FlagDef_NoFlight.letPlayerFly(player, location, claim)) {
            return false;
        }
        if (manageFlight) {
            if (FlagDef_PermissionFly.letPlayerFly(player, location, claim)) {
                return true;
            }
        }
        // we have no flight context set, so we need to compare the response for this method from both claims to determine if they should fly
        return null;
    }

    /**
     * If there's a difference in the two booleans, will set the player's flight to the new status
     * @param player
     * @param flightAllowedAtOldLocation Null means no flags to indicate
     * @param flightAllowedAtNewLocation Null means no flags to indicate
     */
    private static void managePlayerFlight(@NotNull Player player, @Nullable Boolean flightAllowedAtOldLocation, @Nullable Boolean flightAllowedAtNewLocation) {
        if (flightAllowedAtNewLocation == null) {
            if (Boolean.TRUE.equals(flightAllowedAtOldLocation)) {
                if (gpfManagesFlight(player)) {
                    turnOffFlight(player);
                }
            }
            return;
        }
        if (flightAllowedAtNewLocation == Boolean.TRUE) {
            if (gpfManagesFlight(player)) {
                turnOnFlight(player);
            }
            return;
        }
        if (flightAllowedAtNewLocation == Boolean.FALSE) {
            turnOffFlight(player);
        }
    }

    @EventHandler
    private void onFlyToggle(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        manageFlightLater(player, 1, null);
    }

    private static void turnOffFlight(@NotNull Player player) {
        if (!player.getAllowFlight()) return;
        MessagingUtil.sendMessage(player, TextMode.Err, Messages.CantFlyHere);
        player.setFlying(false);
        player.setAllowFlight(false);
        considerForFallImmunity(player);
    }

    public static void considerForFallImmunity(Player player) {
        Location location = player.getLocation();
        Block floor = getFloor(location.getBlock());
        if (location.getY() - floor.getY() >= 4) {
            fallImmune.add(player);
        }
    }

    /**
     * Gets the block where a player who fell from another block would land
     * @param block the starting block
     * @return The landing block
     */
    public static Block getFloor(Block block) {
        Material material = block.getType();
        if (material.isSolid()) return block;
        if (material == Material.WATER) return block;
        Location location = block.getLocation();
        if (location.getBlockY() <= Util.getMinHeight(location)) return block;
        return getFloor(block.getRelative(BlockFace.DOWN));
    }

    private static void turnOnFlight(Player player) {
        if (player.getAllowFlight()) return;
        player.setAllowFlight(true);
        MessagingUtil.sendMessage(player, TextMode.Success, Messages.EnterFlightEnabled);
        if (player.isGliding()) return;
        Material below = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
        if (below == Material.AIR) {
            player.setFlying(true);
        }
    }

    private static boolean gpfManagesFlight(Player player) {
        if (player.hasPermission("gpflags.bypass.fly")) return false;
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE) return false;
        if (mode == GameMode.SPECTATOR) return false;
        return true;
    }
}
