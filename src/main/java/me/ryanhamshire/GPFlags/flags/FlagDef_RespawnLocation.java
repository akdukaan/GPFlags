package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlagDef_RespawnLocation extends FlagDefinition {

    private final ConcurrentHashMap<UUID, Location> respawnMap = new ConcurrentHashMap<>();

    public FlagDef_RespawnLocation(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location location = player.getLocation();

        Flag flag = this.getFlagInstanceAtLocation(location, player);
        if (flag == null) return;

        String[] params = flag.getParametersArray();
        World respawnWorld = Bukkit.getServer().getWorld(params[0]);
        Location respawnLocation = new Location(respawnWorld, Double.valueOf(params[1]), Double.valueOf(params[2]),
                Double.valueOf(params[3]), params.length < 5 ? 0 : Float.valueOf(params[4]),
                params.length < 6 ? 0 : Float.valueOf(params[5]));

        this.respawnMap.put(player.getUniqueId(), respawnLocation);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location respawnLocation = this.respawnMap.get(player.getUniqueId());
        if (respawnLocation != null) {
            event.setRespawnLocation(respawnLocation);
            this.respawnMap.remove(player.getUniqueId());
        }
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        String[] params = parameters.split(" ");

        if (params.length < 4) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.LocationRequired));
        }

        World world = Bukkit.getWorld(params[0]);
        if (world == null) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.WorldNotFound));
        }

        try {
            Double.valueOf(params[1]);
            Double.valueOf(params[2]);
            Double.valueOf(params[3]);
            if (params.length > 4) Float.valueOf(params[4]);
            if (params.length > 5) Float.valueOf(params[5]);
        } catch (NumberFormatException e) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.LocationRequired));
        }

        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public String getName() {
        return "RespawnLocation";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.SetRespawnLocation);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.UnSetRespawnLocation);
    }

}
