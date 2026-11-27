package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;



public class FlagDef_PermissionFly extends FlagDefinition {

    public FlagDef_PermissionFly(FlagManager manager, GPFlags plugin) {
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

