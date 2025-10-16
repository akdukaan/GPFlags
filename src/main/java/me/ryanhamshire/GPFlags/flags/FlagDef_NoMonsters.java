package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.WorldSettings;
import me.ryanhamshire.GPFlags.util.Util;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoMonsters extends FlagDefinition {

    private final String ALLOW_TARGET_TAG = "GPF_AllowTarget";

    public FlagDef_NoMonsters(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!Util.isMonster(entity)) return;

        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

        WorldSettings settings = this.settingsManager.get(entity.getWorld());
        if (settings.noMonsterSpawnIgnoreSpawners && Util.isSpawnerReason(reason)) {
            entity.setMetadata(this.ALLOW_TARGET_TAG, new FixedMetadataValue(GPFlags.getInstance(), Boolean.TRUE));
            return;
        }

        Flag flag = this.getFlagInstanceAtLocation(event.getLocation(), null);
        if (flag == null) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        Entity target = event.getTarget();
        if (target == null) return;

        Entity entity = event.getEntity();
        if (!Util.isMonster(entity)) return;
        if (entity.hasMetadata(this.ALLOW_TARGET_TAG)) return;

        Flag flag = this.getFlagInstanceAtLocation(target.getLocation(), null);
        if (flag == null) return;

        event.setCancelled(true);
        entity.remove();
    }

    @EventHandler
    private void onMobDamage(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        Entity damager = event.getDamager();
        if (!Util.isMonster(damager)) return;
        if (damager instanceof Player) return;
        if (!(damager instanceof LivingEntity)) return;
        if (!(target instanceof Player)) return;
        if (damager.hasMetadata(this.ALLOW_TARGET_TAG)) return;

        Flag flag = this.getFlagInstanceAtLocation(target.getLocation(), null);
        if (flag == null) return;

        event.setCancelled(true);
        damager.remove();
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

    @Override
    public String getName() {
        return "NoMonsters";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.DisableMonsters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.EnableMonsters);
    }

}
