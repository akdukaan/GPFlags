package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class FlagDef_BuyAccessTrust extends PlayerMovementFlagDefinition {

    public FlagDef_BuyAccessTrust(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public String getName() {
        return "BuyAccessTrust";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableBuyAccessTrust, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableBuyAccessTrust);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Collections.singletonList(FlagType.CLAIM);
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty())
            return new SetFlagResult(false, new MessageSpecifier(Messages.CostRequired));

        try {
            double cost = Double.parseDouble(parameters);
            if (cost < 0) {
                return new SetFlagResult(false, new MessageSpecifier(Messages.CostRequired));
            }
        } catch (NumberFormatException e) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.CostRequired));
        }

        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public void onChangeClaim(Player player, Location lastLocation, Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        if (flagTo == null) return;

        if (claimTo.getPermission(player.getUniqueId().toString()) == ClaimPermission.Access) return;
        if (claimTo.getPermission(player.getUniqueId().toString()) == ClaimPermission.Build) return;
        if (claimTo.getPermission(player.getUniqueId().toString()) == ClaimPermission.Inventory) return;
        if (player.getUniqueId().equals(claimTo.getOwnerID())) return;
        MessagingUtil.sendMessage(player, TextMode.Info, Messages.AccessTrustPrice, flagTo.parameters);
    }

}
