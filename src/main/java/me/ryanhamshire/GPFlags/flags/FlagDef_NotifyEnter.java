package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FlagDef_NotifyEnter extends PlayerMovementFlagDefinition {

    public FlagDef_NotifyEnter(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onChangeClaim(Player player, Location lastLocation, Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        if (flagTo == null) return;

        if (shouldNotify(player, claimTo)) {
            notifyEntry(flagTo, claimTo, player);
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
        if (p.hasPermission("gpflags.bypass.notifyenter")) return false;
        return true;
    }

    public void notifyEntry(@NotNull Flag flag, @NotNull Claim claim, @NotNull Player player) {
        UUID uuid = claim.getOwnerID();
        if (uuid == null) return;
        Player owner = Bukkit.getPlayer(uuid);
        if (owner == null) return;
        if (owner.getName().equals(player.getName())) return;
        String param = flag.parameters;
        if (param == null || param.isEmpty()) {
            param = "claim " + claim.getID();
        }
        MessagingUtil.sendMessage(owner, TextMode.Info, Messages.NotifyEnter, player.getName(), param);

    }

    @Override
    public String getName() {
        return "NotifyEnter";
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNotifyEnter, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNotifyEnter);
    }

}
