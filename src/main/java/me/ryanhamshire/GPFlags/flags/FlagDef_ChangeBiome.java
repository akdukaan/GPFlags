package me.ryanhamshire.GPFlags.flags;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.hooks.WorldEditHook;
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
     * TODO Hook into worldEdit. It knows what it's doing better
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

    public static void resetBiome(Claim claim) {
        Location greaterCorner = claim.getGreaterBoundaryCorner();
        greaterCorner.setY(Util.getMaxHeight(greaterCorner));
        Location lesserCorner = claim.getLesserBoundaryCorner();

        Region region = new CuboidRegion(
                BukkitAdapter.asBlockVector(lesserCorner),
                BukkitAdapter.asBlockVector(greaterCorner)
        );
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(lesserCorner.getWorld());

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {

            // Create a custom extent that doesn't set blocks
            Extent biomeOnlyExtent = new AbstractDelegateExtent(editSession) {
                @Override
                public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) {
                    // Prevent block changes
                    return false;
                }
            };

            RegenOptions options = RegenOptions.builder().regenBiomes(true).build();
            weWorld.regenerate(region, biomeOnlyExtent, options);
            Operations.complete(editSession.commit());

        } catch (WorldEditException e) {
            e.printStackTrace();
        }
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
