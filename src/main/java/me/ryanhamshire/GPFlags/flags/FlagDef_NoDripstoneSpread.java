package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockSpreadEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoDripstoneSpread extends FlagDefinition {

    public FlagDef_NoDripstoneSpread(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpread(BlockSpreadEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        if (material != Material.POINTED_DRIPSTONE) return;

        Flag flag = this.getFlagInstanceAtLocation(block.getLocation(), null);
        if (flag == null) return;

        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoDripstoneSpread";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoDripstoneSpread);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoDripstoneSpread);
    }

}
