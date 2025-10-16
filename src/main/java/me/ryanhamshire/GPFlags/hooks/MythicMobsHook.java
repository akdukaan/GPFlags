package me.ryanhamshire.GPFlags.hooks;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import org.bukkit.entity.Entity;

public class MythicMobsHook {

    public static boolean isMythicMob(Entity entity) {
        try {
            BukkitAPIHelper mythicMobsAPI = new BukkitAPIHelper();
            return mythicMobsAPI.isMythicMob(entity);
        } catch (NoClassDefFoundError error) {
            return false;
        }
    }
}
