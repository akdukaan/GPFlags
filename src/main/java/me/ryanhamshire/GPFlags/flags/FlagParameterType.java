package me.ryanhamshire.GPFlags.flags;

import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.StringUtil;

import java.util.*;

/** Parameter shape, shared by command completion and dialog controls. */
public enum FlagParameterType {
    BOOLEAN("On / off"), TEXT("Text"), COMMANDS("Commands (separate with ;)"),
    PLAYERS("Player names or UUIDs (separate with spaces)"), NUMBER("Amount"),
    BIOME("Biome"), ENTITY_TYPES("Mob types (separate with ;)"),
    POTION_EFFECTS("Potion effects (separate with spaces, or all)"),
    SPAWN_REASONS("Spawn reasons (separate with spaces)"),
    DOOR_TYPES("Door types (separate with commas)"), LOCATION("World x y z [yaw pitch]"),
    SPLEEF("Surface block, marker block and height"),
    TIME("Time", "day", "noon", "night", "midnight"),
    WEATHER("Weather", "sun", "rain"),
    GAME_MODE("Game mode", "survival", "creative", "adventure", "spectator");

    private final String label;
    private final List<String> choices;

    FlagParameterType(String label, String... choices) {
        this.label = label;
        this.choices = Collections.unmodifiableList(Arrays.asList(choices));
    }

    public String getLabel() { return label; }
    public List<String> getChoices() { return choices; }

    public List<String> complete(CommandSender sender, String[] parameters) {
        List<String> values = new ArrayList<>(choices);
        String current = parameters[parameters.length - 1];
        String prefix = "";
        String delimiter = this == ENTITY_TYPES ? ";" : this == DOOR_TYPES ? "," : null;
        if (delimiter != null && current.contains(delimiter)) {
            prefix = current.substring(0, current.lastIndexOf(delimiter) + 1);
        }
        switch (this) {
            case PLAYERS:
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!(sender instanceof Player) || ((Player) sender).canSee(player)) values.add(player.getName());
                }
                break;
            case ENTITY_TYPES:
                for (EntityType type : EntityType.values()) {
                    String name = type.name().toLowerCase(Locale.ROOT);
                    if (sender.hasPermission("gpflags.flag.nomobspawnstype." + name)) values.add(name);
                }
                break;
            case BIOME:
                Registry.BIOME.forEach(biome -> {
                    String name = biome.getKey().getKey();
                    if (sender.hasPermission("gpflags.flag.changebiome." + name)) values.add(name);
                });
                break;
            case POTION_EFFECTS:
                values.add("all");
                Registry.EFFECT.forEach(effect -> values.add(effect.getName().toLowerCase(Locale.ROOT)));
                break;
            case SPAWN_REASONS:
                for (CreatureSpawnEvent.SpawnReason reason : CreatureSpawnEvent.SpawnReason.values()) values.add(reason.name().toLowerCase(Locale.ROOT));
                break;
            case DOOR_TYPES:
                values.addAll(Arrays.asList("doors", "trapdoors", "gates"));
                break;
            case LOCATION:
                if (parameters.length == 1) Bukkit.getWorlds().forEach(world -> values.add(world.getName()));
                break;
            default:
                break;
        }
        if (parameters.length > 1 && (this == TIME || this == WEATHER || this == GAME_MODE || this == BIOME || this == NUMBER)) return Collections.emptyList();
        List<String> candidates = new ArrayList<>();
        for (String value : values) candidates.add(prefix + value);
        candidates.sort(String.CASE_INSENSITIVE_ORDER);
        return StringUtil.copyPartialMatches(current, candidates, new ArrayList<>());
    }
}
