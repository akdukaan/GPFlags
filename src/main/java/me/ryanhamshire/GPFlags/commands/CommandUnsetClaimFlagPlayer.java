package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CommandUnsetClaimFlagPlayer implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission("gpflags.command.unsetclaimflagplayer")) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.NoCommandPermission, command.toString());

            return true;
        }
        if (args.length < 2) return false;
        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            MessagingUtil.sendMessage(sender, "<red>" + args[0] + " <grey>is not online");
            return false;
        }
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
        if (claim == null || !Util.canBuild(claim, player)) {
            MessagingUtil.sendMessage(sender, "<red>This player is not standing in a claim they own");
            return false;
        }

        GPFlags gpflags = GPFlags.getInstance();
        String flagName = args[1];
        FlagDefinition def = gpflags.getFlagManager().getFlagDefinitionByName(flagName);
        if (def == null) {
            MessagingUtil.sendMessage(sender, "<red> " + flagName + "<grey> is not a valid flag");
            return false;
        }
        if (!def.getFlagType().contains(FlagDefinition.FlagType.CLAIM)) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoFlagInClaim);
            return true;
        }

        SetFlagResult result = gpflags.getFlagManager().unSetFlag(claim, def);
        String color = result.isSuccess() ? TextMode.Success : TextMode.Err;
        MessagingUtil.sendMessage(sender, color, result.getMessage().getMessageID(), result.getMessage().getMessageParams());
        String message;
        if (result.isSuccess()) {
            gpflags.getFlagManager().save();
            message = "<grey>Flag <aqua>" + def.getName() + " <grey>successfully unset in <aqua>" + player.getName() + "<grey>'s claim.";

        } else {
            message = "<red>Flag <aqua> " + def.getName() + " <red>failed to unset in <aqua> " + player.getName() + "<red>'s claim.";
        }
        MessagingUtil.sendMessage(sender, message);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return null; // returns player list
        } else if (args.length == 2) {
            return Util.flagTab(sender, args[1]);
        }
        return Collections.emptyList();
    }
}
