package me.ryanhamshire.GPFlags;

import me.ryanhamshire.GPFlags.event.PlayerPostClaimBorderEvent;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Remembers which claim each player was last known to be in, according to the
 * {@link PlayerPostClaimBorderEvent}s that GPFlags has fired.
 * <p>
 * Movement flags used to detect claim changes by comparing the claim at the "from" and "to"
 * locations of a single movement event. Anything that moves a player without firing a movement
 * event (pistons, seat plugins, claim resizes, etc.) was invisible to that check, and every
 * movement afterwards looked like it stayed in the same claim. By comparing against the last
 * claim we told flags about instead, a missed transition is detected on the player's next
 * movement (or the next explicit {@link #sync(Player)}), and a Post event is only ever fired
 * when the stored context actually differs, so transitions can't be reported twice.
 * </p>
 */
public class ClaimContextTracker implements Listener {

    private static final Map<UUID, Context> contexts = new HashMap<>();

    private static class Context {
        private final @Nullable Claim claim;
        private final @NotNull Location location;

        private Context(@Nullable Claim claim, @NotNull Location location) {
            this.claim = claim;
            this.location = location;
        }
    }

    /**
     * Fires a {@link PlayerPostClaimBorderEvent}, recording the new claim context first.
     * All Post events should be fired through this method.
     * <p>
     * The context is updated before the event is called so that anything a flag does in
     * response (e.g. NoEnter ejecting the player) is measured against the new context
     * instead of being treated as another missed transition.
     * </p>
     */
    public static void callPostEvent(@NotNull PlayerPostClaimBorderEvent event) {
        Location locTo = event.getLocTo();
        if (locTo != null) {
            contexts.put(event.getPlayer().getUniqueId(), new Context(event.getClaimTo(), locTo));
        }
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Fires a Post event if the claim at the given location differs from the player's last
     * known claim context.
     * @param player The player to check
     * @param claim The claim at location, or null if none
     * @param location A bound-adjusted location where the player currently is
     */
    public static void catchUp(@NotNull Player player, @Nullable Claim claim, @NotNull Location location) {
        Context context = contexts.get(player.getUniqueId());
        if (context == null) {
            // We have no history for this player (e.g. plugin was loaded while they were online).
            // Record the current context without firing anything.
            contexts.put(player.getUniqueId(), new Context(claim, location));
            return;
        }
        if (isSameContext(context.claim, context.location.getWorld(), claim, location.getWorld())) return;
        callPostEvent(new PlayerPostClaimBorderEvent(player, context.claim, claim, context.location, location));
    }

    /**
     * Looks up the claim at the player's current location and fires a Post event if they've
     * ended up in a different claim than the one flags last knew about.
     */
    public static void sync(@NotNull Player player) {
        if (!player.isOnline()) return;
        Location location = Util.getInBoundsLocation(player);
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        catchUp(player, claim, location);
    }

    /**
     * Runs {@link #sync(Player)} after the given number of ticks
     */
    public static void syncLater(@NotNull Player player, int ticks) {
        Bukkit.getScheduler().runTaskLater(GPFlags.getInstance(), () -> sync(player), ticks);
    }

    /**
     * Syncs every player in the world after the given number of ticks.
     * Used when claim boundaries change, since players can end up on either side of the new border.
     */
    public static void syncWorldLater(@NotNull World world, int ticks) {
        Bukkit.getScheduler().runTaskLater(GPFlags.getInstance(), () -> {
            for (Player player : world.getPlayers()) {
                sync(player);
            }
        }, ticks);
    }

    /**
     * Syncs every player whose last known context is the given claim or one of its subclaims.
     */
    public static void syncPlayersIn(@NotNull Claim claim) {
        List<Player> affected = new ArrayList<>();
        for (Map.Entry<UUID, Context> entry : contexts.entrySet()) {
            Claim stored = entry.getValue().claim;
            if (stored == null) continue;
            if (isSameClaim(stored, claim) || (stored.parent != null && isSameClaim(stored.parent, claim))) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) affected.add(player);
            }
        }
        // Collected first since sync modifies the map
        for (Player player : affected) {
            sync(player);
        }
    }

    /**
     * Records the current context of every online player without firing any events.
     * Called on enable so that players online during a reload have a baseline.
     */
    public static void initialize() {
        contexts.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location location = Util.getInBoundsLocation(player);
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
            contexts.put(player.getUniqueId(), new Context(claim, location));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        contexts.remove(event.getPlayer().getUniqueId());
    }

    private static boolean isSameContext(@Nullable Claim a, @Nullable World worldA, @Nullable Claim b, @Nullable World worldB) {
        if (a == null && b == null) return worldA == worldB;
        if (a == null || b == null) return false;
        return isSameClaim(a, b);
    }

    private static boolean isSameClaim(@NotNull Claim a, @NotNull Claim b) {
        if (a == b) return true;
        Long idA = a.getID();
        Long idB = b.getID();
        if (idA == null || idB == null) return false;
        return Objects.equals(idA, idB);
    }
}
