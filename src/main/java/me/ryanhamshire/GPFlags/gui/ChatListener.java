package me.ryanhamshire.GPFlags.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for chat input for flag parameters
 */
public class ChatListener implements Listener {

    /**
     * Listens for chat messages for parameter input
     * @param event The chat event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // If the FlagGUI processes the input, cancel the chat message
        if (FlagGUI.getInstance().handleChatInput(player, message)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Removes players from active sessions when they log out
     * @param event The quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cleanup can happen in the FlagGUI class if needed
    }
} 