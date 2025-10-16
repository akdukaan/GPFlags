package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FlagDef_NotifyExit extends PlayerMovementFlagDefinition {

    public FlagDef_NotifyExit(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onChangeClaim(Player player, Location lastLocation, Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        if (flagFrom == null) return;

        if (shouldNotify(player, claimFrom)) {
            notifyExit(flagFrom, claimFrom, player);
        }
    }

    public boolean shouldNotify(@NotNull Player p, @Nullable Claim c) {
        if (c == null) return false;
        UUID ownerID = c.getOwnerID();
        if (ownerID == null) return false;
        Player owner = Bukkit.getPlayer(ownerID);
        if (owner == null) return false;
        if (owner.getName().equals(p.getName())) return false;
        if (!owner.canSee(p)) return false;
        if (p.getGameMode() == GameMode.SPECTATOR) return false;
        if (p.hasPermission("gpflags.bypass.notifyexit")) return false;
        return true;
    }

    public void notifyExit(@NotNull Flag flag, @NotNull Claim claim, @NotNull Player player) {
        UUID uuid = claim.getOwnerID();
        if (uuid == null) return;
        Player owner = Bukkit.getPlayer(uuid);
        if (owner == null) return;
        if (owner.getName().equals(player.getName())) return;
        String param = flag.parameters;
        if (param == null || param.isEmpty()) {
            param = "claim " + claim.getID();
        }
        MessagingUtil.sendMessage(owner, TextMode.Info, Messages.NotifyExit, player.getName(), param);

    }


    @Override
    public String getName() {
        return "NotifyExit";
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNotifyExit, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNotifyExit);
    }

}
