package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import com.magmaguy.elitemobs.api.EliteMobSpawnEvent;


import java.util.Arrays;
import java.util.List;

public class FlagDef_NoEliteMobSpawns extends FlagDefinition {

    public FlagDef_NoEliteMobSpawns(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onEntitySpawn(EliteMobSpawnEvent event) {
        Entity e = event.getEntity();
        if (e == null) return;
        Flag flag = this.getFlagInstanceAtLocation(e.getLocation(), null);
        if (flag == null) return;
        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "NoEliteMobSpawns";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoEliteMobSpawns);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoEliteMobSpawns);
    }

}
