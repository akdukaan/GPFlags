package me.ryanhamshire.GPFlags;

import me.ryanhamshire.GPFlags.event.PlayerPostClaimBorderEvent;
import me.ryanhamshire.GPFlags.flags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;
import me.ryanhamshire.GriefPrevention.events.TrustChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * Manages flight given or taken away by flags.
 * <p>
 * Flight is reconciled against the player's current location rather than by comparing an old
 * and a new location: every trigger just asks "what should this player's flight be right now?"
 * Missed claim transitions are caught by {@link ClaimContextTracker}, which fires a
 * {@link PlayerPostClaimBorderEvent} that lands here.
 * </p>
 * <p>
 * GPFlags remembers (in the player's persistent data, so it survives relogs and restarts)
 * whether the flight a player has was given by a flag. When a player ends up somewhere with no
 * flight flags, flight is only taken away if GPFlags was the one that gave it, so flight from
 * other plugins or other sources is left alone.
 * </p>
 */
public class FlightManager implements Listener {
    private static final HashSet<Player> fallImmune = new HashSet<>();
    private static NamespacedKey grantedKey;

    private static NamespacedKey getGrantedKey() {
        if (grantedKey == null) {
            grantedKey = new NamespacedKey(GPFlags.getInstance(), "granted_flight");
        }
        return grantedKey;
    }

    @EventHandler
    public void onClaimTransfer(ClaimTransferEvent event) {
        reconcileLater(Util.getPlayersIn(event.getClaim()));
    }

    @EventHandler
    public void onTrustChanged(TrustChangedEvent event) {
        String identifier = event.getIdentifier();

        // If everyone, manage flight for everyone in affected claim(s)
        if (identifier.equalsIgnoreCase("public") || identifier.equalsIgnoreCase("all")) {
            for (Claim claim : event.getClaims()) {
                reconcileLater(Util.getPlayersIn(claim));
            }
            return;
        }

        // If a player, manage flight for the player
        try {
            UUID uuid = UUID.fromString(identifier);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                reconcileLater(player);
            }
            return;
        } catch (IllegalArgumentException ignored) {}

        // Otherwise, it's a permission
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(identifier)) {
                reconcileLater(player);
            }
        }
    }

    @EventHandler
    public void onEnterNewClaim(PlayerPostClaimBorderEvent event) {
        // A tick later so that teleports, respawns, and logins have finished moving the player
        reconcileLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onFlyToggle(PlayerToggleFlightEvent event) {
        // A player turning off flight from another plugin while standing in a flight claim
        // should get flight back from the flag.
        if (event.isFlying()) return;
        reconcileLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onGameModeChange(PlayerGameModeChangeEvent event) {
        // Switching to survival resets allowFlight, so a player in a flight claim should get it back.
        // Switching to creative/spectator hands flight over to the game mode.
        reconcileLater(event.getPlayer());
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
     * Runs {@link #reconcile(Player)} on the next tick
     */
    public static void reconcileLater(@NotNull Player player) {
        Bukkit.getScheduler().runTaskLater(GPFlags.getInstance(), () -> reconcile(player), 1);
    }

    /**
     * Runs {@link #reconcile(Player)} on the next tick for each player
     */
    public static void reconcileLater(@NotNull Collection<Player> players) {
        for (Player player : players) {
            reconcileLater(player);
        }
    }

    /**
     * Sets the player's flight to whatever flags say it should be at their current location
     */
    public static void reconcile(@NotNull Player player) {
        if (!player.isOnline()) return;
        Location location = player.getLocation();
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        reconcile(player, location, claim);
    }

    /**
     * Sets the player's flight to whatever flags say it should be at the location
     * @param player The player
     * @param location The player's current location
     * @param claim The claim at location, or null if none
     */
    public static void reconcile(@NotNull Player player, @NotNull Location location, @Nullable Claim claim) {
        // If the flight we gave is gone (another plugin took it, they changed game mode, etc.)
        // or we're no longer responsible for their flight, forget that we gave it.
        if (isGrantedByGpf(player) && (!player.getAllowFlight() || !gpfManagesFlight(player))) {
            setGrantedByGpf(player, false);
        }

        Boolean flightAllowed = gpfAllowsFlight(player, location, claim);
        if (Boolean.TRUE.equals(flightAllowed)) {
            // Flags give flight here, so we're responsible for it from now on, even if the player
            // already had flight from somewhere else when they arrived.
            turnOnFlight(player);
            setGrantedByGpf(player, true);
            return;
        }
        if (Boolean.FALSE.equals(flightAllowed)) {
            turnOffFlight(player);
            setGrantedByGpf(player, false);
            return;
        }
        // No flags say anything about flight here, so only take away flight that we gave
        if (isGrantedByGpf(player)) {
            turnOffFlight(player);
            setGrantedByGpf(player, false);
        }
    }

    /**
     * @deprecated Use {@link #reconcile(Player)}. The old location is no longer needed.
     */
    @Deprecated
    public static void managePlayerFlight(@NotNull Player player, @Nullable Location oldLocation, @NotNull Location newLocation) {
        reconcile(player);
    }

    /**
     * @deprecated Use {@link #reconcileLater(Player)}. The old location is no longer needed.
     */
    @Deprecated
    public static void manageFlightLater(@NotNull Player player, int ticks, @Nullable Location oldLocation) {
        Bukkit.getScheduler().runTaskLater(GPFlags.getInstance(), () -> reconcile(player), ticks);
    }

    /**
     * Checks what flags say about the player's flight at a location
     * @param player
     * @param location
     * @param claim Claim at location. Helpful in case a claim is being deleted
     * @return true if a flag gives flight, false if a flag denies flight, null if no flag has an opinion
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
        return null;
    }

    /**
     * @return If the player's current flight was given to them by a flag
     */
    public static boolean isGrantedByGpf(@NotNull Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        return pdc.has(getGrantedKey(), PersistentDataType.BYTE);
    }

    private static void setGrantedByGpf(@NotNull Player player, boolean granted) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (granted) {
            pdc.set(getGrantedKey(), PersistentDataType.BYTE, (byte) 1);
        } else {
            pdc.remove(getGrantedKey());
        }
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
