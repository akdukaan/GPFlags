package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.util.Util;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoPlayerDamageByMonster extends FlagDefinition {

    public FlagDef_NoPlayerDamageByMonster(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    private void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!Util.isMonster(damager) && !(damager instanceof Projectile)) return;
        Entity victim = event.getEntity();
        if (!(victim instanceof Player)) return;
        Flag flag = this.getFlagInstanceAtLocation(victim.getLocation(), null);
        if (flag == null) return;
        if (damager instanceof Projectile) {
            Projectile projectile = ((Projectile) damager);
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) return;
            if (shooter instanceof Mob) {
                ((Mob) shooter).setTarget(null);
            }
        }

        event.setCancelled(true);
        if (damager instanceof Mob) {
            ((Mob) damager).setTarget(null);
        }
    }

    @EventHandler
    private void onPoison(EntityPotionEffectEvent event) {
        if (event.getCause() != EntityPotionEffectEvent.Cause.ATTACK) return;
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Flag flag = this.getFlagInstanceAtLocation(entity.getLocation(), null);
            if (flag == null) return;
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onTarget(EntityTargetLivingEntityEvent event) {
        LivingEntity target = event.getTarget();
        if (!(target instanceof Player)) return;
        if (!Util.isMonster(event.getEntity())) return;

        Flag flag = this.getFlagInstanceAtLocation(target.getLocation(), null);
        if (flag == null) return;

        event.setCancelled(true);
        event.setTarget(null);
    }

    @Override
    public String getName() {
        return "NoPlayerDamageByMonster";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoPlayerDamageByMonster);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoPlayerDamageByMonster);
    }

}
