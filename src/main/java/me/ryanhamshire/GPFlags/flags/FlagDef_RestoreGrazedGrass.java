package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.SheepRegrowWoolEvent;


public class FlagDef_RestoreGrazedGrass extends FlagDefinition {

    public FlagDef_RestoreGrazedGrass(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        // Check that a sheep is doing the action
        if (event.getEntityType() != EntityType.SHEEP) return;

        // Check that the block was grass and will turn into dirt
        // Check that the event is a crop trample
        Block block = event.getBlock();
        if (block.getType() != Material.GRASS_BLOCK) return;
        if (event.getTo() != Material.DIRT) return;

        // Check that the flag is set
        Flag flag = this.getFlagInstanceAtLocation(block.getLocation(), null);
        if (flag == null) return;

        // Prevent the eating and regrow the wool
        event.setCancelled(true);
        Sheep sheep = (Sheep) event.getEntity();
        SheepRegrowWoolEvent regrowEvent = new SheepRegrowWoolEvent(sheep);
        Bukkit.getPluginManager().callEvent(regrowEvent);
    }

    @Override
    public String getName() {
        return "RestoreGrazedGrass";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledRestoreGrazedGrass);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledRestoreGrazedGrass);
    }

}
