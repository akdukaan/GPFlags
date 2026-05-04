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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
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
    public @Nonnull List<String> getPlaceholders() {
        List<String> result = new java.util.ArrayList<>();
        result.add("%gpflags_"+ "cansetclaimflag"+ "_<flag>%");
        result.add("%gpflags_"+ "isflagactive"+ "_<flag>%");
        result.add("%gpflags_"+ "flagparam"+ "_<flag>%");
        return result;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        if (!(offlinePlayer instanceof Player)) return null;
        Player player = (Player) offlinePlayer;
        identifier = identifier.toLowerCase();

        String[] parts = identifier.split("_", 2);
        if (parts.length < 2) return null;

        String type = parts[0];
        String flagName = parts[1];

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);

        switch (type) {
            case "cansetclaimflag":
                // Check perms for that specific flag
                if (!player.hasPermission("gpflags.flag." + flagName)) {
                    MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoFlagPermission, flagName);
                    return "No";
                }

                // Check if flag exists and is usable in claims
                FlagDefinition def = plugin.getFlagManager().getFlagDefinitionByName(flagName);
                if (def == null || !def.getFlagType().contains(FlagDefinition.FlagType.CLAIM)) {
                    MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoFlagInClaim);
                    return "No";
                }

                // Check that they are standing in a claim
                if (claim == null) return "No";

                // Check that they can set flags in the area
                if (!Util.canEdit(player, claim)) {
                    MessagingUtil.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
                    return "No";
                }

                return "Yes";

            case "isflagactive":
                if (claim == null) return "No";
                Flag flagActive = plugin.getFlagManager().getEffectiveFlag(player.getLocation(), flagName, claim);
                return (flagActive != null) ? "Yes" : "No";

            case "flagparam":
                if (claim == null) return "No";
                Flag flagParam = plugin.getFlagManager().getEffectiveFlag(player.getLocation(), flagName, claim);
                return (flagParam != null) ? flagParam.parameters : "";

            default:
                return null;
        }
    }
}
