package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoFireDamage extends FlagDefinition {

    public FlagDef_NoFireDamage(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFireSpread(BlockBurnEvent e) {
        Block fire = e.getBlock();
        Flag flag = this.getFlagInstanceAtLocation(fire.getLocation(), null);
        if (flag == null) return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockIgnite(BlockIgniteEvent e) {
        Block fire = e.getBlock();
        Flag flag = this.getFlagInstanceAtLocation(fire.getLocation(), null);
        if (flag == null) return;
        if (e.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) return;

        e.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoFireDamage";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoFireDamage);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoFireDamage);
    }

}
