package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.gui.FlagGUI;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command for opening the Flags GUI
 */
public class CommandFlagsGUI implements CommandExecutor {

    /**
     * Handles the command to open the GUI
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessagingUtil.sendMessage(sender, "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permissions
        if (!player.hasPermission("gpflags.command.flagsgui")) {
            MessagingUtil.sendMessage(player, "You don't have permission to use this command.");
            return true;
        }
        
        // Open the GUI menu
        FlagGUI.getInstance().openMainMenu(player);
        return true;
    }
} 