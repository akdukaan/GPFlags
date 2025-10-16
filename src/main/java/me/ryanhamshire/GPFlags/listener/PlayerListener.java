package me.ryanhamshire.GPFlags.listener;

import java.util.ArrayList;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import me.ryanhamshire.GPFlags.FlightManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.event.PlayerPostClaimBorderEvent;
import me.ryanhamshire.GPFlags.event.PlayerPreClaimBorderEvent;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;

/**
 * Purpose is
 */
public class PlayerListener implements Listener {

    private static final DataStore dataStore = GriefPrevention.instance.dataStore;

    @EventHandler(ignoreCancelled = true)
    private void onMove(PlayerMoveEvent event) {
        Location locTo = event.getTo();
        Location locFrom = event.getFrom();
        Player player = event.getPlayer();

        Set<Player> group = Util.getMovementGroup(player);
        if (flagsPreventMovement(locTo, locFrom, group)) {
            event.setCancelled(true);
            if (player.isGliding()) {
                player.setGliding(false);
                FlightManager.considerForFallImmunity(player);
            }
            Entity vehicleEntity = player.getVehicle();
            if (vehicleEntity instanceof Vehicle) {
                Util.breakVehicle((Vehicle) player.getVehicle(), locFrom);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onTeleport(PlayerTeleportEvent event) {
        Location locTo = event.getTo();
        Location locFrom = event.getFrom();
        Player player = event.getPlayer();
        Set<Player> group = Util.getMovementGroup(player);
        if (flagsPreventMovement(locTo, locFrom, group)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onMount(EntityMountEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        Player player = (Player) entity;
        Location from = player.getLocation();
        Location to = event.getMount().getLocation();
        Set<Player> group = Util.getMovementGroup(player);
        if (flagsPreventMovement(to, from, group)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onEnterVehicle(VehicleEnterEvent event) {
        Entity entity = event.getEntered();
        Vehicle vehicle = event.getVehicle();
        if (!(entity instanceof Player)) return;
        Player player = ((Player) entity);
        Location from = player.getLocation();
        Location to = vehicle.getLocation();
        if (flagsPreventMovement(to, from, player)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onExitVehicle(VehicleExitEvent event) {

        // gets the player who is exiting the vehicle
        if (!(event.getExited() instanceof Player)) return;
        Player player = (Player) event.getExited();

        // manage their flight
        FlightManager.managePlayerFlight(player, null, player.getLocation());
    }

    @EventHandler
    private void onEnterBed(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        Location from = player.getLocation();
        Location to = event.getBed().getLocation();
        if (flagsPreventMovement(to, from, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onLeaveBed(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        Location from = player.getLocation();
        Bukkit.getScheduler().runTaskLater(GPFlags.getInstance(), () -> {
            Location to = player.getLocation();
            if (flagsPreventMovement(to, from, player)) {
                player.teleport(from.add(0, 1, 0));
            }
        }, 1);
    }

    @EventHandler
    private void onClaimDelete(ClaimDeletedEvent event) {
        Claim claim = event.getClaim();
        for (Player player : Util.getPlayersIn(claim)) {
            Location location = player.getLocation();
            PlayerPostClaimBorderEvent borderEvent = new PlayerPostClaimBorderEvent(player, claim, claim.parent, location, location);
            Bukkit.getPluginManager().callEvent(borderEvent);
        }
    }

    @EventHandler
    private void onLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location spawn = player.getLocation();
        Claim cachedClaim = dataStore.getPlayerData(player.getUniqueId()).lastClaim;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(spawn,false, cachedClaim);
        PlayerPostClaimBorderEvent borderEvent = new PlayerPostClaimBorderEvent(event.getPlayer(), null, claim, null, spawn);
        Bukkit.getPluginManager().callEvent(borderEvent);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location spawn = event.getRespawnLocation();
        Claim cachedClaim = dataStore.getPlayerData(player.getUniqueId()).lastClaim;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(spawn,false, cachedClaim);
        PlayerPostClaimBorderEvent borderEvent = new PlayerPostClaimBorderEvent(event.getPlayer(), null, claim, null, spawn);
        Bukkit.getPluginManager().callEvent(borderEvent);
    }

    public static boolean flagsPreventMovement(Location to, Location from, Player player) {
        return flagsPreventMovement(to, from, Set.of(player));
    }

    /**
     * Takes in a movement and calls PreClaimBorderEvent if needed.
     * If the event was allowed, will call PostClaimBorderEvent
     * @param locTo
     * @param locFrom
     * @param players
     * @return If the movement was prevented
     */
    public static boolean flagsPreventMovement(Location locTo, Location locFrom, Set<Player> players) {
        if (locTo.getBlockX() == locFrom.getBlockX() &&
                locTo.getBlockY() == locFrom.getBlockY() &&
                locTo.getBlockZ() == locFrom.getBlockZ()) {
            return false;
        }

        if (players.isEmpty()) return false;
        Location locFromAdj = Util.getInBoundsLocation(locFrom);
        Location locToAdj = Util.getInBoundsLocation(locTo);
        Claim claimFrom = dataStore.getClaimAt(locFromAdj, false, null);
        Claim claimTo = dataStore.getClaimAt(locToAdj, false, null);
        if (claimTo == claimFrom) {
            // If both claims exist and are the same, there's no context change
            if (claimTo != null) {
                return false;
            }
            // If both claims are null and are the same world, there's no context change
            if (locFrom.getWorld() == locTo.getWorld()) {
                return false;
            }
        }

        // validate that the entire manifest is allowed to move to the location
        ArrayList<PlayerPreClaimBorderEvent> events = new ArrayList<>();
        for (Player passenger : players) {
            PlayerPreClaimBorderEvent event = new PlayerPreClaimBorderEvent(passenger, claimFrom, claimTo, locFromAdj, locToAdj);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return true;
            events.add(event);
        }

        // Now that we know everyone is allowed entry, lets call PlayerPostClaimBorderEvent
        for (PlayerPreClaimBorderEvent event : events) {
            Bukkit.getPluginManager().callEvent(new PlayerPostClaimBorderEvent(event));
        }
        return false;
    }
}
