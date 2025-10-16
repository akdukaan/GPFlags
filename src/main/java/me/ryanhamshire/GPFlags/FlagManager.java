package me.ryanhamshire.GPFlags;

import com.google.common.io.Files;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for flags
 * Inherited = Checks higher levels
 * Self = Doesn't check higher levels
 * Raw = Will return the flag for flags that are set to be unset
 * Logical = Will return null for flags that are set to be unset
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class FlagManager {

    private final ConcurrentHashMap<String, FlagDefinition> definitions;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Flag>> flags;
    private final List<String> worlds = new ArrayList<>();

    public static final String DEFAULT_FLAG_ID = "-2";

    public FlagManager() {
        this.definitions = new ConcurrentHashMap<>();
        this.flags = new ConcurrentHashMap<>();
        Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
    }

    /**
     * Register a new flag definition
     *
     * @param def Flag Definition to register
     */
    public void registerFlagDefinition(FlagDefinition def) {
        String name = def.getName();
        this.definitions.put(name.toLowerCase(), def);
    }

    /**
     * Get a flag definition by name
     *
     * @param name Name of the flag to get
     * @return Flag definition by name
     */
    public FlagDefinition getFlagDefinitionByName(String name) {
        return this.definitions.get(name.toLowerCase());
    }

    /**
     * Get a collection of all registered flag definitions
     *
     * @return All registered flag definitions
     */
    public Collection<FlagDefinition> getFlagDefinitions() {
        return new ArrayList<>(this.definitions.values());
    }

    /**
     * Get a collection of names of all registered flag definitions
     *
     * @return Names of all registered flag definitions
     */
    public Collection<String> getFlagDefinitionNames() {
        return new ArrayList<>(this.definitions.keySet());
    }

    /**
     * Set a flag for a claim. This is called on startup to load the datastore and when setting a flag to a value including false
     *
     * @param claimId  ID of {@link Claim} which this flag will be attached to
     * @param def      Flag definition to set
     * @param isActive Whether the flag will be active or not
     * @param args     Message parameters
     * @return Result of setting flag
     */
    public SetFlagResult setFlag(String claimId, FlagDefinition def, boolean isActive, CommandSender sender, String... args) {
        StringBuilder internalParameters = new StringBuilder();
        StringBuilder friendlyParameters = new StringBuilder();
        for (String arg : args) {
            friendlyParameters.append(arg).append(" ");
            if (def.getName().equals("NoEnterPlayer") && !arg.isEmpty()) {
                if (arg.length() <= 30) {
                    OfflinePlayer offlinePlayer;
                    try {
                        offlinePlayer = Bukkit.getOfflinePlayerIfCached(arg);
                        if (offlinePlayer != null) {
                            arg = offlinePlayer.getUniqueId().toString();
                        }
                    } catch (NoSuchMethodError ignored) {
                        offlinePlayer = Bukkit.getOfflinePlayer(arg);
                        arg = offlinePlayer.getUniqueId().toString();
                    }

                }
            }
            internalParameters.append(arg).append(" ");
        }
        internalParameters = new StringBuilder(internalParameters.toString().trim());
        friendlyParameters = new StringBuilder(friendlyParameters.toString().trim());

        SetFlagResult result;
        if (isActive) {
            result = def.validateParameters(friendlyParameters.toString(), sender);
            if (!result.success) return result;
        } else {
            result = new SetFlagResult(true, def.getUnSetMessage());
        }

        Flag flag = new Flag(def, internalParameters.toString());
        flag.setSet(isActive);
        ConcurrentHashMap<String, Flag> claimFlags = this.flags.get(claimId);
        if (claimFlags == null) {
            claimFlags = new ConcurrentHashMap<>();
            this.flags.put(claimId, claimFlags);
        }

        String key = def.getName().toLowerCase();
        if (!claimFlags.containsKey(key) && isActive) {
            def.incrementInstances();
        }
        claimFlags.put(key, flag);

        // Deal with default flags being set
        if (DEFAULT_FLAG_ID.equals(claimId)) {
            for (Claim claim : GriefPrevention.instance.dataStore.getClaims()) {
                if (isActive) {
                    def.onFlagSet(claim, flag.parameters);
                } else {
                    def.onFlagUnset(claim);
                }
            }
            return result;
        }

        Claim claim;
        try {
            claim = GriefPrevention.instance.dataStore.getClaim(Long.parseLong(claimId));
        } catch (Throwable ignored) {
            return result;
        }
        if (claim != null) {
            if (isActive) {
                def.onFlagSet(claim, internalParameters.toString());
            } else {
                def.onFlagUnset(claim);
            }
        }
        return result;
    }

    /**
     *
     * @param claim claim or subclaim
     * @param flag flag name in all lowercase
     * @return The raw instance of the flag
     */
    public @Nullable Flag getRawClaimFlag(@NotNull Claim claim, @NotNull String flag) {
        String claimId = claim.getID().toString();
        ConcurrentHashMap<String, Flag> claimFlags = this.flags.get(claimId);
        if (claimFlags == null) return null;
        return claimFlags.get(flag);
    }

    public @Nullable Flag getRawDefaultFlag(@NotNull String flag) {
        ConcurrentHashMap<String, Flag> defaultFlags = flags.get(DEFAULT_FLAG_ID);
        if (defaultFlags == null) return null;
        return defaultFlags.get(flag);
    }

    public @Nullable Flag getRawWorldFlag(@NotNull World world, @NotNull String flag) {
        ConcurrentHashMap<String, Flag> worldFlags = flags.get(world.getName());
        if (worldFlags == null) return null;
        return worldFlags.get(flag);
    }

    public @Nullable Flag getRawServerFlag(@NotNull String flag) {
        ConcurrentHashMap<String, Flag> serverFlags = this.flags.get("everywhere");
        if (serverFlags == null) return null;
        return serverFlags.get(flag);
    }

    /**
     * @param location
     * @param flagname
     * @param claim
     * @return the effective flag at the location
     */
    public @Nullable Flag getEffectiveFlag(@Nullable Location location, @NotNull String flagname, @Nullable Claim claim) {
        if (location == null) return null;
        flagname = flagname.toLowerCase();
        Flag flag;
        if (GriefPrevention.instance.claimsEnabledForWorld(location.getWorld())) {
            if (claim != null) {
                flag = getRawClaimFlag(claim, flagname);
                if (flag != null) {
                    if (flag.getSet()) return flag;
                    return null;
                }
                Claim parent = claim.parent;
                if (parent != null) {
                    flag = getRawClaimFlag(parent, flagname);
                    if (flag != null) {
                        if (flag.getSet()) return flag;
                        return null;
                    }
                }
                flag = getRawDefaultFlag(flagname);
                if (flag != null) {
                    if (flag.getSet()) return flag;
                    return null;
                }
            }
        }

        flag = getRawWorldFlag(location.getWorld(), flagname);
        if (flag != null && flag.getSet()) return flag;

        flag = getRawServerFlag(flagname);
        if (flag != null && flag.getSet()) return flag;

        return null;
    }

    /**
     *
     * @param flagname
     * @param claim
     * @param world World to be checked if claim is null
     * @return
     */
    public @Nullable Flag getEffectiveFlag(@NotNull String flagname, @Nullable Claim claim, @NotNull World world) {
        flagname = flagname.toLowerCase();
        Flag flag;
        if (claim != null && GriefPrevention.instance.claimsEnabledForWorld(world)) {
            flag = getRawClaimFlag(claim, flagname);
            if (flag != null) {
                if (flag.getSet()) return flag;
                return null;
            }
            Claim parent = claim.parent;
            if (parent != null) {
                flag = getRawClaimFlag(parent, flagname);
                if (flag != null) {
                    if (flag.getSet()) return flag;
                    return null;
                }
            }
            flag = getRawDefaultFlag(flagname);
            if (flag != null) {
                if (flag.getSet()) return flag;
                return null;
            }
        }

        flag = getRawWorldFlag(world, flagname);
        if (flag != null && flag.getSet()) return flag;

        flag = getRawServerFlag(flagname);
        if (flag != null && flag.getSet()) return flag;

        return null;
    }

    /**
     * Get all flags in a claim
     *
     * @param claim Claim to get flags from
     * @return All flags in this claim
     */
    public Collection<Flag> getFlags(Claim claim) {
        if (claim == null) return null;
        return getFlags(claim.getID().toString());
    }

    /**
     * Get all flags in a claim
     *
     * @param claimID ID of claim
     * @return All flags in this claim
     */
    public Collection<Flag> getFlags(String claimID) {
        if (claimID == null) return null;
        ConcurrentHashMap<String, Flag> claimFlags = this.flags.get(claimID);
        if (claimFlags == null) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(claimFlags.values());
        }
    }

    /**
     * Unset a flag in a claim
     *
     * @param claim Claim to remove flag from
     * @param def   Flag definition to remove
     * @return Flag result
     */
    public SetFlagResult unSetFlag(Claim claim, FlagDefinition def) {
        return unSetFlag(claim.getID().toString(), def);
    }

    /**
     * Unset a flag in a claim
     *
     * @param claimId ID of claim
     * @param def     Flag definition to remove
     * @return Flag result
     */
    public SetFlagResult unSetFlag(String claimId, FlagDefinition def) {
        ConcurrentHashMap<String, Flag> claimFlags = this.flags.get(claimId);
        if (claimFlags == null || !claimFlags.containsKey(def.getName().toLowerCase())) {
            return this.setFlag(claimId, def, false, null);
        } else {
            try {
                Claim claim = GriefPrevention.instance.dataStore.getClaim(Long.parseLong(claimId));
                def.onFlagUnset(claim);
            } catch (Throwable ignored) {}
            claimFlags.remove(def.getName().toLowerCase());
            return new SetFlagResult(true, def.getUnSetMessage());
        }
    }

    List<MessageSpecifier> load(String input) throws InvalidConfigurationException {
        this.flags.clear();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(input);

        ArrayList<MessageSpecifier> errors = new ArrayList<>();
        Set<String> claimIDs = yaml.getKeys(false);
        for (String claimID : claimIDs) {
            Set<String> flagNames = yaml.getConfigurationSection(claimID).getKeys(false);
            for (String flagName : flagNames) {
                String paramsDefault = yaml.getString(claimID + "." + flagName);
                String params = yaml.getString(claimID + "." + flagName + ".params", paramsDefault);
                if (FlagsDataStore.PRIOR_CONFIG_VERSION == 0) {
                    params = MessagingUtil.reserialize(params);
                }
                boolean set = yaml.getBoolean(claimID + "." + flagName + ".value", true);
                FlagDefinition def = this.getFlagDefinitionByName(flagName);
                if (def != null) {
                    SetFlagResult result = this.setFlag(claimID, def, set, null, params);
                    if (!result.success) {
                        errors.add(result.message);
                    }
                }
            }
        }
        if (errors.isEmpty() && FlagsDataStore.PRIOR_CONFIG_VERSION == 0) save();
        return errors;
    }

    public void save() {
        try {
            this.save(FlagsDataStore.flagsFilePath);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public HashSet<String> getUsedFlags() {
        HashSet<String> usedFlags = new HashSet<>();
        Set<String> claimIDs = this.flags.keySet();
        for (String claimID : claimIDs) {
            ConcurrentHashMap<String, Flag> claimFlags = this.flags.get(claimID);
            usedFlags.addAll(claimFlags.keySet());
        }
        return usedFlags;
    }

    public String flagsToString() {
        YamlConfiguration yaml = new YamlConfiguration();

        Set<String> claimIDs = this.flags.keySet();
        for (String claimID : claimIDs) {
            ConcurrentHashMap<String, Flag> claimFlags = this.flags.get(claimID);
            Set<String> flagNames = claimFlags.keySet();
            for (String flagName : flagNames) {
                Flag flag = claimFlags.get(flagName);
                String paramsPath = claimID + "." + flagName + ".params";
                yaml.set(paramsPath, flag.parameters);
                String valuePath = claimID + "." + flagName + ".value";
                yaml.set(valuePath, flag.getSet());
            }
        }

        return yaml.saveToString();
    }

    public void save(String filepath) throws IOException {
        String fileContent = this.flagsToString();
        File file = new File(filepath);
        file.getParentFile().mkdirs();
        file.createNewFile();
        Files.write(fileContent.getBytes(StandardCharsets.UTF_8), file);
    }

    /**
     *
     * @param file
     * @return A list of errors
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    public List<MessageSpecifier> load(File file) throws IOException, InvalidConfigurationException {
        if (!file.exists()) return this.load("");

        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append('\n');
        }

        return this.load(builder.toString());
    }

    public void clear() {
        this.flags.clear();
    }

    void removeExceptClaimIDs(HashSet<String> validClaimIDs) {
        HashSet<String> toRemove = new HashSet<>();
        for (String key : this.flags.keySet()) {
            //if not a valid claim ID (maybe that claim was deleted)
            if (!validClaimIDs.contains(key)) {
                try {
                    int numericalValue = Integer.parseInt(key);

                    //if not a special value like default claims ID, remove
                    if (numericalValue >= 0) toRemove.add(key);
                } catch (NumberFormatException ignore) {
                } //non-numbers represent server or world flags, so ignore those
            }
        }
        for (String key : toRemove) {
            this.flags.remove(key);
        }
        save();
    }

}
