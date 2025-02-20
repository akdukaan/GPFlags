package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.flags.FlagDef_ChangeBiome;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandSetClaimFlag implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        // Check perms
        if (!commandSender.hasPermission("gpflags.command.setclaimflag")) {
            MessagingUtil.sendMessage(commandSender, TextMode.Err, Messages.NoCommandPermission, command.toString());
            return true;
        }
        // Check player sender
        if (!(commandSender instanceof Player)) {
            MessagingUtil.sendMessage(commandSender, TextMode.Warn, Messages.PlayerOnlyCommand, command.toString());
            return true;
        }
        Player player = ((Player) commandSender);

        // Check that they provided a flag
        if (args.length < 1) return false;
        String flagName = args[0];

        // If they provided a nonexisting flag, show them the options
        GPFlags gpflags = GPFlags.getInstance();
        FlagDefinition def = gpflags.getFlagManager().getFlagDefinitionByName(flagName);
        if (def == null) {
            MessagingUtil.sendMessage(player, TextMode.Warn, Messages.InvalidFlagDefName, Util.getAvailableFlags(player));
            return true;
        }

        // Check perms for that specific flag
        if (!player.hasPermission("gpflags.flag." + flagName)) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoFlagPermission, flagName);
            return true;
        }

        // Check that the flag can be used in claims
        if (!def.getFlagType().contains(FlagDefinition.FlagType.CLAIM)) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoFlagInClaim);
            return true;
        }

        // Check that they are standing in a claim
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
        if (claim == null) {
            MessagingUtil.sendMessage(commandSender, TextMode.Err, Messages.StandInAClaim);
            return true;
        }

        // Check that they can set flags in the area
        if (!Util.canEdit(player, claim)) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
            return true;
        }

        String[] params = new String[args.length - 1];
        System.arraycopy(args, 1, params, 0, args.length - 1);

        // Prevent combining OwnerMemberFly with OwnerFly
        Collection<Flag> flags;
        flags = gpflags.getFlagManager().getFlags(claim.getID().toString());
        for (Flag flag : flags) {
            if (flagName.equalsIgnoreCase("OwnerFly")) {
                if (flag.getFlagDefinition().getName().equalsIgnoreCase("OwnerMemberFly")) {
                    MessagingUtil.sendMessage(player, TextMode.Warn, Messages.NoOwnerFlag);
                    return true;
                }
            }
            if (flagName.equalsIgnoreCase("OwnerMemberFly")) {
                if (flag.getFlagDefinition().getName().equalsIgnoreCase("OwnerFly")) {
                    MessagingUtil.sendMessage(player, TextMode.Warn, Messages.NoOwnerFlag);
                    return true;
                }
            }
        }

        // Check to see if biome is allowed for the player
        if (flagName.equalsIgnoreCase("ChangeBiome")) {
            if (args.length < 2) return false;
            FlagDef_ChangeBiome flagD = ((FlagDef_ChangeBiome) gpflags.getFlagManager().getFlagDefinitionByName("changebiome"));
            String biome = params[0].toUpperCase().replace(" ", "_");
            if (!flagD.changeBiome(commandSender, claim, biome)) return true;
        }

        // Check permissions for mob type
        if (flagName.equalsIgnoreCase("NoMobSpawnsType")) {
            if (params.length == 0) return false;
            for (String type : params[0].split(";")) {
                if (!player.hasPermission("gpflags.flag.nomobspawnstype." + type)) {
                    MessagingUtil.sendMessage(player, TextMode.Err, Messages.MobTypePerm, type);
                    return true;
                }
            }
        }

        // If they are trying to use subdivisions with an old GP version, deny it.
        Long claimID = claim.getID();
        if (claimID == null || claimID == -1) {
            MessagingUtil.sendMessage(player, TextMode.Err, Messages.UpdateGPForSubdivisionFlags);
            return true;
        }

        // Change the flag in the file storage
        SetFlagResult result = gpflags.getFlagManager().setFlag(claimID.toString(), def, true, commandSender, params);
        String color = result.isSuccess() ? TextMode.Success : TextMode.Err;
        MessagingUtil.sendMessage(player, color, result.getMessage().getMessageID(), result.getMessage().getMessageParams());
        if (result.isSuccess()) gpflags.getFlagManager().save();

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
