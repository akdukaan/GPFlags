package me.ryanhamshire.GPFlags;

import com.google.common.io.Files;
import me.ryanhamshire.GPFlags.commands.CommandBuyAccessTrust;
import me.ryanhamshire.GPFlags.commands.CommandBuyBuildTrust;
import me.ryanhamshire.GPFlags.commands.CommandBuyContainerTrust;
import me.ryanhamshire.GPFlags.commands.CommandBuySubclaim;
import me.ryanhamshire.GPFlags.flags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class GPFlagsConfig {
    
    private final GPFlags plugin;
    private final FlagManager flagManager;

    public static boolean LOG_ENTER_EXIT_COMMANDS = true;

    public GPFlagsConfig(GPFlags plugin) {
        this.plugin = plugin;
        this.flagManager = plugin.getFlagManager();
        loadConfig();
    }

    public void loadConfig() {
        this.flagManager.clear();

        //load the config if it exists
        FileConfiguration inConfig = YamlConfiguration.loadConfiguration(new File(FlagsDataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();

        LOG_ENTER_EXIT_COMMANDS = inConfig.getBoolean("Settings.Log Enter/Exit Messages To Console", true);
        outConfig.set("Settings.Log Enter/Exit Messages To Console", LOG_ENTER_EXIT_COMMANDS);

        List<World> worlds = plugin.getServer().getWorlds();
        ArrayList<String> worldSettingsKeys = new ArrayList<>();
        for (World world : worlds) {
            worldSettingsKeys.add(world.getName());
        }

        for (String worldName : worldSettingsKeys) {
            WorldSettings settings = plugin.getWorldSettingsManager().create(worldName);

            settings.worldGamemodeDefault = inConfig.getString("World Flags." + worldName + ".Default Gamemode", "survival");
            String worldGMDefault = settings.worldGamemodeDefault;
            if (!worldGMDefault.equalsIgnoreCase("survival") && !worldGMDefault.equalsIgnoreCase("creative") &&
                    !worldGMDefault.equalsIgnoreCase("adventure") && !worldGMDefault.equalsIgnoreCase("spectator")) {
                settings.worldGamemodeDefault = "survival";
            }
            outConfig.set("World Flags." + worldName + ".Default Gamemode", settings.worldGamemodeDefault);

            settings.pvpRequiresClaimFlag = inConfig.getBoolean("World Flags." + worldName + ".PvP Only In PvP-Flagged Claims", false);
            outConfig.set("World Flags." + worldName + ".PvP Only In PvP-Flagged Claims", settings.pvpRequiresClaimFlag);

            settings.pvpDeniedMessage = inConfig.getString("World Flags." + worldName + ".Message To Send When PvP Is Denied",
                    "Player vs. player combat is restricted to designated areas.");
            outConfig.set("World Flags." + worldName + ".Message To Send When PvP Is Denied", settings.pvpDeniedMessage);

            settings.pvpEnterClaimMessageEnabled = inConfig.getBoolean("World Flags." + worldName + ".Send Message On Enter PvP Enabled Claim", false);
            outConfig.set("World Flags." + worldName + ".Send Message On Enter PvP Enabled Claim", settings.pvpEnterClaimMessageEnabled);

            settings.pvpEnterClaimMessage = inConfig.getString("World Flags." + worldName + ".Message",
                    "PvP is enabled in this claim, be careful");
            outConfig.set("World Flags." + worldName + ".Message", settings.pvpEnterClaimMessage);

            settings.pvpExitClaimMessageEnabled = inConfig.getBoolean("World Flags." + worldName + ".Send Message On Exit PvP Enabled Claim", false);
            outConfig.set("World Flags." + worldName + ".Send Message On Exit PvP Enabled Claim", settings.pvpExitClaimMessageEnabled);

            settings.pvpExitClaimMessage = inConfig.getString("World Flags." + worldName + ".ExitMessage", "PvP is disabled in this area, you are now safe");
            outConfig.set("World Flags." + worldName + ".ExitMessage", settings.pvpExitClaimMessage);

            settings.noMonsterSpawnIgnoreSpawners = inConfig.getBoolean("World Flags." + worldName + ".NoMonsterSpawn Flag Ignores Spawners and Eggs", true);
            outConfig.set("World Flags." + worldName + ".NoMonsterSpawn Flag Ignores Spawners and Eggs", settings.noMonsterSpawnIgnoreSpawners);

            settings.noMobSpawnIgnoreSpawners = inConfig.getBoolean("World Flags." + worldName + ".NoMobSpawn Flag Ignores Spawners and Eggs", true);
            outConfig.set("World Flags." + worldName + ".NoMobSpawn Flag Ignores Spawners and Eggs", settings.noMobSpawnIgnoreSpawners);

            settings.clearFlagsOnTransferClaim = inConfig.getBoolean("World Flags." + worldName + ".Reset Flags On Transfer Claim", false);
            outConfig.set("World Flags." + worldName + ".Clear Flags On Transfer Claim", settings.clearFlagsOnTransferClaim);

            outConfig.options().header("GP Flags\n" + "Plugin Version: " + plugin.getDescription().getVersion() +
                    "\nServer Version: " + plugin.getServer().getVersion() + "\n\n");
        }

        try {
            outConfig.save(FlagsDataStore.configFilePath);
            MessagingUtil.sendMessage(null, "Finished loading configuration.");
        } catch (IOException exception) {
            MessagingUtil.sendMessage(null, "Unable to write to the configuration file at \"" + FlagsDataStore.configFilePath + "\"");
        }

        //register flag definitions
        if (!plugin.registeredFlagDefinitions) {
            plugin.registeredFlagDefinitions = true;
            this.flagManager.registerFlagDefinition(new FlagDef_NoMonsterSpawns(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoMonsters(this.flagManager, plugin));
            
            FlagDef_AllowPvP allowPvPDef = new FlagDef_AllowPvP(this.flagManager, plugin);
            allowPvPDef.firstTimeSetup();
            this.flagManager.registerFlagDefinition(allowPvPDef);

            this.flagManager.registerFlagDefinition(new FlagDef_EnterActionbar(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_ExitActionbar(this.flagManager, plugin));

            this.flagManager.registerFlagDefinition(new FlagDef_EnterMessage(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_ExitMessage(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_EnterCommand(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_EnterPlayerCommand(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_ExitCommand(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_ExitPlayerCommand(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_RespawnLocation(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_KeepInventory(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_InfiniteArrows(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_KeepLevel(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_KeepLoaded(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NetherPortalPlayerCommand(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NetherPortalConsoleCommand(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoCombatLoot(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoMapMaking(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoMobSpawns(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoPlayerDamage(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoPlayerDamageByMonster(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoEnter(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoMobDamage(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoFluidFlow(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_HealthRegen(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoHunger(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_CommandWhiteList(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_CommandBlackList(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoStructureGrowth(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoBlockFade(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_AllowInfest(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoCoralDeath(this.flagManager, plugin));

            FlagDef_NoFlight noFlight = new FlagDef_NoFlight(this.flagManager, plugin);
            noFlight.firstTimeSetup();
            this.flagManager.registerFlagDefinition(noFlight);
            
            this.flagManager.registerFlagDefinition(new FlagDef_TrappedDestination(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoLootProtection(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoEnderPearl(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoExpiration(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoLeafDecay(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoPetDamage(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoWeatherChange(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoItemPickup(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoChorusFruit(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_SpleefArena(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoItemDrop(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoGrowth(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_OwnerFly(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_OwnerMemberFly(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_PermissionFly(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoEnterPlayer(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_PlayerWeather(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_PlayerTime(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_PlayerGamemode(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoVineGrowth(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoSnowForm(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoIceForm(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoFireSpread(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoFireDamage(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoFallDamage(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_EnterCommand_Owner(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_EnterCommand_Members(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_ExitCommand_Owner(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_ExitCommand_Members(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoExplosionDamage(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_AllowBlockExplosions(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_ProtectNamedMobs(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoBlockGravity(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_ChangeBiome(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoOpenDoors(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoVehicle(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoBlockForm(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoBlockSpread(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoDripstoneSpread(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoMobSpawnsType(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoItemDamage(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoElytra(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NotifyEnter(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NotifyExit(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoPotionEffects(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_SpawnReasonWhitelist(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_NoCropTrampling(this.flagManager, plugin));

            this.flagManager.registerFlagDefinition(new FlagDef_ViewContainers(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_ReadLecterns(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_AllowWitherDamage(this.flagManager, plugin));

            this.flagManager.registerFlagDefinition(new FlagDef_AllowVillagerTrading(this.flagManager, plugin));
            this.flagManager.registerFlagDefinition(new FlagDef_RestoreGrazedGrass(this.flagManager, plugin));

            try {
                Class.forName("org.bukkit.event.raid.RaidTriggerEvent");
                this.flagManager.registerFlagDefinition(new FlagDef_RaidMemberOnly(this.flagManager, plugin));
            } catch (ClassNotFoundException ignored) {
            }

            try {
                Class.forName("com.destroystokyo.paper.event.block.AnvilDamagedEvent");
                this.flagManager.registerFlagDefinition(new FlagDef_NoAnvilDamage(this.flagManager, plugin));
            }
            // if failed, we just won't have this flag available
            catch (ClassNotFoundException ignore) {}

            //try to hook into mcMMO
            try {
                if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
                    this.flagManager.registerFlagDefinition(new FlagDef_NoMcMMOSkills(this.flagManager, plugin));
                    this.flagManager.registerFlagDefinition(new FlagDef_NoMcMMODeathPenalty(this.flagManager, plugin));
                    // Experimental
                    this.flagManager.registerFlagDefinition(new FlagDef_NoMcMMOXP(this.flagManager, plugin));
                }
            }
            //if failed, we just won't have those flags available
            catch (NoClassDefFoundError ignore) {}

            // EliteMob flags
            try {
                if (Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
                    this.flagManager.registerFlagDefinition(new FlagDef_NoEliteMobSpawns(this.flagManager, plugin));
                }
            } catch (NoClassDefFoundError ignored) {}

            // vault-reliant flags
            try {
                if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                    plugin.getCommand("buybuildtrust").setExecutor(new CommandBuyBuildTrust());
                    plugin.getCommand("buycontainertrust").setExecutor(new CommandBuyContainerTrust());
                    plugin.getCommand("buyaccesstrust").setExecutor(new CommandBuyAccessTrust());
                    plugin.getCommand("buysubclaim").setExecutor(new CommandBuySubclaim());

                    this.flagManager.registerFlagDefinition(new FlagDef_BuyBuildTrust(this.flagManager, plugin));
                    this.flagManager.registerFlagDefinition(new FlagDef_BuyContainerTrust(this.flagManager, plugin));
                    this.flagManager.registerFlagDefinition(new FlagDef_BuyAccessTrust(this.flagManager, plugin));
                    this.flagManager.registerFlagDefinition(new FlagDef_BuySubclaim(this.flagManager, plugin));
                }
            } catch (NoClassDefFoundError ignored) {}
        } else {
            // Update world settings for flags (probably on a reload)
            this.flagManager.getFlagDefinitions().forEach(flagDefinition -> flagDefinition.updateSettings(plugin.getWorldSettingsManager()));
        }

        try {
            File flagsFile = new File(FlagsDataStore.flagsFilePath);
            List<MessageSpecifier> errors = this.flagManager.load(flagsFile);
            if (errors.size() > 0) {
                File errorFile = new File(FlagsDataStore.flagsErrorFilePath);
                Files.copy(flagsFile, errorFile);
                for (MessageSpecifier error : errors) {
                    MessagingUtil.sendMessage(null, "Load Error: " + plugin.getFlagsDataStore().getMessage(error.messageID, error.messageParams));
                }
                MessagingUtil.sendMessage(null, "<red>Problems encountered reading the flags data file! " +
                        "Please share this log and your 'flagsError.yml' and 'flags.yml' files with the developer.");
            }
        } catch (Throwable e) {
            MessagingUtil.sendMessage(null, "<red>Unable to initialize the file system data store.  Details:");
            MessagingUtil.sendMessage(null, e.getMessage());
        }

        //drop any flags which no longer correspond to existing land claims (maybe they were deleted)
        Collection<Claim> topLevelClaims = GriefPrevention.instance.dataStore.getClaims();
        HashSet<String> validIDs = new HashSet<>();
        for (Claim claim : topLevelClaims) {
            validIDs.add(claim.getID().toString());
            for (Claim subclaim : claim.children) {
                validIDs.add(subclaim.getID().toString());
            }
        }
        this.flagManager.removeExceptClaimIDs(validIDs);
        MessagingUtil.sendMessage(null, "Finished loading data.");
    }
}
