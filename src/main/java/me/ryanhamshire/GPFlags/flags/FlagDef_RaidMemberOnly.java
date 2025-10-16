package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
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
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FlagDef_RaidMemberOnly extends FlagDefinition {

    public FlagDef_RaidMemberOnly(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    private void onRaidTrigger(RaidTriggerEvent event) {
        Flag flag = this.getFlagInstanceAtLocation(event.getRaid().getLocation(), null);
        if (flag == null) return;
        Player player = event.getPlayer();
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getRaid().getLocation(), false, null);
        if (claim == null) return;
        if (!Util.canAccess(claim, player)) {
            event.setCancelled(true);
            player.removePotionEffect(PotionEffectType.BAD_OMEN);
            MessagingUtil.sendMessage(player, TextMode.Warn, Messages.RaidMemberOnlyDeny);
        }
    }

    @Override
    public String getName() {
        return "RaidMemberOnly";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledRaidMemberOnly);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledRaidMemberOnly);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT);
    }

}
