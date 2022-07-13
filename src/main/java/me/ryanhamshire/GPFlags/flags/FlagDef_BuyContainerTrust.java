package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collections;
import java.util.List;

public class FlagDef_BuyContainerTrust extends PlayerMovementFlagDefinition {

    public FlagDef_BuyContainerTrust(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public String getName() {
        return "BuyContainerTrust";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableBuyContainerTrust, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableBuyContainerTrust);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Collections.singletonList(FlagType.CLAIM);
    }

    @Override
    public SetFlagResult validateParameters(String parameters) {
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
    public boolean allowMovement(Player player, Location lastLocation, Location to, Claim claimFrom, Claim claimTo) {
        Flag flag = this.getFlagInstanceAtLocation(to, player);
        if (flag == null) return true;

        if (claimTo == null) return true;
        if (claimTo.getPermission(player.getUniqueId().toString()) == ClaimPermission.Inventory) return true;
        Util.sendMessage(player, TextMode.Info, Messages.ContainerTrustPrice, flag.parameters);

        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Flag flag = this.getFlagInstanceAtLocation(player.getLocation(), player);
        if (flag == null) return;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        if (claim.getPermission(e.getPlayer().getUniqueId().toString()) == ClaimPermission.Inventory) return;
        Util.sendMessage(player, TextMode.Info, Messages.ContainerTrustPrice, flag.parameters);
    }
}
