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

public class CommandSetDefaultClaimFlag implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!commandSender.hasPermission("gpflags.command.setdefaultclaimflag")) {
            MessagingUtil.sendMessage(commandSender, TextMode.Err, Messages.NoCommandPermission, command.toString());
            return true;
        }
        if (args.length < 1) return false;

        String flagName = args[0];

        GPFlags gpFlags = GPFlags.getInstance();
        FlagDefinition def = gpFlags.getFlagManager().getFlagDefinitionByName(flagName);
        if (def == null) {
            MessagingUtil.sendMessage(commandSender, TextMode.Err, Messages.InvalidFlagDefName, Util.getAvailableFlags(commandSender));
            return true;
        }

        if (!commandSender.hasPermission("gpflags.flag." + def.getName())) {
            MessagingUtil.sendMessage(commandSender, TextMode.Err, Messages.NoFlagPermission, def.getName());
            return true;
        }

        if (!def.getFlagType().contains(FlagDefinition.FlagType.DEFAULT)) {
            MessagingUtil.sendMessage(commandSender, TextMode.Err, Messages.NoFlagInDefault);
            return true;
        }

        String[] params = new String[args.length - 1];
        System.arraycopy(args, 1, params, 0, args.length - 1);

        SetFlagResult result = gpFlags.getFlagManager().setFlag(FlagManager.DEFAULT_FLAG_ID, def, true, commandSender, params);
        String color = result.isSuccess() ? TextMode.Success : TextMode.Err;
        if (result.isSuccess()) {
            MessagingUtil.sendMessage(commandSender, color, Messages.DefaultFlagSet);
            gpFlags.getFlagManager().save();
        } else {
            MessagingUtil.sendMessage(commandSender, color, result.getMessage().getMessageID(), result.getMessage().getMessageParams());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return Util.flagTab(commandSender, args[0]);
        } else if (args.length == 2) {
            return Util.paramTab(commandSender, args);
        }
        return Collections.emptyList();
    }
}
