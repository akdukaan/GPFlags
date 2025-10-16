package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class FlagDef_HealthRegen extends TimedPlayerFlagDefinition {

    public FlagDef_HealthRegen(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public long getPlayerCheckFrequency_Ticks() {
        return 100L;
    }

    @Override
    public void processPlayer(Player player) {
        if (player.getHealth() >= player.getAttribute(Attribute.MAX_HEALTH).getValue() || player.isDead())
            return;

        Flag flag = this.getFlagInstanceAtLocation(player.getLocation(), player);
        if (flag == null) return;

        int healAmount = 1;
        if (flag.parameters != null && !flag.parameters.isEmpty()) {
            try {
                healAmount = Integer.parseInt(flag.parameters);
            } catch (NumberFormatException ignored) {
                // Heal amount can be 1 if invalid
            }
        }

        int newHealth = healAmount + (int) player.getHealth();
        player.setHealth(Math.min(player.getAttribute(Attribute.MAX_HEALTH).getValue(), newHealth));
    }

    @Override
    public String getName() {
        return "HealthRegen";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableHealthRegen);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableHealthRegen);
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty())
            return new SetFlagResult(false, new MessageSpecifier(Messages.HealthRegenGreaterThanZero));
        int amount;
        try {
            amount = Integer.parseInt(parameters);
            if (amount <= 0) {
                return new SetFlagResult(false, new MessageSpecifier(Messages.HealthRegenGreaterThanZero));
            }
        } catch (NumberFormatException e) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.HealthRegenGreaterThanZero));
        }
        if (!senderHasPermissionForHealthAmount(sender, amount)) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.HealthRegenTooHigh));
        }

        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    private boolean senderHasPermissionForHealthAmount(CommandSender sender, int desired) {
        if (sender == null) return true;
        int allowed = 1;
        Set<PermissionAttachmentInfo> attachments = sender.getEffectivePermissions();
        for (PermissionAttachmentInfo attachment : attachments) {
            String permName = attachment.getPermission().toLowerCase();
            if (permName.startsWith("gpflags.flag.healthregen.") && attachment.getValue()) {
                try {
                    int newVal = Integer.parseInt(permName.toLowerCase().replace("gpflags.flag.healthregen.", ""));
                    if (newVal > allowed) {
                        allowed = newVal;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return desired <= allowed;
    }

}
