package me.ryanhamshire.GPFlags.hooks;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionBuilder;
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
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;

public class WorldEditHook {

    public static void resetBiomes(Claim claim) {
        try {
            // Get the corners that we care about
            Location greaterCorner = claim.getGreaterBoundaryCorner();
            greaterCorner.setY(Util.getMaxHeight(greaterCorner));
            Location lesserCorner = claim.getLesserBoundaryCorner();

            Region region = new CuboidRegion(
                    BukkitAdapter.asBlockVector(lesserCorner),
                    BukkitAdapter.asBlockVector(greaterCorner)
            );
            World weWorld = BukkitAdapter.adapt(lesserCorner.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {

                // Create a custom extent that only modifies biomes
                Extent biomeOnlyExtent = new AbstractDelegateExtent(editSession) {
                    @Override
                     public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) {
                        // Prevent block changes
                        return false;
                    }

                    @Override
                    public boolean setBiome(BlockVector3 position, BiomeType biome) {
                        // Allow biome modifications
                        return super.setBiome(position, biome);
                    }
                };

                RegenOptions options = RegenOptions.builder().regenBiomes(true).build();
                weWorld.regenerate(region, biomeOnlyExtent, options);
                Operations.complete(editSession.commit());

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (NoClassDefFoundError error) {
            return;
        }
    }
}