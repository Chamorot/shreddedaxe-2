package com.shreddedaxe.plugin;

import com.shreddedaxe.plugin.commands.ShreddedAxeCommand;
import com.shreddedaxe.plugin.items.ShreddedAxeItem;
import com.shreddedaxe.plugin.listeners.ContainerListener;
import com.shreddedaxe.plugin.listeners.CorrosivePoisonListener;
import com.shreddedaxe.plugin.storage.OwnerStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ShreddedAxePlugin extends JavaPlugin implements Listener {

    private static ShreddedAxePlugin instance;
    private ShreddedAxeItem shreddedAxeItem;
    private OwnerStorage ownerStorage;
    private CorrosivePoisonListener corrosivePoisonListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        shreddedAxeItem        = new ShreddedAxeItem();
        ownerStorage           = new OwnerStorage(this);
        corrosivePoisonListener = new CorrosivePoisonListener(this);

        // Listeners
        getServer().getPluginManager().registerEvents(corrosivePoisonListener, this);
        getServer().getPluginManager().registerEvents(new ContainerListener(this), this);
        getServer().getPluginManager().registerEvents(this, this); // PlayerQuitEvent cleanup

        // Command
        var cmd = getCommand("shreddedaxe");
        if (cmd != null) {
            var handler = new ShreddedAxeCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        getLogger().info("Shredded Axe loaded. The venom is ready.");
    }

    @Override
    public void onDisable() {
        if (corrosivePoisonListener != null) {
            corrosivePoisonListener.clearAll();
        }
        getLogger().info("Shredded Axe unloaded.");
    }

    // Clean up cooldown map when a player disconnects
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (corrosivePoisonListener != null) {
            corrosivePoisonListener.clearCooldown(event.getPlayer().getUniqueId());
        }
    }

    public static ShreddedAxePlugin getInstance() {
        return instance;
    }

    public ShreddedAxeItem getShreddedAxeItem() {
        return shreddedAxeItem;
    }

    public OwnerStorage getOwnerStorage() {
        return ownerStorage;
    }
}
