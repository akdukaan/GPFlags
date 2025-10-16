package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFormEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoBlockForm extends FlagDefinition {

    public FlagDef_NoBlockForm(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(BlockFormEvent event) {
        Flag flag = this.getFlagInstanceAtLocation(event.getBlock().getLocation(), null);
        if (flag == null) return;
        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoBlockForm";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoBlockForm);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoBlockForm);
    }

}
