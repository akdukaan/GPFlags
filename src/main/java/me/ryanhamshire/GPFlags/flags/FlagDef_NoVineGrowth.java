package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockSpreadEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoVineGrowth extends FlagDefinition {

    public FlagDef_NoVineGrowth(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrowth(BlockSpreadEvent event) {
        Block block = event.getBlock();

        Flag flag = this.getFlagInstanceAtLocation(block.getLocation(), null);
        if (flag == null) return;

        Material newType = event.getSource().getType();
        if (newType == Material.VINE || newType == Material.GLOW_LICHEN || newType == Material.WEEPING_VINES || newType == Material.TWISTING_VINES || newType == Material.CAVE_VINES || newType == Material.CAVE_VINES_PLANT) {
            event.setCancelled(true);
        }
    }

    @Override
    public String getName() {
        return "NoVineGrowth";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoVineGrowth);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoVineGrowth);
    }

}
