package me.ryanhamshire.GPFlags.event;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Called when a player tries to change regions.
 * It's expected that the same code that calls this event will also handle what happens when this gets cancelled
 */
@SuppressWarnings("unused")
public class PlayerPreClaimBorderEvent extends PlayerEvent implements Cancellable {

    private static HandlerList handlerList = new HandlerList();
    private Claim claimFrom, claimTo;
    private Location locFrom, locTo;
    private boolean cancelled = false;

    public PlayerPreClaimBorderEvent(Player who, Claim claimFrom, Claim claimTo, Location from, Location to) {
        super(who);
        this.claimFrom = claimFrom;
        this.claimTo = claimTo;
        this.locFrom = from;
        this.locTo = to;
    }

    /**
     * Get the claim the player exited
     * Null if there is no claim at the location
     * @return Claim the player exited (can be null)
     */
    public Claim getClaimFrom() {
        return claimFrom;
    }

    /**
     * Get the claim the player entered
     * Null if there is no claim at the location
     * @return Claim the player entered 
     */
    public Claim getClaimTo() {
        return claimTo;
    }

    /**
     * Get the location the player moved from
     * This is adjusted to be within world height
     * @return Location the player moved from
     */
    public Location getLocFrom() {
        return locFrom;
    }

    /**
     * Get the location the player moved to
     * This is adjusted to be within world height
     * @return Location the player moved to
     */
    public Location getLocTo() {
        return locTo;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

}
