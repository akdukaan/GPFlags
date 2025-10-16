package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class FlagDef_AllowWitherDamage extends FlagDefinition {

    public FlagDef_AllowWitherDamage(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!isFromWither(event)) return;
        Entity attacked = event.getEntity();
        if (attacked.getType() == EntityType.PLAYER) return;

        Flag flag = this.getFlagInstanceAtLocation(attacked.getLocation(), null);
        if (flag == null) return;

        event.setCancelled(false);
    }

    public boolean isFromWither(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof WitherSkull) return true;
        if (event.getDamager() instanceof Wither) return true;
        if (event.getCause() == EntityDamageEvent.DamageCause.WITHER) return true;
        return false;
    }

    @Override
    public String getName() {
        return "AllowWitherDamage";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledAllowWitherDamage);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledAllowWitherDamage);
    }

}
