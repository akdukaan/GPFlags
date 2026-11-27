package me.ryanhamshire.GPFlags.listener;

import me.ryanhamshire.GPFlags.ClaimContextTracker;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClaimModifiedListener implements Listener {

    @EventHandler
    private void onClaimResize(ClaimModifiedEvent event) {
        Claim claimFrom = event.getFrom();
        World world = claimFrom.getGreaterBoundaryCorner().getWorld();

        // GriefPrevention fires this event before checking whether the resize overlaps another claim,
        // so the event's "to" claim can describe a resize that never happens. Instead of trusting it,
        // re-check where everyone in the world actually is a tick later, once the resize has been
        // applied (or rejected).
        ClaimContextTracker.syncWorldLater(world, 1);
    }
}
