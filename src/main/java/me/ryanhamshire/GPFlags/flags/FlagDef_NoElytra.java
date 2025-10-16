package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoElytra extends PlayerMovementFlagDefinition {


    public FlagDef_NoElytra(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onChangeClaim(@NotNull Player player, Location lastLocation, @NotNull Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        if (flagTo == null) return;
        if (!player.isGliding()) return;
        if (Util.shouldBypass(player, claimTo, flagTo)) return;
        player.setGliding(false);
        FlightManager.considerForFallImmunity(player);
    }

    @EventHandler
    private void onToggleElytra(EntityToggleGlideEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        if (!event.isGliding()) return;
        Player player = (Player) event.getEntity();
        Location location = player.getLocation();
        Flag flag = this.getFlagInstanceAtLocation(location, player);
        if (flag == null) return;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        if (Util.shouldBypass(player, claim, flag)) return;

        event.setCancelled(true);

    }

    @Override
    public String getName() {
        return "NoElytra";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoElytra, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoElytra);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT, FlagType.WORLD, FlagType.SERVER);
    }

}
