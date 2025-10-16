package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;


public class FlagDef_PermissionFly extends FlagDefinition {

    public FlagDef_PermissionFly(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onFlagSet(Claim claim, String param) {
        for (Player p : Util.getPlayersIn(claim)) {
            FlightManager.managePlayerFlight(p, null, p.getLocation());
        }
    }

    @Override
    public void onFlagUnset(Claim claim) {
        for (Player p : Util.getPlayersIn(claim)) {
            FlightManager.manageFlightLater(p, 1, p.getLocation());
        }
    }

    public static boolean letPlayerFly(Player player, Location location, Claim claim) {
        Flag flag = GPFlags.getInstance().getFlagManager().getEffectiveFlag(location, "PermissionFly", claim);
        if (flag == null) return false;
        return Util.shouldBypass(player, claim, flag);
    }

    @Override
    public String getName() {
        return "PermissionFly";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.PermissionFlightEnabled);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.PermissionFlightDisabled);
    }

}

