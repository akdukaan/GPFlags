package me.ryanhamshire.GPFlags.listener;

import me.ryanhamshire.GPFlags.event.PlayerPostClaimBorderEvent;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClaimModifiedListener implements Listener {

    @EventHandler
    private void onClaimResize(ClaimModifiedEvent event) {
        Claim claimTo = event.getTo();
        Claim claimFrom = event.getFrom();
        World world = claimFrom.getGreaterBoundaryCorner().getWorld();
        for (Player player : world.getPlayers()) {
            Location loc = Util.getInBoundsLocation(player);

            // Resizing a claim to be smaller and falling on the outside
            if (!claimTo.contains(loc, false, false) && claimFrom.contains(loc, false, false)) {
                Claim parent = claimFrom.parent;
                PlayerPostClaimBorderEvent borderEvent;
                // Falling in the parent claim
                if (parent != null && parent.contains(loc, false, false)) {
                    borderEvent = new PlayerPostClaimBorderEvent(player, claimFrom, parent, loc, loc);
                } else {
                    // Falling in the non-claim
                    borderEvent = new PlayerPostClaimBorderEvent(player, claimFrom, null, loc, loc);
                }
                Bukkit.getPluginManager().callEvent(borderEvent);
                return;
            }
            // Resizing a claim to be larger and falling on the inside
            if (claimTo.contains(loc, false, false) && !claimFrom.contains(loc, false, false)) {
                PlayerPostClaimBorderEvent borderEvent = new PlayerPostClaimBorderEvent(player, claimTo.parent, claimTo, loc, loc);
                Bukkit.getPluginManager().callEvent(borderEvent);
                return;
            }
        }
    }
}
