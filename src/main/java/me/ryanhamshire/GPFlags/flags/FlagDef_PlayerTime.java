package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlagDef_PlayerTime extends PlayerMovementFlagDefinition implements Listener {

    public FlagDef_PlayerTime(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onChangeClaim(Player player, Location from, Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        // Reset the time if moving from enabled to disabled
        if (flagTo == null && flagFrom != null) {
            player.resetPlayerTime();
            return;
        }

        // Set time to new flag if exists
        if (flagTo == null) return;
        setPlayerTime(player, flagTo);
    }

    public void setPlayerTime(Player player, @NotNull Flag flag) {
        String time = flag.parameters;
        if (time.equalsIgnoreCase("day")) {
            player.setPlayerTime(0, false);
        } else if (time.equalsIgnoreCase("noon")) {
            player.setPlayerTime(6000, false);
        } else if (time.equalsIgnoreCase("night")) {
            player.setPlayerTime(12566, false);
        } else if (time.equalsIgnoreCase("midnight")) {
            player.setPlayerTime(18000, false);
        }
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty()) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.PlayerTimeRequired));
        }
        if (!parameters.equalsIgnoreCase("day") && !parameters.equalsIgnoreCase("noon") &&
                !parameters.equalsIgnoreCase("night") && !parameters.equalsIgnoreCase("midnight")) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.PlayerTimeRequired));
        }
        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public String getName() {
        return "PlayerTime";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.PlayerTimeSet, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.PlayerTimeUnSet);
    }

}
