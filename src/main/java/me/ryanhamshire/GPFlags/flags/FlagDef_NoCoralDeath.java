package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFadeEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoCoralDeath extends FlagDefinition {

    public FlagDef_NoCoralDeath(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFade(BlockFadeEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        if (!Tag.CORALS.isTagged(material) &&
                !Tag.CORAL_PLANTS.isTagged(material) &&
                !Tag.CORAL_BLOCKS.isTagged(material) &&
                !Tag.WALL_CORALS.isTagged(material)) {
            return;
        }

        Flag flag = this.getFlagInstanceAtLocation(block.getLocation(), null);
        if (flag == null) return;

        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoCoralDeath";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoCoralDeath);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoCoralDeath);
    }

}
