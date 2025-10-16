package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFromToEvent;

import java.util.Arrays;
import java.util.List;

public class FlagDef_NoFluidFlow extends FlagDefinition {

    private Location previousLocation = null;
    private boolean previousWasCancelled = false;

    public FlagDef_NoFluidFlow(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Location location = event.getBlock().getLocation();
        if (location.equals(this.previousLocation)) {
            if (!this.previousWasCancelled) return;
            event.setCancelled(true);
            return;
        }

        Flag flag = this.getFlagInstanceAtLocation(location, null);
        boolean cancel = (flag != null);

        this.previousLocation = location;
        this.previousWasCancelled = cancel;
        if (cancel) {
            event.setCancelled(true);
        }
    }

    @Override
    public String getName() {
        return "NoFluidFlow";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnableNoFluidFlow);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisableNoFluidFlow);
    }

}
