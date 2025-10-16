package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.CommandList;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.SetFlagResult;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ConcurrentHashMap;

public abstract class CommandListFlagDefinition extends FlagDefinition {
    public CommandListFlagDefinition(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    private static final ConcurrentHashMap<String, CommandList> commandListMap = new ConcurrentHashMap<>();

    protected boolean commandInList(String flagParameters, String commandLine) {
        CommandList list = commandListMap.get(flagParameters);
        if (list == null) {
            list = new CommandList(flagParameters);
            commandListMap.put(flagParameters, list);
        }

        String command = commandLine.split(" ")[0];
        return list.Contains(command);
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty()) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.CommandListRequired));
        }

        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

}
