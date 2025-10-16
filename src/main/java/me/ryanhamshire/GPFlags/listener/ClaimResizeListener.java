package me.ryanhamshire.GPFlags.listener;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.event.PlayerPostClaimBorderEvent;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClaimResizeListener implements Listener {

    @EventHandler
    private void onClaimResize(ClaimResizeEvent event) {
        Claim claimTo = event.getTo();
        Claim claimFrom = event.getFrom();
        World world = claimFrom.getGreaterBoundaryCorner().getWorld();
        for (Player player : world.getPlayers()) {
            Location loc = Util.getInBoundsLocation(player);

            // Resizing a claim and falling on the outside
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
            // Resizing a claim and falling on the inside
            if (claimTo.contains(loc, false, false) && !claimFrom.contains(loc, false, false)) {
                PlayerPostClaimBorderEvent borderEvent = new PlayerPostClaimBorderEvent(player, claimTo.parent, claimTo, loc, loc);
                Bukkit.getPluginManager().callEvent(borderEvent);
            }
        }

        // Deal with claims
        FlagManager manager = GPFlags.getInstance().getFlagManager();
        Flag keepLoaded = manager.getRawClaimFlag(claimFrom, "KeepLoaded");
        if (keepLoaded != null) {
            FlagDefinition def = manager.getFlagDefinitionByName("KeepLoaded");
            if (keepLoaded.getSet()) {
                def.onFlagUnset(claimFrom);
                def.onFlagSet(claimTo, null);
            }
        }

        Flag changeBiome = manager.getRawClaimFlag(claimFrom, "ChangeBiome");
        if (changeBiome != null) {
            FlagDefinition def = manager.getFlagDefinitionByName("ChangeBiome");
            if (changeBiome.getSet()) {
                def.onFlagUnset(claimFrom);
                def.onFlagSet(claimTo, changeBiome.getParameters());
            }
        }

    }
}
