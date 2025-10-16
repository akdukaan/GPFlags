package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoMobSpawnsType extends FlagDefinition {

    private final String ALLOW_TARGET_TAG = "GPF_AllowTarget";

    public FlagDef_NoMobSpawnsType(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    private void onMobSpawn(CreatureSpawnEvent event) {
        Flag flag = this.getFlagInstanceAtLocation(event.getLocation(), null);
        if (flag == null) return;

        EntityType type = event.getEntityType();
        if (type == EntityType.PLAYER) return;
        if (type == EntityType.ARMOR_STAND) return;

        if (isNotAllowed(type, flag)) {
            CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
            if (reason == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) return;
            if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
                event.getEntity().setMetadata(this.ALLOW_TARGET_TAG, new FixedMetadataValue(GPFlags.getInstance(), Boolean.TRUE));
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onMobTarget(EntityTargetEvent event) {
        Entity target = event.getTarget();
        Entity damager = event.getEntity();

        if (target == null) return;

        Flag flag = this.getFlagInstanceAtLocation(target.getLocation(), null);
        if (flag == null) return;
        if (isNotAllowed(damager.getType(), flag)) {
            if (damager.hasMetadata(this.ALLOW_TARGET_TAG)) return;
            event.setCancelled(true);
            damager.remove();
        }
    }

    @EventHandler
    private void onMobDamage(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        Entity damager = event.getDamager();

        if (damager instanceof Player) return;
        if (!(damager instanceof LivingEntity)) return;
        if (!(target instanceof Player)) return;

        Flag flag = this.getFlagInstanceAtLocation(target.getLocation(), null);
        if (flag == null) return;
        if (isNotAllowed(damager.getType(), flag)) {
            if (damager.hasMetadata(this.ALLOW_TARGET_TAG)) return;
            event.setCancelled(true);
            damager.remove();
        }
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty()) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.MobTypeRequired));
        }
        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public String getName() {
        return "NoMobSpawnsType";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledNoMobSpawnsType, parameters.replace(";", ", "));
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledNoMobSpawnsType);
    }

    private boolean isNotAllowed(EntityType type, Flag flag) {
        for (String t : flag.parameters.split(";")) {
            if (t.equalsIgnoreCase(type.toString())) return true;
        }
        return false;
    }

}
