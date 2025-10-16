package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.WorldSettings;
import me.ryanhamshire.GPFlags.util.Util;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class FlagDef_NoMonsterSpawns extends FlagDefinition {

    public FlagDef_NoMonsterSpawns(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        Flag flag = this.getFlagInstanceAtLocation(event.getLocation(), null);
        if (flag == null) return;

        LivingEntity entity = event.getEntity();
        if (!Util.isMonster(entity)) return;

        SpawnReason reason = event.getSpawnReason();
        if (reason == SpawnReason.SLIME_SPLIT) return;

        WorldSettings settings = this.settingsManager.get(event.getEntity().getWorld());
        if (settings.noMonsterSpawnIgnoreSpawners && Util.isSpawnerReason(reason)) return;

        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoMonsterSpawns";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.DisableMonsterSpawns);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.EnableMonsterSpawns);
    }

}
