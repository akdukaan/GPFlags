package me.ryanhamshire.GPFlags.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderApiHook extends PlaceholderExpansion {

    private final GPFlags plugin;

    public PlaceholderApiHook(GPFlags plugin) {
        this.plugin = plugin;
    }

    /**
     * Used to add oter plugin's placeholders to GPFlags messages
     * @param player Player context for placeholders that use it
     * @param message String before placeholders are added
     * @return String with placeholders added
     */
    public static String addPlaceholders(OfflinePlayer player, String message) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gpflags";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep registered even if PlaceholderAPI reloads
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        identifier = identifier.toLowerCase();
        if (!(offlinePlayer instanceof Player)) return null;
        Player player = (Player) offlinePlayer;
        String flagName = identifier.substring(identifier.indexOf('_') + 1);

        if (identifier.startsWith("cansetclaimflag_")) {
            // Check perms for that specific flag
            if (!player.hasPermission("gpflags.flag." + flagName)) {
                MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoFlagPermission, flagName);
                return "No";
            }

            // Check that the flag can be used in claims
            FlagDefinition def = plugin.getFlagManager().getFlagDefinitionByName(flagName);
            if (!def.getFlagType().contains(FlagDefinition.FlagType.CLAIM)) {
                MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoFlagInClaim);
                return "No";
            }

            // Check that they are standing in a claim
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
            if (claim == null) {
                return "No";
            }

            // Check that they can set flags in the area
            if (!Util.canEdit(player, claim)) {
                MessagingUtil.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
                return "No";
            }

            return "Yes";
        }
        if (identifier.startsWith("isflagactive_")) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
            Flag flag = plugin.getFlagManager().getEffectiveFlag(player.getLocation(), flagName, claim);
            if (flag == null) return "No";
            return "Yes";
        }
        return null;
    }
}
