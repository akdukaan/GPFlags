package me.ryanhamshire.GPFlags.listener;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.ClaimContextTracker;
import me.ryanhamshire.GPFlags.flags.FlagDef_ChangeBiome;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClaimResizeListener implements Listener {

    @EventHandler
    private void onClaimResize(ClaimResizeEvent event) {
        Claim claimTo = event.getTo();
        Claim claimFrom = event.getFrom();
        World world = claimFrom.getGreaterBoundaryCorner().getWorld();

        // GriefPrevention fires this event before checking whether the resize overlaps another claim,
        // so the event's "to" claim can describe a resize that never happens. Instead of trusting it,
        // re-check where everyone in the world actually is a tick later, once the resize has been
        // applied (or rejected).
        ClaimContextTracker.syncWorldLater(world, 1);

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
        if (changeBiome != null && changeBiome.getSet()) {
            FlagDefinition def = manager.getFlagDefinitionByName("ChangeBiome");
            if (def instanceof FlagDef_ChangeBiome) {
                ((FlagDef_ChangeBiome) def).changeBiome(
                        Bukkit.getConsoleSender(),
                        claimTo,
                        changeBiome.getParameters()
                );
            }
        }

    }
}
