package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class FlagDef_OwnerFly extends FlagDefinition {

    public FlagDef_OwnerFly(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onFlagSet(Claim claim, String param) {
        FlightManager.reconcileLater(Util.getPlayersIn(claim));
    }

    @Override
    public void onFlagUnset(Claim claim) {
        // A tick later since the flag hasn't been removed yet
        FlightManager.reconcileLater(Util.getPlayersIn(claim));
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

