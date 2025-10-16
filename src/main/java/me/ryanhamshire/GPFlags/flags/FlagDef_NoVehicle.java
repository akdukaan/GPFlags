package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FlagDef_NoVehicle extends PlayerMovementFlagDefinition {

    public FlagDef_NoVehicle(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onChangeClaim(@NotNull Player player, @Nullable Location from, @NotNull Location to, @Nullable Claim claimFrom, @Nullable Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        // Check if the flag exists
        if (flagTo == null) return;

        // Check if it's a minecart or boat and not an animal
        if (player.getVehicle() == null) return;
        Entity entity = player.getVehicle();
        if (!(entity instanceof Vehicle)) return;
        Vehicle vehicle = (Vehicle) entity;

        // Ignore event for non-drivers
        // This gets called for every rider but we only want to take action on it once
        List<Entity> passengers = vehicle.getPassengers();
        if (passengers.isEmpty()) return;
        Entity passenger1 = passengers.get(0);
        if (!(passenger1 instanceof Player)) return;
        Player driver = (Player) passenger1;
        if (driver != player) return;

        // Check if the driver can bypass
        if (Util.shouldBypass(player, claimTo, flagTo)) return;

        // Break the vehicle and alert all passengers
        Util.breakVehicle(vehicle, from);
        for (Entity rider : passengers) {
            if (rider instanceof Player) {
                MessagingUtil.sendMessage(rider, TextMode.Err, Messages.NoVehicleAllowed);
            }
        }

    }

    // Low priority so that PlayerPreClaimBorderEvent is only made if this doesn't cancel the event
    @EventHandler(priority = EventPriority.LOW)
    private void onMount(VehicleEnterEvent event) {
        Entity entity = event.getEntered();
        Vehicle vehicle = event.getVehicle();
        if (!(entity instanceof Player)) return;
        Player player = ((Player) entity);

        // Check if the player can bypass
        Flag flag = this.getFlagInstanceAtLocation(vehicle.getLocation(), player);
        if (flag == null) return;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(vehicle.getLocation(), false, null);
        if (Util.shouldBypass(player, claim, flag)) return;

        event.setCancelled(true);
        MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoEnterVehicle);
    }

    @Override
    public String getName() {
        return "NoVehicle";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledNoVehicle);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledNoVehicle);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT, FlagType.WORLD, FlagType.SERVER);
    }

}
