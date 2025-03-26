package me.ryanhamshire.GPFlags.hooks;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.World;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;

public class WorldEditHook {

    /**
     * Do not use. This resets everything in the claim
     * Need to somehow adapt this to mask out block data
     *
     * I was told: somehow get a custom extent in there,
     * the MaskingExtent tests for biome-setting as well,
     * which would make EditSession#setMask not really suitable
     * @param claim
     */
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

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld);) {

                RegenOptions options = RegenOptions.builder().regenBiomes(true).build();
                weWorld.regenerate(region, editSession, options);
                Operations.complete(editSession.commit());

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (NoClassDefFoundError error) {
            return;
        }
    }
}