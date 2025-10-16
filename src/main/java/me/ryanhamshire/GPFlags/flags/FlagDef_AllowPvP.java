package me.ryanhamshire.GPFlags.flags;

import java.util.*;

import me.ryanhamshire.GPFlags.util.MessagingUtil;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import me.ryanhamshire.GPFlags.Flag;
import me.ryanhamshire.GPFlags.FlagManager;
import me.ryanhamshire.GPFlags.GPFlags;
import me.ryanhamshire.GPFlags.MessageSpecifier;
import me.ryanhamshire.GPFlags.Messages;
import me.ryanhamshire.GPFlags.TextMode;
import me.ryanhamshire.GPFlags.WorldSettings;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.PreventPvPEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlagDef_AllowPvP extends PlayerMovementFlagDefinition {

    private final NamespacedKey infinity = NamespacedKey.minecraft("infinity");
    private final Enchantment infinityEnchantment = Registry.ENCHANTMENT.get(infinity);
    
    // For EntityShootBow event being called multiple times when using the enchantment Multishot
    private Set<Player> justFiredCrossbow = Collections.newSetFromMap(new WeakHashMap<>());
    
    public FlagDef_AllowPvP(FlagManager manager, GPFlags plugin) {
        super(manager, plugin);
        createPositiveEffects();
    }

    private static HashSet POSITIVE_EFFECTS = new HashSet<>();

    public static void createPositiveEffects() {
        try {
            POSITIVE_EFFECTS = new HashSet( Arrays.asList(
                    PotionEffectType.ABSORPTION,
                    PotionEffectType.CONDUIT_POWER,
                    Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("resistance")),
                    PotionEffectType.DOLPHINS_GRACE,
                    Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("haste")),
                    PotionEffectType.FIRE_RESISTANCE,
                    Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("instant_health")),
                    PotionEffectType.HEALTH_BOOST,
                    PotionEffectType.HERO_OF_THE_VILLAGE,
                    Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("strength")),
                    PotionEffectType.INVISIBILITY,
                    Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("jump_boost")),
                    PotionEffectType.LUCK,
                    PotionEffectType.NIGHT_VISION,
                    PotionEffectType.REGENERATION,
                    PotionEffectType.SATURATION,
                    PotionEffectType.SLOW_FALLING,
                    PotionEffectType.SPEED,
                    PotionEffectType.WATER_BREATHING));
        } catch (Throwable ignored) {}
    }


    @Override
    public void onChangeClaim(@NotNull Player player, Location lastLocation, @NotNull Location to, Claim claimFrom, Claim claimTo, @Nullable Flag flagFrom, @Nullable Flag flagTo) {
        // Check if we need to send a message
        WorldSettings settings = this.settingsManager.get(player.getWorld());
        if (!settings.pvpRequiresClaimFlag) return;
        if (!settings.pvpEnterClaimMessageEnabled) return;

        // Entering a PVP zone
        if (flagTo != null && flagFrom == null) {
            MessagingUtil.sendMessage(player, TextMode.Warn + settings.pvpEnterClaimMessage);
            return;
        }
        // Leaving a PVP zone
        if (flagTo == null && flagFrom != null) {
            MessagingUtil.sendMessage(player, TextMode.Success + settings.pvpExitClaimMessage);
        }
    }

    /***
     * Depending on the claim flag, possibly cancels GP from preventing PvP.
     * @param event GP found a PVP event within a claim and will prevent it
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreventPvP(PreventPvPEvent event) {
        Flag defenderFlag = this.getFlagInstanceAtLocation(event.getDefender().getLocation(), null);
        // If AllowPvp is enabled,
        // Don't let GP protect the player.
        if (defenderFlag != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPotionSplash(PotionSplashEvent event) {
        handlePotionEvent(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPotionSplash(LingeringPotionSplashEvent event) {
        handlePotionEvent(event);
    }

    private void handlePotionEvent(ProjectileHitEvent event) {
        ThrownPotion potion;
        if (event instanceof PotionSplashEvent) {
            potion = ((PotionSplashEvent) event).getPotion();
        } else if (event instanceof LingeringPotionSplashEvent) {
            potion = ((LingeringPotionSplashEvent) event).getEntity();
        } else {
            return;
        }
        // ignore potions not thrown by players
        ProjectileSource projectileSource = potion.getShooter();
        if (!(projectileSource instanceof Player)) return;
        Player thrower = (Player) projectileSource;

        // ignore potions without any negative effects
        Collection<PotionEffect> effects = potion.getEffects();
        boolean hasNegativeEffect = false;
        for (PotionEffect effect : effects) {
            if (!POSITIVE_EFFECTS.contains(effect.getType())) {
                hasNegativeEffect = true;
                break;
            }
        }
        if (!hasNegativeEffect) return;

        // if not in a no-pvp world, we don't care
        WorldSettings settings = this.settingsManager.get(potion.getWorld());
        if (!settings.pvpRequiresClaimFlag) return;

        // ignore potions not affecting players or pets
        if (event instanceof PotionSplashEvent) {
            boolean hasProtectableTarget = false;
            for (LivingEntity affected : ((PotionSplashEvent) event).getAffectedEntities()) {
                if (affected instanceof Player && affected != thrower) {
                    hasProtectableTarget = true;
                    break;
                } else if (affected instanceof Tameable) {
                    Tameable pet = (Tameable) affected;
                    if (pet.isTamed() && pet.getOwner() != null) {
                        hasProtectableTarget = true;
                        break;
                    }
                }
            }
            if (!hasProtectableTarget) return;
        }

        // if in a flagged-for-pvp area, allow
        Flag flag = this.getFlagInstanceAtLocation(thrower.getLocation(), thrower);
        if (flag != null) return;

        // otherwise disallow
        ((Cancellable) event).setCancelled(true);
        MessagingUtil.sendMessage(thrower, TextMode.Err + settings.pvpDeniedMessage);
    }

    // when an entity is set on fire
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        // handle it just like we would an entity damage by entity event, except don't send player messages to avoid double messages
        // in cases like attacking with a flame sword or flame arrow, which would ALSO trigger the direct damage event handler
        this.handleEntityDamageEvent(event, false, event.getCombuster(), event.getEntity(), DamageCause.FIRE_TICK);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        this.handleEntityDamageEvent(event, true, event.getDamager(), event.getEntity(), event.getCause());
    }

    private boolean hasPlayerRider(Entity entity) {
        List<Entity> passengers = entity.getPassengers();
        if (passengers.isEmpty()) return false;
        return passengers.get(0) instanceof Player;
    }

    public boolean isPlayerOrPet(Entity entity) {
        if (entity instanceof Player) return true;
        if (hasPlayerRider(entity)) return true;
        return false;
    }

    private void handleEntityDamageEvent(Cancellable event, boolean sendErrorMessagesToPlayers, Entity attacker, Entity defender, DamageCause cause) {
        //if not in a no-pvp world, we don't care
        WorldSettings settings = this.settingsManager.get(attacker.getWorld());
        if (!settings.pvpRequiresClaimFlag) return;

        Projectile projectile = null;
        if (attacker instanceof Projectile) {
            projectile = (Projectile) attacker;
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        // both attacker and defender need to be a player or a dog
        if (!isPlayerOrPet(attacker)) return;
        if (!isPlayerOrPet(defender)) return;

        // If both players are in an allowPvp area, let them get hurt
        Flag flag = this.getFlagInstanceAtLocation(attacker.getLocation(), null);
        Flag flag2 = this.getFlagInstanceAtLocation(defender.getLocation(), null);
        if (flag != null && flag2 != null) return;

        // Enderpearl are considered as FALL with event.getEntityType() = player
        // so we allow them to get hurt
        if (cause == DamageCause.FALL) return;

        // At this point, we know we will prevent the damage
        event.setCancelled(true);
        if (sendErrorMessagesToPlayers && attacker instanceof Player) {
            MessagingUtil.sendMessage(attacker, TextMode.Err + settings.pvpDeniedMessage);
        }

        // give the shooter back their projectile
        if (projectile == null) return;
        if (projectile.hasMetadata("item-stack")) {
            MetadataValue meta = projectile.getMetadata("item-stack").get(0);
            if (meta != null) {
                ItemStack item = ((ItemStack) meta.value());
                if (item != null && item.getType() != Material.AIR) {
                    item.setAmount(1);
                    ((Player) attacker).getInventory().addItem(item);
                }
            }
            // Remove metadata in case the projectile is to damage multiple entities
            // i.e. firework aoe
            projectile.removeMetadata("item-stack", GPFlags.getInstance());
        }
        if (!(projectile instanceof Trident)) {
            projectile.remove();
        }

    }
    
    @EventHandler
    private void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = ((Player) event.getEntity());
            ItemStack projectile;
            ItemStack bow = event.getBow();
            if (bow == null) return;
            ItemMeta meta = bow.getItemMeta();
            if (meta != null && meta.hasEnchant(infinityEnchantment)) return;
            if (bow.getType() == Material.CROSSBOW) {
                if (bow.getItemMeta() != null) {
                    List<ItemStack> projs = ((CrossbowMeta) bow.getItemMeta()).getChargedProjectiles();
                    if (projs.size() == 0) return;
                    projectile = projs.get(0);

                    // EntityShootBowEvent is still fired for each projectile
                    // Only add the metadata to the first projectile launched within a short time
                    if (projs.size() > 1) {
                        if (justFiredCrossbow.contains(player)) return;
                        justFiredCrossbow.add(player);
                        Bukkit.getScheduler().runTaskLater(GPFlags.getInstance(), () -> {
                            justFiredCrossbow.remove(player);
                        }, 5); // players have to fully charge their crossbow to fire more than one projectile.
                               // We can give this time to account for lag
                    }
                    
                    event.getProjectile().setMetadata("item-stack", new FixedMetadataValue(GPFlags.getInstance(), projectile.clone()));
                    return;
                }
            }
            if (isProjectile(player.getInventory().getItemInOffHand())) {
                projectile = player.getInventory().getItemInOffHand();
                event.getProjectile().setMetadata("item-stack", new FixedMetadataValue(GPFlags.getInstance(), projectile.clone()));
                return;
            }
            for (ItemStack item : player.getInventory()) {
                if (isProjectile(item)) {
                    event.getProjectile().setMetadata("item-stack", new FixedMetadataValue(GPFlags.getInstance(), item.clone()));
                    return;
                }
            }
        }
    }

    private boolean isProjectile(ItemStack item) {
        if (item == null) {
            return false;
        }
        switch (item.getType()) {
            case ARROW:
            case SPECTRAL_ARROW:
            case TIPPED_ARROW:
            case FIREWORK_ROCKET:
                return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return "AllowPvP";
    }

    @Override
    public MessageSpecifier getSetMessage(String parameters) {
        return new MessageSpecifier(Messages.AddEnablePvP);
    }

    @Override
    public MessageSpecifier getUnSetMessage() {
        return new MessageSpecifier(Messages.RemoveEnabledPvP);
    }

    @Override
    public List<FlagType> getFlagType() {
        return Arrays.asList(FlagType.CLAIM, FlagType.DEFAULT, FlagType.WORLD, FlagType.SERVER);
    }

}
