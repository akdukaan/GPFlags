package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CommandBuyContainerTrust implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(sender instanceof Player)) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.PlayerOnlyCommand, command.toString());
            return true;
        }
        Player player = (Player) sender;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        if (claim == null) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.CannotBuyTrustHere);
            return true;
        }

        Collection<Flag> flags = GPFlags.getInstance().getFlagManager().getFlags(claim.getID().toString());
        for (Flag flag : flags) {
            if (flag.getFlagDefinition().getName().equalsIgnoreCase("BuyContainerTrust")) {
                if (claim.getPermission(player.getUniqueId().toString()) == ClaimPermission.Inventory  ||
                        claim.getPermission(player.getUniqueId().toString()) == ClaimPermission.Build ||
                        player.getUniqueId().equals(claim.getOwnerID())) {
                    MessagingUtil.sendMessage(sender, TextMode.Err, Messages.AlreadyHaveTrust);
                    return true;
                }
                if (flag.parameters == null || flag.parameters.isEmpty()) {
                    MessagingUtil.sendMessage(sender, TextMode.Err, Messages.ProblemWithFlagSetup);
                    return true;
                }
                double cost;
                try {
                    cost = Double.parseDouble(flag.parameters);
                } catch (NumberFormatException e) {
                    MessagingUtil.sendMessage(sender, TextMode.Err, Messages.ProblemWithFlagSetup);
                    return true;
                }
                if (!VaultHook.takeMoney(player, cost)) {
                    MessagingUtil.sendMessage(sender, TextMode.Err, Messages.NotEnoughMoney);
                    return true;
                }
                if (claim.getOwnerID() != null) {
                    VaultHook.giveMoney(claim.getOwnerID(), cost);
                }
                claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Inventory);
                GriefPrevention.instance.dataStore.saveClaim(claim);
                MessagingUtil.sendMessage(sender, TextMode.Info, Messages.BoughtTrust, flag.parameters);
                return true;
            }
        }
        MessagingUtil.sendMessage(sender, TextMode.Err, Messages.CannotBuyTrustHere);
        return true;
    }
}
