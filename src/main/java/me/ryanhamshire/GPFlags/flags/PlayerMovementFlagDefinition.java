package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.event.PlayerPostClaimBorderEvent;
import me.ryanhamshire.GPFlags.event.PlayerPreClaimBorderEvent;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base flag definition for movement based flags
 * <p>When creating a flag that requires checks for players moving in/out of claims, extend from this class</p>
 */
@SuppressWarnings("WeakerAccess")
public abstract class PlayerMovementFlagDefinition extends FlagDefinition {

    public PlayerMovementFlagDefinition(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    /**
     * A slightly easier way for Movement flags to use PlayerPreClaimBorderEvent
     * @param player
     * @param from
     * @param to
     * @param claimFrom
     * @param claimTo
     * @return false if the flag denied entry or true if it allowed it.
     */
    public boolean allowMovement(Player player, Location from, Location to, Claim claimFrom, Claim claimTo) {
        return true;
    }

    @EventHandler
    public void onPreMove(PlayerPreClaimBorderEvent event) {
        Player player = event.getPlayer();
        Location from = event.getLocFrom();
        Location to = event.getLocTo();
        Claim claimFrom = event.getClaimFrom();
        Claim claimTo = event.getClaimTo();
        if (!this.allowMovement(player, from, to, claimFrom, claimTo)) {
            event.setCancelled(true);
            player.setVelocity(new Vector());
        }
    }

    @EventHandler
    public void onPostMove(PlayerPostClaimBorderEvent event) {
        Flag fromFlag = getEffectiveFlag(event.getClaimFrom(), event.getLocFrom());
        Flag toFlag = getEffectiveFlag(event.getClaimTo(), event.getLocTo());
        if (fromFlag == toFlag) return;
        onChangeClaim(event.getPlayer(), event.getLocFrom(), event.getLocTo(), event.getClaimFrom(), event.getClaimTo(), fromFlag, toFlag);
    }

    /**
     * Called after a player has successfully moved from one region to another.
     * Not called if the flags are the same due to like subclaims
     * @param player
     * @param from A bound-adjusted location or null if a login event
     * @param to A bound-adjusted location
     * @param claimFrom The claim that the player is coming from if one exists
     * @param claimTo The claim that the player is now in if one exists
     */
    public void onChangeClaim(@NotNull Player player, @Nullable Location from, @NotNull Location to, @Nullable Claim claimFrom, @Nullable Claim claimTo, @Nullable Flag fromFlag, @Nullable Flag toFlag) {}

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT);
    }

}
