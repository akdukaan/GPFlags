package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class FlagDef_AllowVillagerTrading extends FlagDefinition {

    public FlagDef_AllowVillagerTrading(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public String getName() {
        return "AllowVillagerTrading";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledAllowVillagerTrading);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledAllowVillagerTrading);
    }

    @EventHandler
    public void onGPVillagerTrade(ClaimPermissionCheckEvent event) {

        if(event.getCheckedPlayer() == null) return;

//        only looking for PlayerInteractEntityEvent (the event that GP cancels for trading with villagers)
        if(!(event.getTriggeringEvent() instanceof PlayerInteractEntityEvent)) return;
        final PlayerInteractEntityEvent clickMobEvent = (PlayerInteractEntityEvent) event.getTriggeringEvent();

//        only check if the player right-clicked a villager
        if(!(clickMobEvent.getRightClicked() instanceof Villager)) return;
        final Villager villager = (Villager) clickMobEvent.getRightClicked();

//        check if the current flag (AllowVillagerTrading) is set at the villager's location
        final Flag flag = this.getFlagInstanceAtLocation(villager.getLocation(), clickMobEvent.getPlayer());
        if(flag == null) return;

//        allow the player to trade (in GP, a null denial reason basically tells GP the player has permission)
        event.setDenialReason(null);
    }
}