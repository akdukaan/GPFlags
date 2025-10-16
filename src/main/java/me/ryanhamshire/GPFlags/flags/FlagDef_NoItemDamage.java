package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemDamageEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoItemDamage extends FlagDefinition {

    public FlagDef_NoItemDamage(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    private void onItemDamage(PlayerItemDamageEvent event) {
        Flag flag = this.getFlagInstanceAtLocation(event.getPlayer().getLocation(), null);
        if (flag == null) return;
        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoItemDamage";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledNoItemDamage);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledNoItemDamage);
    }

}
