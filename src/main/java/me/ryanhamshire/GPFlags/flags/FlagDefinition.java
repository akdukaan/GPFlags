package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GPFlags.WorldSettingsManager;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Base flag definition
 * <p>When creating a new flag, extend from this class</p>
 */
public abstract class FlagDefinition implements Listener {

    private final FlagManager flagManager;
    WorldSettingsManager settingsManager;
    private int instances = 0;
    protected GPFlags plugin;
    protected Claim cachedClaim = null;

    public FlagDefinition(FlagManager manager, GPFlags plugin) {
        this.flagManager = manager;
        this.plugin = plugin;
        this.settingsManager = plugin.getWorldSettingsManager();
    }

    public abstract String getName();

    public SetFlagResult validateParameters(String parameters, @Nullable CommandSender sender) {
        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    public abstract MessageSpecifier getSetMessage(String parameters);

    public abstract MessageSpecifier getUnSetMessage();

    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT, FlagType.WORLD, FlagType.SERVER);
    }

    // Called when a flag is set to false/true, etc.
    public void onFlagSet(Claim claim, String params) {
    }

    // Called when the flag is killed
    public void onFlagUnset(Claim claim) {
    }

    /**
     * Get an instance of a flag at a location
     * @param location Location for checking for flag
     * @param player Player for checking cached claims
     * @return Logical instance of flag at location
     */
    public Flag getFlagInstanceAtLocation(@NotNull Location location, @Nullable Player player) {
        if (cachedClaim == null && player != null) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
            cachedClaim = playerData.lastClaim;
        }
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, false, cachedClaim);
        cachedClaim = claim;
        return flagManager.getEffectiveFlag(location, this.getName(), claim);
    }

    public Flag getEffectiveFlag(@Nullable Claim claim, @NotNull World world) {
        return flagManager.getEffectiveFlag(this.getName(), claim, world);
    }

    public Flag getEffectiveFlag(@Nullable Claim claim, Location location) {
        if (location == null) return null;
        return flagManager.getEffectiveFlag(this.getName(), claim, location.getWorld());
    }

    public void incrementInstances() {
        if (++this.instances == 1) {
            this.firstTimeSetup();
        }
    }

    private boolean hasRegisteredEvents = false;
    
    public void firstTimeSetup() {
        if (hasRegisteredEvents) return;
        hasRegisteredEvents = true;
        Bukkit.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    public void updateSettings(WorldSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    /**
     * Flag types <br>Defines the types of claims a flag can be set in
     */
    public enum FlagType {
        /**
         * Flag can be set in a claim
         */
        CLAIM("<green>CLAIM"),
        /**
         * Flag can be set for an entire world
         */
        WORLD("<gold>WORLD"),
        /**
         * Flag can bet set for the entire server
         */
        SERVER("<dark_aqua>SERVER"),
        /**
         * Flag can be set as a default flag
         */
        DEFAULT("<YELLOW>DEFAULT");

        String name;

        FlagType(String string) {
            this.name = string;
        }

        @Override
        public String toString() {
            return name + "<grey>";
        }
    }

}
