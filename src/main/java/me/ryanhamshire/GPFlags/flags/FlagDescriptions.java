package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.GPFlags;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/** Configurable plain text descriptions used by claim flag tooltips and search. */
public final class FlagDescriptions {
    private static final String FILE_NAME = "flag-descriptions.yml";
    private static volatile Map<String, String> descriptions = Collections.emptyMap();

    private FlagDescriptions() {
    }

    public static void reload(GPFlags plugin) {
        Map<String, String> loaded = new HashMap<>();
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(plugin.getResource(FILE_NAME)), StandardCharsets.UTF_8)) {
            readDescriptions(YamlConfiguration.loadConfiguration(reader), loaded);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read bundled flag descriptions", e);
        }

        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) plugin.saveResource(FILE_NAME, false);
        try {
            YamlConfiguration configured = new YamlConfiguration();
            configured.load(file);
            readDescriptions(configured, loaded);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not load " + FILE_NAME + "; using bundled flag descriptions", e);
        }
        descriptions = Collections.unmodifiableMap(loaded);
    }

    private static void readDescriptions(YamlConfiguration config, Map<String, String> target) {
        for (String key : config.getKeys(false)) {
            if (config.isString(key)) target.put(normalize(key), config.getString(key));
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    public static String get(String name) {
        return descriptions.getOrDefault(normalize(name), "Configure " + name + " in this claim.");
    }
}
