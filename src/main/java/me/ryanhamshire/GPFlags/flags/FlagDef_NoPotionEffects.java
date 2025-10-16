package me.ryanhamshire.GPFlags.flags;

import me.ryanhamshire.GPFlags.*;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class FlagDef_NoPotionEffects extends PlayerMovementFlagDefinition {

    public FlagDef_NoPotionEffects(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
    }

    @Override
    public void onFlagSet(Claim claim, String string) {
        World world = claim.getLesserBoundaryCorner().getWorld();
        for (Player player : world.getPlayers()) {
            if (claim.contains(Util.getInBoundsLocation(player), false, false)) {
                for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                    PotionEffectType effectType = potionEffect.getType();
                    if (string.equalsIgnoreCase("all")) {
                        player.removePotionEffect(effectType);
                    } else {
                        for (String s : string.split(" ")) {
                            if (effectType.getName().equalsIgnoreCase(s)) {
                                player.removePotionEffect(effectType);
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void onChangeClaim(Player player, Location lastLocation, Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        if (flagTo == null) return;
        if (player.hasPermission("gpflags.bypass.nopotioneffects")) return;

        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            PotionEffectType effectType = potionEffect.getType();

            if (flagTo.parameters.equalsIgnoreCase("all")) {
                player.removePotionEffect(effectType);
            } else {
                String[] paramArray = flagTo.getParametersArray();
                for (String string : paramArray) {
                    if (effectType.getName().equalsIgnoreCase(string)) {
                        player.removePotionEffect(effectType);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        Entity entity = event.getEntity();
        Player player = null;
        if (entity instanceof Player) {
            player = (Player) event.getEntity();
            if (player.hasPermission("gpflags.bypass.nopotioneffects")) return;
        }
        Flag flag = this.getFlagInstanceAtLocation(event.getEntity().getLocation(), player);
        if (flag == null) return;
        PotionEffect potionEffect = event.getNewEffect();
        if (potionEffect == null) return;

        PotionEffectType effectType = potionEffect.getType();
        String[] paramArray = flag.getParametersArray();
        if (flag.parameters.equalsIgnoreCase("all")) {
            event.setCancelled(true);
            return;
        }
        for (String string : paramArray) {
            if (effectType.getName().equalsIgnoreCase(string)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public SetFlagResult validateParameters(String parameters, CommandSender sender) {
        if (parameters.isEmpty()) {
            return new SetFlagResult(false, new MessageSpecifier(Messages.SpecifyPotionEffectName));
        }
        if (parameters.equalsIgnoreCase("all")) {
            return new SetFlagResult(true, this.getSetMessage(parameters));
        }
        for (String s : parameters.split(" ")) {
            PotionEffectType pet = PotionEffectType.getByName(s.toUpperCase());
            if (pet == null) {
                return new SetFlagResult(false, new MessageSpecifier(Messages.NotValidPotionName, s));
            }
        }
        return new SetFlagResult(true, this.getSetMessage(parameters));
    }

    @Override
    public String getName() {
        return "NoPotionEffects";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.EnabledNoPotionEffects, parameters);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.DisabledNoPotionEffects);
    }

}
