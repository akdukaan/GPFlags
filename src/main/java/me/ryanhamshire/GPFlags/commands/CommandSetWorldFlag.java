package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandSetWorldFlag implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission("gpflags.command.setworldflag")) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.NoCommandPermission, command.toString());
            return true;
        }
        if (args.length < 2) return false;

        World world = Bukkit.getWorld(args[0]);
        if (world == null) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.WorldNotFound, args[0]);
            return true;
        }

        String flagName = args[1];
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

        if (!def.getFlagType().contains(FlagDefinition.FlagType.WORLD)) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.NoFlagInWorld);
            return true;
        }

        String[] params = new String[args.length - 2];
        System.arraycopy(args, 2, params, 0, args.length - 2);

        SetFlagResult result = gpflags.getFlagManager().setFlag(world.getName(), def, true, sender, params);
        String color = result.isSuccess() ? TextMode.Success : TextMode.Err;
        if (result.isSuccess()) {
            MessagingUtil.sendMessage(sender, color, Messages.WorldFlagSet);
            gpflags.getFlagManager().save();
        } else {
            MessagingUtil.sendMessage(sender, color, result.getMessage().getMessageID(), result.getMessage().getMessageParams());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> worlds = new ArrayList<>();
            Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
            return StringUtil.copyPartialMatches(args[0], worlds, new ArrayList<>());
        } else if (args.length == 2) {
            return Util.flagTab(commandSender, args[1]);
        } else if (args.length == 3) {
            return Util.paramTab(commandSender, args);
        }
        return Collections.emptyList();
    }
}
