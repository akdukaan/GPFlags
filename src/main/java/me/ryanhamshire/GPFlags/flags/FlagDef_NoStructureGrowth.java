package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoStructureGrowth extends FlagDefinition {

    public FlagDef_NoStructureGrowth(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onGrowth(StructureGrowEvent event) {

        Flag flag = this.getFlagInstanceAtLocation(event.getLocation(), null);
        if (flag == null) return;

        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoStructureGrowth";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoGrowth);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoStructureGrowth);
    }

}
