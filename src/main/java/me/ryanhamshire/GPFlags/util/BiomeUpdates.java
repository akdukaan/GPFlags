package me.ryanhamshire.GPFlags.util;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/** Sends the dedicated biome update used by modern Mojang-mapped Paper servers. */
public final class BiomeUpdates {
    private final Plugin plugin;
    private Method getHandle;
    private Method getChunk;
    private Method getChunkSource;
    private Field chunkMap;
    private Method resendBiomes;
    private boolean unavailable;

    public BiomeUpdates(Plugin plugin) {
        this.plugin = plugin;
    }

    // Called on the server thread after all biome writes for this chunk have finished.
    public void send(World world, int x, int z) {
        if (!unavailable) {
            try {
                if (getHandle == null) {
                    initialize(world);
                }
                Object level = getHandle.invoke(world);
                Object chunk = getChunk.invoke(level, x, z);
                Object map = chunkMap.get(getChunkSource.invoke(level));
                resendBiomes.invoke(map, Collections.singletonList(chunk));
                return;
            } catch (ReflectiveOperationException | RuntimeException e) {
                unavailable = true;
                plugin.getLogger().log(Level.WARNING,
                        "Cannot send biome update packets on this server; falling back to chunk refresh. "
                                + "Players may need to relog to see biome changes.", e);
            }
        }
        world.refreshChunk(x, z);
    }

    private void initialize(World world) throws ReflectiveOperationException {
        Method handle = world.getClass().getMethod("getHandle");
        Object level = handle.invoke(world);
        Method chunk = level.getClass().getMethod("getChunk", int.class, int.class);
        Method source = level.getClass().getMethod("getChunkSource");
        Object cache = source.invoke(level);
        Field map = cache.getClass().getField("chunkMap");
        Object mapInstance = map.get(cache);
        Method resend = mapInstance.getClass().getMethod("resendBiomesForChunks", List.class);
        // Publish only after every method has been resolved successfully.
        getChunk = chunk;
        getChunkSource = source;
        chunkMap = map;
        resendBiomes = resend;
        getHandle = handle;
    }
}
