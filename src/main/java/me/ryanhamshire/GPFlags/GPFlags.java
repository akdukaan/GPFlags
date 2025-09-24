package me.ryanhamshire.GPFlags;

import java.util.*;
import java.util.logging.Level;

import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.GPFlags.commands.*;
import me.ryanhamshire.GPFlags.flags.FlagDefinition;
import me.ryanhamshire.GPFlags.hooks.PlaceholderApiHook;
import me.ryanhamshire.GPFlags.listener.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import me.ryanhamshire.GPFlags.flags.FlagDef_ViewContainers;

/**
 * <b>Main GriefPrevention Flags class</b>
 */
public class GPFlags extends JavaPlugin {

    private static GPFlags instance;
    private FlagsDataStore flagsDataStore;
    private final FlagManager flagManager = new FlagManager();
    private WorldSettingsManager worldSettingsManager;
    boolean registeredFlagDefinitions = false;
    private PlayerListener playerListener;

    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;

        this.playerListener = new PlayerListener();
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        try {
            Class.forName("io.papermc.paper.event.entity.EntityMoveEvent");
            Bukkit.getPluginManager().registerEvents(new EntityMoveListener(), this);
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent");
            Bukkit.getPluginManager().registerEvents(new ClaimResizeListener(), this);
        } catch (ClassNotFoundException e) {
            Bukkit.getPluginManager().registerEvents(new ClaimModifiedListener(), this);
        }
        Bukkit.getPluginManager().registerEvents(new ClaimCreatedListener(), this);
        Bukkit.getPluginManager().registerEvents(new ClaimTransferListener(), this);
        Bukkit.getPluginManager().registerEvents(new FlightManager(), this);

        this.flagsDataStore = new FlagsDataStore();
        reloadConfig();

        // Register Commands
        getCommand("allflags").setExecutor(new CommandAllFlags());
        getCommand("gpflags").setExecutor(new CommandGPFlags());
        getCommand("listclaimflags").setExecutor(new CommandListClaimFlags());
        getCommand("setclaimflag").setExecutor(new CommandSetClaimFlag());
        getCommand("setclaimflagplayer").setExecutor(new CommandSetClaimFlagPlayer());
        getCommand("setdefaultclaimflag").setExecutor(new CommandSetDefaultClaimFlag());
        getCommand("setserverflag").setExecutor(new CommandSetServerFlag());
        getCommand("setworldflag").setExecutor(new CommandSetWorldFlag());
        getCommand("unsetclaimflag").setExecutor(new CommandUnsetClaimFlag());
        getCommand("unsetclaimflagplayer").setExecutor(new CommandUnsetClaimFlagPlayer());
        getCommand("unsetdefaultclaimflag").setExecutor(new CommandUnsetDefaultClaimFlag());
        getCommand("unsetserverflag").setExecutor(new CommandUnsetServerFlag());
        getCommand("unsetworldflag").setExecutor(new CommandUnsetWorldFlag());
        getCommand("bulksetflag").setExecutor(new CommandBulkSetFlag());
        getCommand("bulkunsetflag").setExecutor(new CommandBulkUnsetFlag());

        UpdateChecker.run(this, "gpflags");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderApiHook(this).register();
        }

        try {
            addCustomMetrics();
        } catch (Throwable e) {
            getLogger().log(Level.WARNING, "Error enabling metrics", e);
        }

        float finish = (float) (System.currentTimeMillis() - start) / 1000;
        MessagingUtil.sendMessage(null, "Successfully loaded in " + String.format("%.2f", finish) + " seconds");
    }

    public void onDisable() {
        FlagDef_ViewContainers.getViewingInventories().forEach(inv -> {
            inv.setContents(new ItemStack[inv.getSize()]);
            new ArrayList<>(inv.getViewers()).forEach(HumanEntity::closeInventory);
        });
        flagsDataStore = null;
        instance = null;
        playerListener = null;
    }


    /**
     * Reload the config file
     */
    public void reloadConfig() {
        this.worldSettingsManager = new WorldSettingsManager();
        new GPFlagsConfig(this);
    }

    /**
     * Get an instance of this plugin
     *
     * @return Instance of this plugin
     */
    public static GPFlags getInstance() {
        return instance;
    }

    /**
     * Get an instance of the flags data store
     *
     * @return Instance of the flags data store
     */
    public FlagsDataStore getFlagsDataStore() {
        return this.flagsDataStore;
    }

    /**
     * Get an instance of the flag manager
     *
     * @return Instance of the flag manager
     */
    public FlagManager getFlagManager() {
        return this.flagManager;
    }

    /**
     * Get an instance of the world settings manager
     *
     * @return Instance of the world settings manager
     */
    public WorldSettingsManager getWorldSettingsManager() {
        return this.worldSettingsManager;
    }

    private void addCustomMetrics() {
        Metrics bStats = new Metrics(this, 17786);

        Set<String> usedFlags = GPFlags.getInstance().getFlagManager().getUsedFlags();
        Collection<FlagDefinition> defs = GPFlags.getInstance().getFlagManager().getFlagDefinitions();
        for (FlagDefinition def : defs) {
            bStats.addCustomChart(new SimplePie("using_" + def.getName().toLowerCase(), () -> {
                return String.valueOf(usedFlags.contains(def.getName().toLowerCase()));
            }));
        }
        bStats.addCustomChart(new SimplePie("griefprevention_version", () -> {
            return GriefPrevention.instance.getDescription().getVersion();
        }));

        String serverVersion = getServer().getBukkitVersion().split("-")[0];

        bStats.addCustomChart(createStaticDrilldownStat("version_mc_plugin", serverVersion, getDescription().getVersion()));
        bStats.addCustomChart(createStaticDrilldownStat("version_plugin_mc", getDescription().getVersion(), serverVersion));

        bStats.addCustomChart(createStaticDrilldownStat("version_brand_plugin", getServer().getName(), getDescription().getVersion()));
        bStats.addCustomChart(createStaticDrilldownStat("version_plugin_brand", getDescription().getVersion(), getServer().getName()));

        bStats.addCustomChart(createStaticDrilldownStat("version_mc_brand", serverVersion, getServer().getName()));
        bStats.addCustomChart(createStaticDrilldownStat("version_brand_mc", getServer().getName(), serverVersion));
    }

    private static DrilldownPie createStaticDrilldownStat(String statId, String value1, String value2) {
        final Map<String, Map<String, Integer>> map = ImmutableMap.of(value1, ImmutableMap.of(value2, 1));
        return new DrilldownPie(statId, () -> map);
    }

}
