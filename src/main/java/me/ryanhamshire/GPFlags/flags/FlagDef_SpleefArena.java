package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.events.PreventBlockBreakEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;

public class FlagDef_SpleefArena extends FlagDefinition {

    public FlagDef_SpleefArena(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location location = player.getLocation();

        Flag flag = this.getFlagInstanceAtLocation(location, player);
        if (flag == null) return;

        SpleefData data = new SpleefData(flag.getParametersArray());
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, playerData.lastClaim);
        if (claim == null) return;
        if (!claim.contains(Util.getInBoundsLocation(player), false, false)) return;


        ArrayList<Chunk> chunks = claim.getChunks();
        for (Chunk chunk : chunks) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = Util.getMinHeight(location); y < Util.getMaxHeight(location) - data.differenceY; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (data.isReference(block)) {
                            chunk.getBlock(x, y + data.differenceY, z).setType(data.actionableMaterial);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPreventBlockBreak(PreventBlockBreakEvent event) {
        Block block = event.getInnerEvent().getBlock();
        Location location = block.getLocation();

        Flag flag = this.getFlagInstanceAtLocation(location, null);
        if (flag == null) return;

        SpleefData data = new SpleefData(flag.getParametersArray());
        if (data.isActionable(block)) {
            event.setCancelled(true);  //break the block
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        Block block = e.getBlock();
        Location location = block.getLocation();

        Flag flag = this.getFlagInstanceAtLocation(location, null);
        if (flag == null) return;
        SpleefData data = new SpleefData(flag.getParametersArray());
        if (data.isActionable(block)) {
            e.setDropItems(false);  //don't drop anything
        }

    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        String[] params = parameters.split(" ");
        String supportMaterialName;
        String blockMaterialName;

        if (params.length != 3) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.SpleefArenaHelp));
        }

        try {
            if (params[0].contains(":")) {
                String[] params_2 = params[0].split(":");
                if (!params_2[0].startsWith("minecraft")) {
                    throw new IllegalArgumentException("Only supports Minecraft blocks");
                }
                blockMaterialName = params_2[1].toUpperCase();

            } else {
                blockMaterialName = params[0].toUpperCase();
            }

            if (params[1].contains(":")) {
                String[] params_2 = params[1].split(":");
                if (!params_2[0].startsWith("minecraft")) {
                    throw new IllegalArgumentException("Only supports Minecraft blocks");
                }
                supportMaterialName = params_2[1].toUpperCase();
            } else {
                supportMaterialName = params[1].toUpperCase();
            }

            if (!isValidMaterial(blockMaterialName)) {
                throw new IllegalArgumentException("Such Material is not found");
            }

            if (!isValidMaterial(supportMaterialName)) {
                throw new IllegalArgumentException("Such Material is not found");
            }

        } catch (IllegalArgumentException e) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.SpleefArenaHelp));
        }

        try {
            Integer.valueOf(params[2]);
        } catch (NumberFormatException e) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.SpleefArenaHelp));
        }

        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    boolean isValidMaterial(String materialName) {
        return (Material.getMaterial(materialName) != null && Material.getMaterial(materialName).isBlock());
    }

    @Override
    public String getName() {
        return "SpleefArena";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.SetSpleefArena);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.UnSetSpleefArena);
    }

    private class SpleefData {
        Material referenceMaterial = null; // bricks
        Material actionableMaterial = null; // snow
        Integer differenceY = null; // high much higher the snow is from the bricks

        SpleefData(String[] params) {

            // Like snow
            if (params[0].contains(":")) {
                String[] params_2 = params[0].split(":");
                actionableMaterial = Material.getMaterial(params_2[1].toUpperCase());
            } else {
                actionableMaterial = Material.getMaterial(params[0].toUpperCase());
            }

            // Like bricks
            if (params[1].contains(":")) {
                String[] params_2 = params[1].split(":");
                referenceMaterial = Material.getMaterial(params_2[1].toUpperCase());
            } else {
                referenceMaterial = Material.getMaterial(params[1].toUpperCase());
            }
            differenceY = Integer.valueOf(params[2]);
        }

        boolean isReference(Block b) {
            return b.getType() == referenceMaterial;
        }

        boolean isActionable(Block b) {
            return b.getType() == actionableMaterial;
        }
    }

    @Override
    public List<FlagType> getFlagType() {
        return List.of(FlagType.CLAIM);
    }

}
