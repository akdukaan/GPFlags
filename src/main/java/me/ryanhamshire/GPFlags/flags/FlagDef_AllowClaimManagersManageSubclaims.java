package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.ShovelMode;
import me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_AllowClaimManagersManageSubclaims extends FlagDefinition {

    public FlagDef_AllowClaimManagersManageSubclaims(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public String getName() {
        return FlagManager.CLAIM_MANAGER_MANAGE_SUBCLAIMS_FLAG;
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledAllowClaimManagersManageSubclaims);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledAllowClaimManagersManageSubclaims);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClaimPermissionCheck(ClaimPermissionCheckEvent event) {
        if (event.getDenialReason() == null) return;

        Player player = event.getCheckedPlayer();
        if (player == null) return;

        Claim claim = event.getClaim();
        ClaimPermission requiredPermission = event.getRequiredPermission();

        if (claim.parent != null) {
            if (requiredPermission != ClaimPermission.Edit && requiredPermission != ClaimPermission.Manage) return;
            if (requiredPermission == ClaimPermission.Edit && claim.siegeData != null) return;
            if (!hasFlag(claim) || !hasParentManageTrust(claim, player)) return;

            event.setDenialReason(null);
            return;
        }

        if (requiredPermission != ClaimPermission.Edit) return;
        if (!(event.getTriggeringEvent() instanceof PlayerInteractEvent)) return;

        PlayerInteractEvent interactEvent = (PlayerInteractEvent) event.getTriggeringEvent();
        if (interactEvent.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        if (playerData.shovelMode != ShovelMode.Subdivide) return;
        if (playerData.lastShovelLocation != null && playerData.claimSubdividing != claim) return;

        Block clickedBlock = interactEvent.getClickedBlock();
        if (clickedBlock == null || isClaimCorner(claim, clickedBlock.getLocation())) return;
        if (claim.siegeData != null) return;
        if (!hasFlag(claim) || !hasManageTrust(claim, player)) return;

        event.setDenialReason(null);
    }

    private boolean hasFlag(Claim claim) {
        Location location = claim.getLesserBoundaryCorner();
        if (location == null || location.getWorld() == null) return false;
        Flag flag = getEffectiveFlag(claim, location.getWorld());
        return flag != null;
    }

    private boolean hasParentManageTrust(Claim claim, Player player) {
        return claim.parent != null && hasManageTrust(claim.parent, player);
    }

    private boolean hasManageTrust(Claim claim, Player player) {
        return claim.checkPermission(player, ClaimPermission.Manage, null) == null;
    }

    private boolean isClaimCorner(Claim claim, Location location) {
        Location lesser = claim.getLesserBoundaryCorner();
        Location greater = claim.getGreaterBoundaryCorner();
        boolean xCorner = location.getBlockX() == lesser.getBlockX() || location.getBlockX() == greater.getBlockX();
        boolean zCorner = location.getBlockZ() == lesser.getBlockZ() || location.getBlockZ() == greater.getBlockZ();
        return xCorner && zCorner;
    }
}
