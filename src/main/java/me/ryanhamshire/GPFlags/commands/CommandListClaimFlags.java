package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.*;
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

import java.util.Collection;
import java.util.List;

public class CommandListClaimFlags implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player)) {
            MessagingUtil.sendMessage(commandSender, TextMode.Warn, Messages.PlayerOnlyCommand, command.toString());
            return true;
        }
        Player player = (Player) commandSender;

        GPFlags gpflags = GPFlags.getInstance();
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);

        if (!Util.shouldBypass(player, claim, "gpflags.command.listclaimflags")) {
            MessagingUtil.sendMessage(commandSender, TextMode.Err, Messages.NoCommandPermission, command.toString());
            return true;
        }

        Collection<Flag> flags;
        boolean flagsFound = false;
        StringBuilder builder1 = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();
        StringBuilder builder3 = new StringBuilder();
        if (claim != null) {
            // Subclaim or Claim
            flags = gpflags.getFlagManager().getFlags(claim.getID().toString());
            for (Flag flag : flags) {
                flagsFound = true;
                builder1.append(flag.getSet() ? "<green>" : "<red>").append(flag.getFlagDefinition().getName()).append(flag.parameters.length() > 0 ? "<grey>(" + flag.getFriendlyParameters() + "<grey>)" : "").append(" ");
            }

            // Claim if previous was subclaim, else none
            if (claim.parent != null) {
                flags = gpflags.getFlagManager().getFlags(claim.parent.getID().toString());
                for (Flag flag : flags) {
                    flagsFound = true;
                    builder2.append(flag.getSet() ? "<green>" : "<red>").append(flag.getFlagDefinition().getName()).append(flag.parameters.length() > 0 ? "<grey>(" + flag.getFriendlyParameters() + "<grey>)" : "").append(" ");
                }
            }

            // Default flags
            flags = gpflags.getFlagManager().getFlags(FlagManager.DEFAULT_FLAG_ID);
            for (Flag flag2 : flags) {
                flagsFound = true;
                builder3.append(flag2.getSet() ? "<green>" : "<red>").append(flag2.getFlagDefinition().getName()).append(flag2.parameters.length() > 0 ? "<grey>(" + flag2.getFriendlyParameters() + "<grey>)" : "").append(" ");
            }
        }

        // World
        StringBuilder builder4 = new StringBuilder();
        flags = gpflags.getFlagManager().getFlags(player.getWorld().getName());
        for (Flag flag3 : flags) {
            flagsFound = true;
            builder4.append(flag3.getSet() ? "<green>" : "<red>").append(flag3.getFlagDefinition().getName()).append(flag3.parameters.length() > 0 ? "<grey>(" + flag3.getFriendlyParameters() + "<grey>)" : "").append(" ");
        }

        // Server
        StringBuilder builder5 = new StringBuilder();
        flags = gpflags.getFlagManager().getFlags("everywhere");
        for (Flag flag4 : flags) {
            flagsFound = true;
            builder5.append(flag4.getSet() ? "<green>" : "<red>").append(flag4.getFlagDefinition().getName()).append(flag4.parameters.length() > 0 ? "<grey>(" + flag4.getFriendlyParameters() + "<grey>)" : "").append(" ");
        }

        if (builder1.length() > 0)
            MessagingUtil.sendMessage(player, TextMode.Info, Messages.FlagsClaim, builder1.toString());
        if (builder2.length() > 0)
            MessagingUtil.sendMessage(player, TextMode.Info, Messages.FlagsParent, builder2.toString());
        if (builder3.length() > 0)
            MessagingUtil.sendMessage(player, TextMode.Info, Messages.FlagsDefault, builder3.toString());
        if (builder4.length() > 0)
            MessagingUtil.sendMessage(player, TextMode.Info, Messages.FlagsWorld, builder4.toString());
        if (builder5.length() > 0)
            MessagingUtil.sendMessage(player, TextMode.Info, Messages.FlagsServer, builder5.toString());

        if (!flagsFound) {
            MessagingUtil.sendMessage(player, TextMode.Info, Messages.NoFlagsHere);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        return null;
    }
}
