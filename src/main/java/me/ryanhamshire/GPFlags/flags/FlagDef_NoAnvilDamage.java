package me.ryanhamshire.GPFlags.flags;

import com.destroystokyo.paper.event.block.AnvilDamagedEvent;
import me.ryanhamshire.GPFlags.*;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoAnvilDamage extends FlagDefinition {

    public FlagDef_NoAnvilDamage(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onAnvilDamage(AnvilDamagedEvent event) {
        Location location = event.getInventory().getLocation();
        if (location == null) return;

        HumanEntity human = event.getView().getPlayer();
        Player player = null;
        if (human instanceof Player) player = (Player) human;

        Flag flag = this.getFlagInstanceAtLocation(location, player);
        if (flag == null) return;
        event.setCancelled(true);

    }

    @Override
    public String getName() {
        return "NoAnvilDamage";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoAnvilDamage);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoAnvilDamage);
    }

}
