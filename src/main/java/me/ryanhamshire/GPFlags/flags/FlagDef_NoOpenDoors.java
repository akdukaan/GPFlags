package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FlagDef_NoOpenDoors extends FlagDefinition {

    public FlagDef_NoOpenDoors(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler
    public void onDoorOpen(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = e.getClickedBlock();
            assert block != null;
            Flag flag = this.getFlagInstanceAtLocation(block.getLocation(), player);
            if (flag == null) return;

            if (block.getBlockData() instanceof Openable) {

                PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
                Claim claim = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), false, playerData.lastClaim);

                String[] params = null;
                if (!flag.parameters.isEmpty()) {
                    params = flag.parameters.split(",");
                }

                if (!Util.canAccess(claim, player)) {
                    if (params != null) {
                        for (String param : params) {
                            if (param.equalsIgnoreCase("doors") && block.getBlockData() instanceof Door) {
                                e.setCancelled(true);
                                MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoOpenDoorMessage, param);
                            }
                            if (param.equalsIgnoreCase("trapdoors") && block.getBlockData() instanceof TrapDoor) {
                                e.setCancelled(true);
                                MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoOpenDoorMessage, param);
                            }
                            if (param.equalsIgnoreCase("gates") && block.getBlockData() instanceof Gate) {
                                e.setCancelled(true);
                                MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoOpenDoorMessage, param);
                            }
                        }
                    } else {
                        e.setCancelled(true);
                        MessagingUtil.sendMessage(player, TextMode.Err, Messages.NoOpenDoorMessage, "doors");
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "NoOpenDoors";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        if (parameters.isEmpty()) {
            return new MessageSpecifier(Messages.EnableNoOpenDoor);
        } else {
            return new MessageSpecifier(Messages.EnableNoOpenDoor, parameters);
        }
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoOpenDoor);
    }

}
