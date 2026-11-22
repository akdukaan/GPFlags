package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FlagDef_ChangeBiome extends FlagDefinition {

    public FlagDef_ChangeBiome(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public FlagParameterType getParameterType() {
        return FlagParameterType.BIOME;
    }

    private void changeBiome(Claim claim, Biome biome) {
        Location greater = claim.getGreaterBoundaryCorner();
        Location lesser = claim.getLesserBoundaryCorner();
        int lX = lesser.getBlockX();
        int lY = lesser.getBlockY();
        int lZ = lesser.getBlockZ();
        int gX = greater.getBlockX();
        int gY = Util.getMaxHeight(greater) - 1;
        int gZ = greater.getBlockZ();
        World world = lesser.getWorld();
        int ticks = 0;
        for (int chunkX = lX >> 4; chunkX <= gX >> 4; chunkX++) {
            for (int chunkZ = lZ >> 4; chunkZ <= gZ >> 4; chunkZ++) {
                int finalChunkX = chunkX;
                int finalChunkZ = chunkZ;
                // Finish each chunk before resending it, spreading large claims over multiple ticks.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        world.getChunkAt(finalChunkX, finalChunkZ).load();
                        int minX = Math.max(lX, finalChunkX << 4);
                        int maxX = Math.min(gX, (finalChunkX << 4) + 15);
                        int minZ = Math.max(lZ, finalChunkZ << 4);
                        int maxZ = Math.min(gZ, (finalChunkZ << 4) + 15);
                        for (int x = minX; x <= maxX; x++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                for (int y = lY; y <= gY; y++) {
                                    world.setBiome(x, y, z, biome);
                                }
                            }
                        }
                        // Resend to everyone viewing this chunk, even when the owner is offline.
                        world.refreshChunk(finalChunkX, finalChunkZ);
                    }
                }.runTaskLater(GPFlags.getInstance(), ticks++);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public boolean changeBiome(CommandSender sender, Claim claim, String biome) {
        Biome b = Registry.BIOME.get(NamespacedKey.minecraft(biome.toLowerCase(Locale.ROOT).replace(" ", "_")));
        if (b == null) {
            MessagingUtil.sendMessage(sender, "<red>Invalid biome: <aqua>" + biome);
            return false;
        }
        World world = claim.getLesserBoundaryCorner().getWorld();
        if (world == null) {
            MessagingUtil.sendMessage(sender, "<red>World does not exist.");
            return false;
        }
        String biomeKey = b.getKey().getKey();
        if (!sender.hasPermission("gpflags.flag.changebiome." + biomeKey)) {
            MessagingUtil.sendMessage(sender, "<red>You do not have permissions for the biome <aqua>" + biomeKey + " <red>.");
            return false;
        }
        changeBiome(claim, b);
        return true;
    }

    @EventHandler
    public void onClaimDelete(ClaimDeletedEvent e) {
        Claim claim = e.getClaim();
        Claim parent = claim.parent;
        if (parent != null) {
            Flag flag = getEffectiveFlag(parent, parent.getLesserBoundaryCorner().getWorld());
            if (flag != null) {
                changeBiome(Bukkit.getConsoleSender(), claim, flag.parameters);
            }
            return;
        }
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
