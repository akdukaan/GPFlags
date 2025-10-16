package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
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

public class FlagDef_NoChorusFruit extends FlagDefinition {

    public FlagDef_NoChorusFruit(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != TeleportCause.CHORUS_FRUIT) return;

        Player player = event.getPlayer();

        Flag flag = this.getFlagInstanceAtLocation(event.getFrom(), event.getPlayer());
        if (flag != null) {
            Claim claimFrom = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
            if (!Util.shouldBypass(player, claimFrom, flag)) {
                event.setCancelled(true);
            }
        }

        flag = this.getFlagInstanceAtLocation(event.getTo(), event.getPlayer());
        if (flag != null) {
            Claim claimTo = GriefPrevention.instance.dataStore.getClaimAt(event.getTo(), false, null);
            if (!Util.shouldBypass(player, claimTo, flag)) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getName() {
        return "NoChorusFruit";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoChorusFruit);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoChorusFruit);
    }

}
