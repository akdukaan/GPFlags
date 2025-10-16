package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoMobDamage extends FlagDefinition {

    public FlagDef_NoMobDamage(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        // check flag
        Entity entity = event.getEntity();
        Flag flag = this.getFlagInstanceAtLocation(entity.getLocation(), null);
        if (flag == null) return;

        // fix for GP discussion https://github.com/TechFortress/GriefPrevention/issues/1481
        DamageCause cause = event.getCause();
        if (event.getDamage() == 0 && cause == DamageCause.CUSTOM) return;

        // Always allow cramming damage
        if (cause == DamageCause.CRAMMING) return;

        // Always allow attacks to players
        if (entity instanceof Player) return;

        // Always allow attacks from players
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent event2 = (EntityDamageByEntityEvent) event;
            Entity attacker = event2.getDamager();
            if (attacker.getType() == EntityType.PLAYER) return;
            if (attacker instanceof Projectile) {
                ProjectileSource source = ((Projectile) attacker).getShooter();
                if (source instanceof Player) return;
            }
        }

        if (entity instanceof LivingEntity) {
            event.setCancelled(true);
        }
    }

    @Override
    public String getName() {
        return "NoMobDamage";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.DisableMobDamage);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.EnableMobDamage);
    }

}
