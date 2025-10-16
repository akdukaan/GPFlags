package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class FlagDef_ExitMessage extends PlayerMovementFlagDefinition {

    private final String prefix;

    public FlagDef_ExitMessage(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
        this.prefix = plugin.getFlagsDataStore().getMessage(Messages.EnterExitPrefix);
    }

    @Override
    public void onChangeClaim(Player player, Location lastLocation, Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        if (flagFrom == null) return;
        // moving to different claim with the same message
        if (flagTo != null && flagTo.parameters.equals(flagFrom.parameters)) return;
        String message = flagFrom.parameters;
        if (claimFrom != null) {
            String ownerName = claimFrom.getOwnerName();
            if (ownerName != null) {
                message = message.replace("%owner%", ownerName);
            }
        }
        message = message.replace("%name%", player.getName());
        MessagingUtil.sendMessage(player, TextMode.Info + prefix + message);
    }

    @Override
    public String getName() {
        return "ExitMessage";
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty()) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.MessageRequired));
        }

        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.AddedExitMessage, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.RemovedExitMessage);
    }

}
