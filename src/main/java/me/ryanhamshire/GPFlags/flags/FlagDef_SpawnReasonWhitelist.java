package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_SpawnReasonWhitelist extends FlagDefinition {

    public FlagDef_SpawnReasonWhitelist(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Flag flag = this.getFlagInstanceAtLocation(event.getLocation(), null);
        if (flag == null) return;
        for (String string : flag.getParametersArray()) {
            CreatureSpawnEvent.SpawnReason reason;
            try {
                reason = CreatureSpawnEvent.SpawnReason.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return;
            }
            if (reason != event.getSpawnReason()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty()) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.SpecifySpawnReason));
        }
        for (String s : parameters.split(" ")) {
            try {
                CreatureSpawnEvent.SpawnReason.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return new SetFlagResult(false, new MessageSpecifier(Messages.NotValidSpawnReason, s));
            }
        }
        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public String getName() {
        return "SpawnReasonWhitelist";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledSpawnReasonWhitelist, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledSpawnReasonWhitelist);
    }


}
