package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;


public class FlagDef_NoCropTrampling extends FlagDefinition {

    public FlagDef_NoCropTrampling(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        // Check that the event is a crop trample
        Block block = event.getBlock();
        if (block.getType() != Material.FARMLAND) return;

        // Check that the flag is set
        Flag flag = this.getFlagInstanceAtLocation(block.getLocation(), null);
        if (flag == null) return;

        // Prevent the trample
        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoCropTrampling";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoCropTrampling);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoCropTrampling);
    }

}
