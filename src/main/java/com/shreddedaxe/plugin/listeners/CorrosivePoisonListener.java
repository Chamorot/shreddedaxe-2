package com.shreddedaxe.plugin.listeners;

import com.shreddedaxe.plugin.ShreddedAxePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CorrosivePoisonListener implements Listener {

    // Poison II for 2 seconds = 40 ticks. Amplifier 1 = Poison II.
    private static final int  POISON_DURATION_TICKS = 40;
    private static final int  POISON_AMPLIFIER      = 1;       // Poison II
    private static final long COOLDOWN_MS           = 20_000L; // 20 seconds

    private final ShreddedAxePlugin plugin;

    // Players whose axe is currently coated and ready to deliver poison on next hit
    private final Set<UUID> coated = new HashSet<>();

    // Tracks when the cooldown started (so we can show remaining time)
    private final Map<UUID, Long> lastTrigger = new HashMap<>();

    public CorrosivePoisonListener(ShreddedAxePlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Step 1: Press F — coat the axe with poison
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOffhandPress(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // Only intercept if the Shredded Axe is in the main hand
        if (!plugin.getShreddedAxeItem().isShreddedAxe(event.getMainHandItem())) return;

        // Keep the axe in the main hand — F is purely the ability key
        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        // Cooldown check
        if (lastTrigger.containsKey(uuid)) {
            long elapsed = now - lastTrigger.get(uuid);
            if (elapsed < COOLDOWN_MS) {
                long remainingSecs = Math.max(1, (COOLDOWN_MS - elapsed) / 1000L);
                player.sendActionBar(
                    Component.text("\u2622 Corrosive Poison \u2014 ")
                        .color(NamedTextColor.DARK_GREEN)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(
                            Component.text(remainingSecs + "s")
                                .color(NamedTextColor.YELLOW)
                                .decoration(TextDecoration.BOLD, false)
                                .decoration(TextDecoration.ITALIC, false)
                        )
                );
                return;
            }
        }

        // Already coated — don't reset or double-arm
        if (coated.contains(uuid)) {
            player.sendActionBar(
                Component.text("\u2622 Axe is already coated — hit an enemy!")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return;
        }

        // Arm the axe and start the cooldown
        coated.add(uuid);
        lastTrigger.put(uuid, now);

        player.sendActionBar(
            Component.text("\u2622 Axe coated with Corrosive Poison — hit an enemy!")
                .color(NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        );
    }

    // -------------------------------------------------------------------------
    // Step 2: Melee hit — deliver the poison and consume the coating
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        // Damager must be a player
        if (!(event.getDamager() instanceof Player player)) return;

        // Victim must be a living entity
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Axe must be in the main hand
        if (!plugin.getShreddedAxeItem().isShreddedAxe(player.getInventory().getItemInMainHand())) return;

        UUID uuid = player.getUniqueId();

        // Only fire if the axe is coated
        if (!coated.contains(uuid)) return;

        // Consume the coating — one hit, one proc
        coated.remove(uuid);

        // Apply Poison II for 2 seconds
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.POISON,
            POISON_DURATION_TICKS,
            POISON_AMPLIFIER,
            false, // ambient (beacon-style particles) — off
            true,  // show particles
            true   // show icon
        ));

        player.sendActionBar(
            Component.text("\u2622 Corrosive Poison delivered!")
                .color(NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        );
    }

    // -------------------------------------------------------------------------
    // Clear coated state on death so it doesn't persist into respawn
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        clearCooldown(event.getEntity().getUniqueId());
    }

    public void clearCooldown(UUID uuid) {
        lastTrigger.remove(uuid);
        coated.remove(uuid);
    }

    public void clearAll() {
        lastTrigger.clear();
        coated.clear();
    }
}
