package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CommandUnsetDefaultClaimFlag implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission("gpflags.command.unsetdefaultclaimflag")) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.NoCommandPermission, command.toString());
            return true;
        }
        if (args.length < 1) return false;

        String flagName = args[0];
        GPFlags gpflags = GPFlags.getInstance();
        FlagDefinition def = gpflags.getFlagManager().getFlagDefinitionByName(flagName);
        if (def == null) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.InvalidFlagDefName, Util.getAvailableFlags(sender));
            return true;
        }

        if (!sender.hasPermission("gpflags.flag." + def.getName())) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.NoFlagPermission, def.getName());
            return true;
        }

        SetFlagResult result = gpflags.getFlagManager().unSetFlag(FlagManager.DEFAULT_FLAG_ID, def);
        String color = result.isSuccess() ? TextMode.Success : TextMode.Err;
        if (result.isSuccess()) {
            MessagingUtil.sendMessage(sender, color, Messages.DefaultFlagUnSet);
            gpflags.getFlagManager().save();
        } else {
            MessagingUtil.sendMessage(sender, color, result.getMessage().getMessageID(), result.getMessage().getMessageParams());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return Util.flagTab(sender, args[0]);
        }
        return Collections.emptyList();
    }
}
