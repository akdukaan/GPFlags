package me.ryanhamshire.GPFlags.listener;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Collection;

public class ClaimCreatedListener implements Listener {

    @EventHandler
    private void onClaimCreate(ClaimCreatedEvent event) {
        Collection<Flag> defaultFlags = GPFlags.getInstance().getFlagManager().getFlags(FlagManager.DEFAULT_FLAG_ID);
        for (Flag flag : defaultFlags) {
            flag.getFlagDefinition().onFlagSet(event.getClaim(), flag.parameters);
        }
    }
}
