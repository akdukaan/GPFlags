package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;


public class FlagDef_EnterPlayerCommand extends PlayerMovementFlagDefinition {

    public FlagDef_EnterPlayerCommand(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onChangeClaim(Player player, Location lastLocation, Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        if (flagTo == null) return;
        // moving to different claim with the same params
        if (flagFrom != null && flagFrom.parameters.equals(flagTo.parameters)) return;

        executeFlagCommands(flagTo, player, claimTo);
    }

    public void executeFlagCommands(Flag flag, Player player, Claim claim) {
        String commandLinesString = flag.parameters.replace("%name%", player.getName()).replace("%uuid%", player.getUniqueId().toString());
        if (claim != null) {
            String ownerName = claim.getOwnerName();
            if (ownerName != null) {
                commandLinesString = commandLinesString.replace("%owner%", ownerName);
            }
        }
        String[] commandLines = commandLinesString.split(";");
        for (String commandLine : commandLines) {
            MessagingUtil.logFlagCommands("Entrance command: " + commandLine);
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), commandLine);
        }
    }

    @Override
    public String getName() {
        return "EnterPlayerCommand";
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty()) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.PlayerCommandRequired));
        }

        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.AddedEnterCommand, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.RemovedEnterCommand);
    }

}
