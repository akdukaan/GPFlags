package me.ryanhamshire.GPFlags.event;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player enters/exits a claim
 * Calls onChangeClaim on every movement flag
 */
public class PlayerPostClaimBorderEvent extends PlayerEvent {

    private static HandlerList handlerList = new HandlerList();
    private Claim claimFrom, claimTo;
    private Location locFrom, locTo;

    public PlayerPostClaimBorderEvent(Player who, Claim claimFrom, Claim claimTo, Location from, Location to) {
        super(who);
        this.claimFrom = claimFrom;
        this.claimTo = claimTo;
        this.locFrom = from;
        this.locTo = to;
    }

    public PlayerPostClaimBorderEvent(PlayerPreClaimBorderEvent playerPreClaimBorderEvent) {
        super(playerPreClaimBorderEvent.getPlayer());
        this.claimFrom = playerPreClaimBorderEvent.getClaimFrom();
        this.claimTo = playerPreClaimBorderEvent.getClaimTo();
        this.locFrom = playerPreClaimBorderEvent.getLocFrom();
        this.locTo = playerPreClaimBorderEvent.getLocTo();
    }

    /**
     * Get the claim the player exited
     *
     * @return Claim the player exited
     */
    public @Nullable Claim getClaimFrom() {
        return claimFrom;
    }

    /**
     * Get the claim the player entered
     *
     * @return Claim the player entered
     */
    public @Nullable Claim getClaimTo() {
        return claimTo;
    }

    /**
     * Get the location the player moved from
     *
     * @return Location the player moved from
     */
    public Location getLocFrom() {
        return locFrom;
    }

    /**
     * Get the location the player moved to
     *
     * @return Location the player moved to
     */
    public Location getLocTo() {
        return locTo;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

}
