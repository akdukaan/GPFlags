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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FlagDef_ChangeBiome extends FlagDefinition {

    public FlagDef_ChangeBiome(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
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
        for (int x = lX; x <= gX; x++) {
            // We don't loop over y because then all chunks would get loaded in the same runnable
            // and it's better to split that up
            int finalX = x;
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    for (int z = lZ; z <= gZ; z++) {
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
