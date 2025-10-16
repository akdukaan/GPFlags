package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlagDef_ChangeBiome extends FlagDefinition {

    public FlagDef_ChangeBiome(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    /**
     * Runs the other changeBiome and then refreshes chunks in the claim
     * @param claim
     * @param biome
     */
    private void changeBiome(Claim claim, Biome biome) {
        Location greater = claim.getGreaterBoundaryCorner();
        greater.setY(Util.getMaxHeight(greater));
        Location lesser = claim.getLesserBoundaryCorner();
        int lX = (int) lesser.getX();
        int lY = (int) lesser.getY();
        int lZ = (int) lesser.getZ();
        int gX = (int) greater.getX();
        int gY = (int) greater.getY();
        int gZ = (int) greater.getZ();
        World world = lesser.getWorld();
        int ticks = 0;
        for (int x = lX; x < gX; x++) {
            // We don't loop over y because then all chunks would get loaded in the same runnable
            // and it's better to split that up
            int finalX = x;
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    for (int z = lZ; z < gZ; z++) {
                        Location loadLoc = new Location(world, finalX, 100, z);
                        Chunk loadChunk = loadLoc.getChunk();
                        if (!(loadChunk.isLoaded())) {
                            loadChunk.load();
                        }
                        for (int y = lY; y <= gY; y++) {
                            world.setBiome(finalX, y, z, biome);
                        }
                    }
                }
            };
            runnable.runTaskLater(GPFlags.getInstance(), ticks++);
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                refreshChunks(claim);
            }
        };
        runnable.runTaskLater(GPFlags.getInstance(), ticks);
    }

    private void refreshChunks(Claim claim) {
        int view = Bukkit.getServer().getViewDistance();
        Player player = Bukkit.getPlayer(claim.getOwnerName());
        if (player != null && player.isOnline()) {
            Location loc = player.getLocation();
            if (claim.contains(loc, true, true)) {
                int X = loc.getChunk().getX();
                int Z = loc.getChunk().getZ();
                for (int x = X - view; x <= (X + view); x++) {
                    for (int z = Z - view; z <= (Z + view); z++) {
                        player.getWorld().refreshChunk(x, z);
                    }
                }
            }
        }
    }

    /**
     * Validates biome name and permissions and then runs the changeBiome command
     * @param sender
     * @param claim
     * @param biome
     * @return
     */
    public boolean changeBiome(CommandSender sender, Claim claim, String biome) {
        Biome b;
        try {
            b = Biome.valueOf(biome);
        } catch (Throwable e) {
            sender.sendMessage("<red>Invalid biome");
            return false;
        }
        World world = claim.getLesserBoundaryCorner().getWorld();
        if (world == null) {
            sender.sendMessage("<red>World does not exist");
            return false;
        }
        if (!sender.hasPermission("gpflags.flag.changebiome." + biome)) {
            MessagingUtil.sendMessage(sender,"<red>You do not have permissions for the biome <aqua>" + biome + " <red>." );
            return false;
        }
        changeBiome(claim, b);
        return true;
    }

    public void resetBiome(Claim claim) {
        // Get the corners that we care about
        Location greater = claim.getGreaterBoundaryCorner();
        greater.setY(Util.getMaxHeight(greater));
        Location lesser = claim.getLesserBoundaryCorner();

        int lX = lesser.getBlockX();
        int lY = lesser.getBlockY();
        int lZ = lesser.getBlockZ();
        int gX = greater.getBlockX();
        int gY = greater.getBlockY();
        int gZ = greater.getBlockZ();
        World world = lesser.getWorld();

        // Get a list of chunks that we're going to process
        List<Chunk> chunks = new ArrayList<>();
        int lXChunk = lX >> 4;
        int lZChunk = lZ >> 4;
        int gXChunk = gX >> 4;
        int gZChunk = gZ >> 4;

        for (int chunkX = lXChunk; chunkX <= gXChunk; chunkX++) {
            for (int chunkZ = lZChunk; chunkZ <= gZChunk; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                chunks.add(chunk);
            }
        }

        // Build a runnable that gets the chunk snapshot and processes it if within coords
        int ticks = 0;
        for (Chunk chunk : chunks) {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    // Make sure chunk is loaded
                    if (!(chunk.isLoaded())) {
                        chunk.load();
                    }
                    // Get a snapshot of the chunk
                    int chunkX = chunk.getX();
                    int chunkZ = chunk.getZ();
                    // Unfortunately, this only gets the existing chunk, and not a new one based on the seed.
                    // Going to remove the attempt to regenerate the chunk instead of fixing it since it's not easy.
                    // If an API is added in the future, will use the API and add in the functionality to GPFlags.
                    // Otherwise, would need code similar to worldedits, but we need to only do the biome parts of it.
                    // Eg, create a new temporary world, generate parts of it, copy over biome data, and then delete it.
                    // https://github.com/EngineHub/WorldEdit/blob/97630e6d5099242ef7129089caf60b60b8db8af8/worldedit-bukkit/adapters/adapter-1.21.3/src/main/java/com/sk89q/worldedit/bukkit/adapter/impl/v1_21_3/PaperweightAdapter.java#L714
                    ChunkSnapshot chunkSnapshot = world.getEmptyChunkSnapshot(chunkX, chunkZ, true, false);

                    // Loop through coordinates within the chunk and set the biome
                    chunkX = chunkX * 16;
                    chunkZ = chunkZ * 16;
                    for (int x = 0; x < 16; x++) {
                        int worldX = chunkX + x;
                        if (worldX >= lX && worldX <= gX) {
                            for (int z = 0; z < 16; z++) {
                                int worldZ = chunkZ + z;
                                if (worldZ >= lZ && worldZ <= gZ) {
                                    for (int worldY = lY; worldY < gY; worldY++) {
                                        Location location = new Location(world, worldX, worldY, worldZ);
                                        Biome biome = chunkSnapshot.getBiome(x, worldY, z);
                                        world.setBiome(location, biome);
                                        System.out.println("The biome at " + worldX + " " + worldY + " " + worldZ + " has been set to " + biome.name());
                                    }
                                }
                            }
                        }
                    }
                }
            };
            runnable.runTaskLater(GPFlags.getInstance(), ticks++);
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                refreshChunks(claim);
            }
        };
        runnable.runTaskLater(GPFlags.getInstance(), ticks);
    }

    @EventHandler
    public void onClaimDelete(ClaimDeletedEvent e) {
        Claim claim = e.getClaim();
        Claim parent = claim.parent;
        if (parent != null) {
            Flag flag = getEffectiveFlag(parent, parent.getLesserBoundaryCorner().getWorld());
            changeBiome(Bukkit.getConsoleSender(), claim, flag.parameters);
            return;
        }
        if (getEffectiveFlag(claim, claim.getLesserBoundaryCorner().getWorld()) == null) return;

//        resetBiome(claim);
    }

    @Override
    public String getName() {
        return "ChangeBiome";
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty()) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.MessageRequired));
        }

        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.ChangeBiomeSet, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.ChangeBiomeUnset);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Collections.singletonList(FlagType.CLAIM);
    }
}
