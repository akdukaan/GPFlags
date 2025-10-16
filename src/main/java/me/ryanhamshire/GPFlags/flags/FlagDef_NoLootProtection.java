package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GriefPrevention.events.ProtectDeathDropsEvent;
import org.bukkit.event.EventHandler;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoLootProtection extends FlagDefinition {

    public FlagDef_NoLootProtection(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onPlayerDeath(ProtectDeathDropsEvent event) {
        if (event.getClaim() != null) {
            Flag flag = this.getFlagInstanceAtLocation(event.getClaim().getLesserBoundaryCorner(), null);
            if (flag == null) return;

            event.setCancelled(true);
        }
    }

    @Override
    public String getName() {
        return "NoLootProtection";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoLootProtection);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoLootProtection);
    }

}
