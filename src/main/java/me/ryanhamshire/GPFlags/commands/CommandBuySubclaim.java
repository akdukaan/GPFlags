package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandBuySubclaim implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        // Check that it was a player who ran the command
        if (!(sender instanceof Player)) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.PlayerOnlyCommand, command.toString());
            return true;
        }
        Player player = (Player) sender;

        // Make sure that the claim that the player is in has the flag set
        // and that we're in a subclaim
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        FlagDefinition def = GPFlags.getInstance().getFlagManager().getFlagDefinitionByName("BuySubclaim");
        Flag flag = def.getFlagInstanceAtLocation(player.getLocation(), null);
        if (flag == null || claim == null || claim.parent == null) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.CannotBuyTrustHere);
            return true;
        }
        // If the player already has build permission, error
        if (claim.getPermission(player.getUniqueId().toString()) == ClaimPermission.Build
                || player.getUniqueId().equals(claim.getOwnerID())) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.AlreadyHaveTrust);
            return true;
        }
        // If the flag doesn't have a cost set up, error
        if (flag.parameters == null || flag.parameters.isEmpty()) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.ProblemWithFlagSetup);
            return true;
        }
        // If the cost isn't a number, error
        double cost;
        try {
            cost = Double.parseDouble(flag.parameters);
        } catch (NumberFormatException e) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.ProblemWithFlagSetup);
            return true;
        }
        // Remove money from the player and give it to the claim owner
        if (!VaultHook.takeMoney(player, cost)) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.NotEnoughMoney);
            return true;
        }
        if (claim.getOwnerID() != null) {
            VaultHook.giveMoney(claim.getOwnerID(), cost);
        }
        
        // Give the player build trust and managetrust in the subclaim
        claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Build);
        claim.setPermission(player.getUniqueId().toString(), ClaimPermission.Manage);
        GriefPrevention.instance.dataStore.saveClaim(claim);
        
        // Remove the flag from the subclaim so it can't be re-bought
        FlagManager flagManager = GPFlags.getInstance().getFlagManager();
        SetFlagResult result = flagManager.setFlag(claim.getID().toString(), flag.getFlagDefinition(), false, sender);
        if (!result.isSuccess()) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.ProblemWithFlagSetup);
            return true;
        }
        flagManager.save();
        MessagingUtil.sendMessage(sender, TextMode.Info, Messages.BoughtTrust, flag.parameters);
        return true;
    }
}
