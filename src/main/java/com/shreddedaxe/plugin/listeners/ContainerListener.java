package com.shreddedaxe.plugin.listeners;

import com.shreddedaxe.plugin.ShreddedAxePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ContainerListener implements Listener {

    private final ShreddedAxePlugin plugin;

    public ContainerListener(ShreddedAxePlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Block placing into any external inventory
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        boolean topIsExternal = isExternalInventory(top.getType());

        // Shift-click from player inventory into an external container
        if (event.isShiftClick() && isShreddedAxe(event.getCurrentItem())) {
            if (topIsExternal) {
                event.setCancelled(true);
                notifyPlayer(player);
                return;
            }
        }

        // Direct click into an external slot
        Inventory clicked = event.getClickedInventory();
        if (clicked != null && isExternalInventory(clicked.getType())) {
            if (isShreddedAxe(event.getCursor()) || isShreddedAxe(event.getCurrentItem())) {
                event.setCancelled(true);
                notifyPlayer(player);
                return;
            }
        }

        // Hotbar number-key swap while an external inventory is open
        if (event.getClick() == ClickType.NUMBER_KEY && topIsExternal) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                if (isShreddedAxe(hotbarItem)) {
                    event.setCancelled(true);
                    notifyPlayer(player);
                    return;
                }
            }
        }

        // F-key offhand swap while hovering over a slot in an external inventory.
        // Without this, pressing F over a chest slot silently moves the axe in.
        if (event.getClick() == ClickType.SWAP_OFFHAND && topIsExternal) {
            if (isShreddedAxe(player.getInventory().getItemInOffHand())
                    || isShreddedAxe(event.getCurrentItem())) {
                event.setCancelled(true);
                notifyPlayer(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isShreddedAxe(event.getOldCursor())) return;

        Inventory top = event.getView().getTopInventory();
        if (!isExternalInventory(top.getType())) return;

        int topSize = top.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                notifyPlayer(player);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Block item frames (regular + glow) and armor stands
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        var entity = event.getRightClicked();

        boolean isHolder = entity instanceof ItemFrame || entity instanceof ArmorStand;
        if (!isHolder) return;

        ItemStack hand = event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();

        if (isShreddedAxe(hand)) {
            event.setCancelled(true);
            notifyPlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        var entity = event.getRightClicked();

        boolean isHolder = entity instanceof ItemFrame || entity instanceof ArmorStand;
        if (!isHolder) return;

        ItemStack hand = event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();

        if (isShreddedAxe(hand)) {
            event.setCancelled(true);
            notifyPlayer(player);
        }
    }

    // -------------------------------------------------------------------------
    // Block hoppers / minecart hoppers
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (isShreddedAxe(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Block all non-owner entities (mobs) from picking it up
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!isShreddedAxe(event.getItem().getItemStack())) return;

        if (!(event.getEntity() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        var storage = plugin.getOwnerStorage();
        if (!storage.isClaimed()) return;

        if (!player.getUniqueId().equals(storage.getOwnerUuid())) {
            event.setCancelled(true);
            player.sendActionBar(
                Component.text("This weapon does not belong to you.")
                    .color(NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false)
            );
        }
    }

    // -------------------------------------------------------------------------
    // Protect the item entity from environmental destruction (lava, fire, etc.)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item itemEntity)) return;
        if (!isShreddedAxe(itemEntity.getItemStack())) return;
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Soulbound on death — keep axe on the player through death/respawn.
    // getItemsToKeep() handles normal deaths. We also remove from getDrops()
    // defensively, and flag the item on the entity directly to survive /kill
    // (which skips getDrops() on some Paper builds).
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        var drops    = event.getDrops();
        var iterator = drops.iterator();

        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isShreddedAxe(item)) {
                iterator.remove();
                event.getItemsToKeep().add(item);
            }
        }

        // Also scan the player's live inventory — /kill bypasses getDrops()
        // entirely on Paper 1.21, so we remove it here and re-add it after
        // the event via a 1-tick delayed task.
        Player player = event.getEntity();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isShreddedAxe(item)) {
                final ItemStack kept = item.clone();
                inv.setItem(i, null);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.getInventory().addItem(kept);
                    }
                }, 1L);
                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Block the owner from accidentally dropping the axe
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isShreddedAxe(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        event.getPlayer().sendActionBar(
            Component.text("The Shredded Axe cannot be discarded.")
                .color(NamedTextColor.DARK_RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        );
    }

    // -------------------------------------------------------------------------
    // Block anvil interactions — prevents renaming or merging enchantments
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        var inventory = event.getInventory();
        if (isShreddedAxe(inventory.getItem(0))
                || isShreddedAxe(inventory.getItem(1))) {
            event.setResult(null);
            inventory.setItem(2, null);
        }
    }

    // -------------------------------------------------------------------------
    // Block grindstone interactions — prevents stripping enchantments for XP
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        var inventory = event.getInventory();
        if (isShreddedAxe(inventory.getItem(0))
                || isShreddedAxe(inventory.getItem(1))) {
            event.setResult(null);
            inventory.setItem(2, null);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isExternalInventory(InventoryType type) {
        return switch (type) {
            case PLAYER, CRAFTING -> false;
            default -> true;
        };
    }

    private boolean isShreddedAxe(ItemStack item) {
        return plugin.getShreddedAxeItem().isShreddedAxe(item);
    }

    private void notifyPlayer(Player player) {
        player.sendActionBar(
            Component.text("The Shredded Axe cannot be contained.")
                .color(NamedTextColor.DARK_RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        );
    }
}
