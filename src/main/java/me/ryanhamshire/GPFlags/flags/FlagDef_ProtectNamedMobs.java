package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.hooks.MythicMobsHook;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_ProtectNamedMobs extends FlagDefinition {

    public FlagDef_ProtectNamedMobs(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        Flag flag = this.getFlagInstanceAtLocation(entity.getLocation(), null);
        if (flag == null) return;
        if (entity.getCustomName() == null) return;

        EntityType eType = entity.getType();
        if (eType == EntityType.PLAYER) return;
        if (eType == EntityType.ARMOR_STAND) return;
        if (eType == EntityType.ITEM_FRAME) return;
        if (eType == EntityType.GLOW_ITEM_FRAME) return;
        if (MythicMobsHook.isMythicMob(entity)) return;

        Entity damager = event.getDamager();
        if (damager.getType() != EntityType.PLAYER) {
            event.setCancelled(true);
            return;
        }

        Player player = (Player) damager;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false, null);
        if (claim != null && claim.getPermission(player.getUniqueId().toString()) == ClaimPermission.Inventory) return;
        event.setCancelled(true);
    }

    @Override
    public String getName() {
        return "ProtectNamedMobs";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledProtectNamedMobs);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledProtectNamedMobs);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT);
    }

}
