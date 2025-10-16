package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.Util;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class FlagDef_NoMobSpawns extends FlagDefinition {

    public FlagDef_NoMobSpawns(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        EntityType type = event.getEntityType();
        if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND) return;

        SpawnReason reason = event.getSpawnReason();
        if (reason == SpawnReason.SLIME_SPLIT) return;
        WorldSettings settings = this.settingsManager.get(event.getEntity().getWorld());
        if (settings.noMonsterSpawnIgnoreSpawners && Util.isSpawnerReason(reason)) return;

        Flag flag = this.getFlagInstanceAtLocation(event.getLocation(), null);
        if (flag == null) return;

        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoMobSpawns";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.DisableMobSpawns);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.EnableMobSpawns);
    }

}
