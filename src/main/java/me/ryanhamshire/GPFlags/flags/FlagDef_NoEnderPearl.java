package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.FlagsDataStore;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FlagDef_NoEnderPearl extends FlagDefinition {

    public FlagDef_NoEnderPearl(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != TeleportCause.ENDER_PEARL) return;

        Player player = event.getPlayer();

        Flag flag = this.getFlagInstanceAtLocation(event.getFrom(), event.getPlayer());
        if (flag != null) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getFrom(), false, null);
            if (Util.shouldBypass(player, claim, flag)) return;

            event.setCancelled(true);
            String owner = claim.getOwnerName();
            String playerName = player.getName();

            String msg = new FlagsDataStore().getMessage(Messages.NoEnderPearlInClaim);
            msg = msg.replace("{p}", playerName).replace("{o}", owner);
            msg = msg.replace("{0}", playerName).replace("{1}", owner);
            MessagingUtil.sendMessage(player, TextMode.Warn + msg);
            return;

        }

        flag = this.getFlagInstanceAtLocation(event.getTo(), event.getPlayer());
        if (flag != null) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getTo(), false, null);
            if (!Util.shouldBypass(player, claim, flag)) return;

            event.setCancelled(true);
            String owner = claim.getOwnerName();

            String msg = new FlagsDataStore().getMessage(Messages.NoEnderPearlToClaim);
            MessagingUtil.sendMessage(player, TextMode.Warn + msg.replace("{o}", owner).replace("{p}", player.getName()));
        }
    }

    @Override
    public String getName() {
        return "NoEnderPearl";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoEnderPearl);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoEnderPearl);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT);
    }

}
