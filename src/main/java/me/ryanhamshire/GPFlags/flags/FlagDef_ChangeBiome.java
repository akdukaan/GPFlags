package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
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
                            ChunkSnapshot chunkSnapshot = world.getEmptyChunkSnapshot(finalX >> 4, y >> 4, true, false);
                            Biome biome = chunkSnapshot.getBiome(Math.floorMod(finalX, 16), Math.floorMod(y, 16), Math.floorMod(z, 16));
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

    @EventHandler
    public void onClaimDelete(ClaimDeletedEvent e) {
        if (e.getClaim().parent != null) {
            // todo here is where we'd get the parent's flag and set the inner biome to that
            return;
        }
        Claim claim = e.getClaim();
        FlagManager fm = GPFlags.getInstance().getFlagManager();
        if (fm.getEffectiveFlag(claim.getLesserBoundaryCorner(), this.getName(), claim) == null) return;

        resetBiome(claim);
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
