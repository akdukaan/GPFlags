package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FlagDef_OwnerFly extends FlagDefinition {

    public FlagDef_OwnerFly(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onFlagSet(Claim claim, String param) {
        UUID uuid = claim.getOwnerID();
        if (uuid == null) return;
        Player owner = Bukkit.getPlayer(uuid);
        if (owner == null) return;
        Location location = owner.getLocation();
        if (!claim.contains(location, false, false)) return;
        FlightManager.managePlayerFlight(owner, null, location);
    }

    @Override
    public void onFlagUnset(Claim claim) {
        UUID uuid = claim.getOwnerID();
        if (uuid == null) return;
        Player owner = Bukkit.getPlayer(uuid);
        if (owner == null) {
            return;
        };
        Location location = owner.getLocation();
        if (!claim.contains(location, false, false)) {
            return;
        }
        FlightManager.manageFlightLater(owner, 1, owner.getLocation());
    }

    public static boolean letPlayerFly(Player player, Location location, Claim claim) {
        if (claim == null) return false;
        Flag flag = GPFlags.getInstance().getFlagManager().getEffectiveFlag(location, "OwnerFly", claim);
        if (flag == null) return false;
        return (player.getUniqueId().equals(claim.getOwnerID()));
    }

    @Override
    public String getName() {
        return "OwnerFly";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.OwnerFlightEnabled);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.OwnerFlightDisabled);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT);
    }

}

