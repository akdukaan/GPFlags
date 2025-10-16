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

public class FlagDef_NoFireSpread extends FlagDefinition {

    public FlagDef_NoFireSpread(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFireSpread(BlockSpreadEvent e) {
        Block fire = e.getSource();
        if (fire.getType() != Material.FIRE) return;

        Flag flag = this.getFlagInstanceAtLocation(fire.getLocation(), null);
        if (flag == null) return;

        e.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoFireSpread";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledNoFireSpread);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledNoFireSpread);
    }

}
