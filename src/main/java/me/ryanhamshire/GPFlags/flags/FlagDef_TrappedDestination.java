package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GriefPrevention.events.SaveTrappedPlayerEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;

import java.util.Arrays;
import java.util.List;

public class FlagDef_TrappedDestination extends FlagDefinition {

    public FlagDef_TrappedDestination(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onPlayerDeath(SaveTrappedPlayerEvent event) {
        Flag flag = this.getFlagInstanceAtLocation(event.getClaim().getLesserBoundaryCorner(), null);
        if (flag == null) return;

        String[] params = flag.getParametersArray();
        World world = Bukkit.getServer().getWorld(params[0]);
        Location location = new Location(world, Integer.valueOf(params[1]), Integer.valueOf(params[2]), Integer.valueOf(params[3]));

        event.setDestination(location);
    }

    @Override
    public String getName() {
        return "TrappedDestination";
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        String[] params = parameters.split(" ");

        if (params.length != 4) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.LocationRequired));
        }

        World world = Bukkit.getWorld(params[0]);
        if (world == null) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.WorldNotFound));
        }

        try {
            Integer.valueOf(params[1]);
            Integer.valueOf(params[2]);
            Integer.valueOf(params[3]);
        } catch (NumberFormatException e) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.LocationRequired));
        }

        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableTrappedDestination);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableTrappedDestination);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT);
    }
}
