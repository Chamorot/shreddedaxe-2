package com.shreddedaxe.plugin.storage;

import com.shreddedaxe.plugin.ShreddedAxePlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

/**
 * Persists the one-time claim of the Shredded Axe to config.yml.
 * Once claimed, the owner UUID is locked forever.
 */
public class OwnerStorage {

    private static final String KEY_CLAIMED    = "claimed";
    private static final String KEY_OWNER_UUID = "owner-uuid";
    // KEY_OWNER_NAME is kept in config only as a human-readable fallback;
    // at runtime we always resolve the name from the UUID via OfflinePlayer
    // so username changes are handled automatically.
    private static final String KEY_OWNER_NAME = "owner-name";

    private final ShreddedAxePlugin plugin;

    public OwnerStorage(ShreddedAxePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isClaimed() {
        return plugin.getConfig().getBoolean(KEY_CLAIMED, false);
    }

    public void setClaimed(UUID ownerUuid, String ownerName) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set(KEY_CLAIMED, true);
        cfg.set(KEY_OWNER_UUID, ownerUuid.toString());
        cfg.set(KEY_OWNER_NAME, ownerName); // stored as fallback only
        plugin.saveConfig();
    }

    public UUID getOwnerUuid() {
        String raw = plugin.getConfig().getString(KEY_OWNER_UUID);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the owner's current username by resolving the UUID through
     * Bukkit's OfflinePlayer cache. This automatically reflects Mojang
     * username changes without any manual config edits.
     * Falls back to the saved config name if the UUID can't be resolved.
     */
    public String getOwnerName() {
        UUID uuid = getOwnerUuid();
        if (uuid != null) {
            String resolved = Bukkit.getOfflinePlayer(uuid).getName();
            if (resolved != null && !resolved.isBlank()) {
                return resolved;
            }
        }
        return plugin.getConfig().getString(KEY_OWNER_NAME, "Unknown");
    }
}
