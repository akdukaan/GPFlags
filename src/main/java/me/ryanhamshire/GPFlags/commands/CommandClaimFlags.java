package me.ryanhamshire.GPFlags.commands;

import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.util.MessagingUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** Keeps optional dialog and sprite APIs out of the executor loaded on older servers. */
public class CommandClaimFlags implements TabExecutor {
    private final TabExecutor dialog = createDialog();

    private static TabExecutor createDialog() {
        try {
            ClassLoader loader = CommandClaimFlags.class.getClassLoader();
            Class.forName("io.papermc.paper.dialog.Dialog", false, loader);
            Class.forName("io.papermc.paper.registry.data.dialog.input.DialogInput", false, loader);
            Class.forName("io.papermc.paper.registry.data.dialog.action.DialogAction", false, loader);
            Class<?> contents = Class.forName("net.kyori.adventure.text.object.ObjectContents", false, loader);
            Class.forName("net.kyori.adventure.text.Component", false, loader).getMethod("object", contents);
            Class<?> dialogLike = Class.forName("net.kyori.adventure.dialog.DialogLike", false, loader);
            Player.class.getMethod("showDialog", dialogLike);
            return (TabExecutor) Class.forName(
                    "me.ryanhamshire.GPFlags.commands.CommandClaimFlagsDialog", true, loader)
                    .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError unavailable) {
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("gpflags.command.setclaimflag")) {
            MessagingUtil.sendMessage(sender, TextMode.Err, Messages.NoCommandPermission, command.toString());
            return true;
        }
        if (!(sender instanceof Player)) {
            MessagingUtil.sendMessage(sender, TextMode.Warn, Messages.PlayerOnlyCommand, command.toString());
            return true;
        }
        if (dialog != null) return dialog.onCommand(sender, command, label, args);
        sender.sendMessage("The claim flag dialog is unavailable on this server. Use /setclaimflag <flag> [parameters] or /unsetclaimflag <flag>.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
