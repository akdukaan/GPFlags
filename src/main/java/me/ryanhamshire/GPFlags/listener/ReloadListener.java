package me.ryanhamshire.GPFlags.listener;

import better.reload.api.ReloadEvent;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ReloadListener implements Listener {

    @EventHandler
    private void onReload(ReloadEvent event) {
        GPFlags.getInstance().reloadConfig();
        GPFlags.getInstance().getFlagsDataStore().loadMessages();
        MessagingUtil.sendMessage(event.getCommandSender(), TextMode.Success, Messages.ReloadComplete);
    }
}
