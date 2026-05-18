package com.shreddedaxe.plugin.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import com.shreddedaxe.plugin.ShreddedAxePlugin;

import java.util.List;

public class ShreddedAxeItem {

    public static final String SHREDDED_AXE_KEY = "shredded_axe";

    private final NamespacedKey itemKey;

    public ShreddedAxeItem() {
        this.itemKey = new NamespacedKey(ShreddedAxePlugin.getInstance(), SHREDDED_AXE_KEY);
    }

    public NamespacedKey getItemKey() {
        return itemKey;
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.DIAMOND_AXE);

        item.editMeta(ItemMeta.class, meta -> {
            // Name
            meta.displayName(
                Component.text("Shredded Axe")
                    .color(NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false)
            );

            // Lore
            meta.lore(List.of(
                Component.empty(),
                Component.text("\u2622 Corrosive Poison")
                    .color(NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("Press [F] to activate")
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("Erodes the axe with potent Poison,")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("harming the opponent for 2 seconds.")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Cooldown: ")
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(
                        Component.text("20s")
                            .color(NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false)
                    ),
                Component.empty(),
                Component.text("\"...the blade weeps venom with every swing...\"")
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, true)
            ));

            // Enchantments
            meta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("efficiency")), 6, true);
            meta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("sharpness")), 5, true);

            // Unbreakable
            meta.setUnbreakable(true);

            // Hide enchants, unbreakable tag, and attributes from tooltip
            meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ATTRIBUTES
            );

            // PDC tag to identify this item
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        });

        return item;
    }

    public boolean isShreddedAxe(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_AXE) return false;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }
}
