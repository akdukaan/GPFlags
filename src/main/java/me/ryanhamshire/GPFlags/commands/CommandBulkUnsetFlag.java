package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class CommandBulkUnsetFlag implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        // Check perms
        if (!commandSender.hasPermission("gpflags.command.bulkunsetflag")) {
            MessagingUtil.sendMessage(commandSender, TextMode.Err, Messages.NoCommandPermission, command.toString());
            return true;
        }

        // Check that they provided a player and flag
        if (args.length < 2) return false;
        String playerName = args[0];
        String flagName = args[1];

        // If they provided a nonexisting flag, show them the options
        GPFlags gpflags = GPFlags.getInstance();
        FlagDefinition def = gpflags.getFlagManager().getFlagDefinitionByName(flagName);
        if (def == null) {
            MessagingUtil.sendMessage(commandSender, TextMode.Warn, Messages.InvalidFlagDefName, Util.getAvailableFlags(commandSender));
            return true;
        }

        // Check that the flag can be used in claims
        if (!def.getFlagType().contains(FlagDefinition.FlagType.CLAIM)) {
            MessagingUtil.sendMessage(commandSender, TextMode.Err, Messages.NoFlagInClaim);
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        Vector<Claim> playerClaims = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId()).getClaims();
        for (Claim claim : playerClaims) {
            SetFlagResult result = gpflags.getFlagManager().unSetFlag(claim.getID().toString(), def);
            if (result.isSuccess()) gpflags.getFlagManager().save();
            String color = result.isSuccess() ? TextMode.Success : TextMode.Err;
            MessagingUtil.sendMessage(commandSender, color, result.getMessage().getMessageID(), result.getMessage().getMessageParams());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 2) {
            return Util.flagTab(commandSender, args[1]);
        } else if (args.length == 3) {
            return Util.paramTab(commandSender, args);
        } else if (args.length > 3) {
            return Collections.emptyList();
        }
        return null;
    }
}
