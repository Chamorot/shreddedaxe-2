package com.shreddedaxe.plugin.commands;

import com.shreddedaxe.plugin.ShreddedAxePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ShreddedAxeCommand implements CommandExecutor, TabCompleter {

    private final ShreddedAxePlugin plugin;

    public ShreddedAxeCommand(ShreddedAxePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length > 0) {
            return switch (args[0].toLowerCase()) {
                case "owner"   -> handleOwner(sender);
                case "restore" -> handleRestore(sender);
                default -> {
                    sendUsage(sender);
                    yield true;
                }
            };
        }

        // /shreddedaxe — claim the weapon
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (plugin.getOwnerStorage().isClaimed()) {
            String ownerName = plugin.getOwnerStorage().getOwnerName();
            player.sendMessage(
                Component.text("The Shredded Axe has already been claimed by ")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(
                        Component.text(ownerName)
                            .color(NamedTextColor.DARK_RED)
                            .decoration(TextDecoration.BOLD, true)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    .append(
                        Component.text(". It can never be claimed again.")
                            .color(NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)
                    )
            );
            return true;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(
                Component.text("Your inventory is full! Clear a slot and use /shreddedaxe again to claim it.")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return true;
        }

        plugin.getOwnerStorage().setClaimed(player.getUniqueId(), player.getName());

        var item = plugin.getShreddedAxeItem().create();
        player.getInventory().addItem(item);

        plugin.getServer().broadcast(
            Component.text("\u2622 ")
                .color(NamedTextColor.DARK_GREEN)
                .append(
                    Component.text(player.getName())
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true)
                )
                .append(Component.text(" has claimed the ").color(NamedTextColor.GRAY))
                .append(
                    Component.text("Shredded Axe")
                        .color(NamedTextColor.DARK_GREEN)
                        .decoration(TextDecoration.BOLD, true)
                )
                .append(Component.text(". The venom runs deep.").color(NamedTextColor.GRAY))
        );

        player.sendMessage(
            Component.text("The Shredded Axe is now yours \u2014 and yours alone.")
                .color(NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        );
        player.sendMessage(
            Component.text("Hold it in your offhand and strike to trigger Corrosive Poison.")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        );

        return true;
    }

    // -------------------------------------------------------------------------
    // /shreddedaxe owner
    // -------------------------------------------------------------------------

    private boolean handleOwner(CommandSender sender) {
        if (!plugin.getOwnerStorage().isClaimed()) {
            sender.sendMessage(
                Component.text("The Shredded Axe has not yet been claimed by anyone.")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return true;
        }

        String ownerName = plugin.getOwnerStorage().getOwnerName();
        sender.sendMessage(
            Component.text("\u2622 Shredded Axe Owner: ")
                .color(NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
                .append(
                    Component.text(ownerName)
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false)
                )
        );
        return true;
    }

    // -------------------------------------------------------------------------
    // /shreddedaxe restore — admin only, requires shreddedaxe.admin permission
    // -------------------------------------------------------------------------

    private boolean handleRestore(CommandSender sender) {
        if (!sender.hasPermission("shreddedaxe.admin")) {
            sender.sendMessage(
                Component.text("You do not have permission to use this command.")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return true;
        }

        var storage = plugin.getOwnerStorage();
        if (!storage.isClaimed()) {
            sender.sendMessage(
                Component.text("The Shredded Axe has not been claimed yet. Nothing to restore.")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return true;
        }

        Player owner = plugin.getServer().getPlayer(storage.getOwnerUuid());
        if (owner == null) {
            sender.sendMessage(
                Component.text("The owner (" + storage.getOwnerName() + ") is not online. They must be online to receive the weapon.")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return true;
        }

        if (owner.getInventory().firstEmpty() == -1) {
            sender.sendMessage(
                Component.text(owner.getName() + "'s inventory is full. Ask them to clear a slot before restoring.")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            );
            owner.sendMessage(
                Component.text("An admin tried to restore the Shredded Axe but your inventory is full. Clear a slot and ask them to try again.")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return true;
        }

        var item = plugin.getShreddedAxeItem().create();
        owner.getInventory().addItem(item);

        sender.sendMessage(
            Component.text("Shredded Axe restored and given to " + owner.getName() + ".")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
        );
        owner.sendMessage(
            Component.text("The Shredded Axe has been restored to you by an administrator.")
                .color(NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.ITALIC, false)
        );

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(
            Component.text("Usage: /shreddedaxe | /shreddedaxe owner | /shreddedaxe restore")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
        );
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("owner", "restore").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}
