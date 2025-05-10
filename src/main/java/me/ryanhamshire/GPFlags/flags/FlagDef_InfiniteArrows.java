package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class FlagDef_InfiniteArrows extends FlagDefinition {

    public FlagDef_InfiniteArrows(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        ItemStack bow = event.getBow();
        if (bow == null) return;

        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Player)) return;
        Player player = (Player) livingEntity;

        Flag flag = this.getFlagInstanceAtLocation(player.getLocation(), player);
        if (flag == null) return;

        Entity projectile = event.getProjectile();
        if (!(projectile instanceof Arrow)) return;
        Arrow arrow = (Arrow) projectile;

        try {
            event.setConsumeArrow(false);
        } catch (NoSuchMethodError error) {
            event.setConsumeItem(false);
        }
        player.updateInventory();
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

    }

    @Override
    public String getName() {
        return "InfiniteArrows";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableInfiniteArrows);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableInfiniteArrows);
    }

}
