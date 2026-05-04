package me.ryanhamshire.GPFlags.flags;

import java.util.Arrays;
import java.util.List;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class FlagDef_AllowItemFrameContents extends FlagDefinition {

    public FlagDef_AllowItemFrameContents(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onItemFramePermissionCheck(ClaimPermissionCheckEvent event) {
        if (event.getCheckedPlayer() == null) return;
        if (event.getRequiredPermission() != ClaimPermission.Build) return;

        Event triggeringEvent = event.getTriggeringEvent();

        if (triggeringEvent instanceof PlayerInteractEntityEvent) {
            PlayerInteractEntityEvent interactEvent = (PlayerInteractEntityEvent) triggeringEvent;
            if (!(interactEvent.getRightClicked() instanceof ItemFrame)) return;

            ItemFrame itemFrame = (ItemFrame) interactEvent.getRightClicked();
            if (hasFlagAndContainerTrust(itemFrame, interactEvent.getPlayer())) {
                event.setDenialReason(null);
            }
            return;
        }

        if (triggeringEvent instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) triggeringEvent;
            if (!(damageEvent.getEntity() instanceof ItemFrame)) return;
            if (!(damageEvent.getDamager() instanceof Player)) return;

            ItemFrame itemFrame = (ItemFrame) damageEvent.getEntity();
            ItemStack item = itemFrame.getItem();
            if (item == null || item.getType() == Material.AIR) return;

            Player player = (Player) damageEvent.getDamager();
            if (hasFlagAndContainerTrust(itemFrame, player)) {
                event.setDenialReason(null);
            }
        }
    }

    private boolean hasFlagAndContainerTrust(ItemFrame itemFrame, Player player) {
        Flag flag = this.getFlagInstanceAtLocation(itemFrame.getLocation(), player);
        if (flag == null) return false;

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(itemFrame.getLocation(), false, playerData.lastClaim);
        if (claim == null) return false;

        return Util.canInventory(claim, player);
    }

    @Override
    public String getName() {
        return "AllowItemFrameContents";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledAllowItemFrameContents);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledAllowItemFrameContents);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT);
    }
}
