package me.ryanhamshire.GPFlags.listener;

import me.ryanhamshire.GPFlags.event.PlayerPostClaimBorderEvent;
import me.ryanhamshire.GPFlags.event.PlayerPreClaimBorderEvent;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.ArrayList;
import java.util.Set;

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

    @EventHandler
    private void onMount(VehicleEnterEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntered();
        Vehicle vehicle = event.getVehicle();
        if (entity instanceof Player) {
            Player player = ((Player) entity);
            Location from = player.getLocation();
            Location to = vehicle.getLocation();
            if (flagsPreventMovement(to, from, Set.of(player))) {
                event.setCancelled(true);
            }
        }
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

    @EventHandler
    private void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location spawn = event.getRespawnLocation();
        Claim cachedClaim = dataStore.getPlayerData(player.getUniqueId()).lastClaim;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(spawn,false, cachedClaim);
        PlayerPostClaimBorderEvent borderEvent = new PlayerPostClaimBorderEvent(event.getPlayer(), null, claim, null, spawn);
        Bukkit.getPluginManager().callEvent(borderEvent);
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

        Location locFromAdj = Util.getInBoundsLocation(locFrom);
        Location locToAdj = Util.getInBoundsLocation(locTo);
        if (players.isEmpty()) return false;
        Player rando = (Player) players.toArray()[0];
        if (rando == null) return false;
        Claim lastClaim = dataStore.getPlayerData(rando.getUniqueId()).lastClaim;
        Claim claimFrom = dataStore.getClaimAt(locFromAdj, false, lastClaim);
        Claim claimTo = dataStore.getClaimAt(locToAdj, false, null);
        if (claimTo == claimFrom) return false;

        ArrayList<PlayerPreClaimBorderEvent> events = new ArrayList<>();
        for (Player passenger : players) {
            PlayerPreClaimBorderEvent event = new PlayerPreClaimBorderEvent(passenger, claimFrom, claimTo, locFromAdj, locToAdj);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return true;
            events.add(event);
        }

        // Now that we know everyone is allowed entry, lets call postclaimborderevent
        for (PlayerPreClaimBorderEvent event : events) {
            Bukkit.getPluginManager().callEvent(new PlayerPostClaimBorderEvent(event));
        }
        return false;
    }
}
