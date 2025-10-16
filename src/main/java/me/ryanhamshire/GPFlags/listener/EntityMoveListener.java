package me.ryanhamshire.GPFlags.listener;

import io.papermc.paper.event.entity.EntityMoveEvent;
import me.ryanhamshire.GPFlags.util.Util;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;

public class EntityMoveListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    private void onMove(EntityMoveEvent event) {
        // Check if it'll be allowed
        Set<Player> group = Util.getMovementGroup(event.getEntity());
        Location locTo = event.getTo();
        Location locFrom = event.getFrom();
        if (PlayerListener.flagsPreventMovement(locTo, locFrom, group)) {
            event.setCancelled(true);
        }
    }
}