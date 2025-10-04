package dev.warriorrr.simplertp.commands;

import dev.warriorrr.simplertp.SimpleRTP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RTPCommand implements CommandExecutor {
    private final SimpleRTP plugin;

    public RTPCommand(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command cannot be used by console! See also: /rtp reload, /rtp [name]", NamedTextColor.RED));
                return true;
            }
            if (!player.hasPermission("simplertp.command.rtp")) {
                player.sendMessage(Component.text("You do not have permissions to use this command.", NamedTextColor.RED));
                return true;
            }
            final Location location = plugin.generator().getAndRemove();
            if (location == null) {
                sender.sendMessage(Component.text("Could not find a suitable location.").color(NamedTextColor.RED));
                return true;
            }
            player.teleportAsync(location).thenRun(() -> player.sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."));
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("simplertp.command.rtp.reload"))
                sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            else {
                plugin.reload();
                sender.sendMessage(Component.text("Reloaded config.", NamedTextColor.GREEN));
            }
            return true;
        }

        if (!sender.hasPermission("simplertp.command.rtp.others")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(Component.text("Invalid subcommand: '" + args[0] + "'.", NamedTextColor.RED));
            return true;
        }
        final Location location = plugin.generator().getAndRemove();
        if (location == null) {
            sender.sendMessage(Component.text("Could not find a suitable location.").color(NamedTextColor.RED));
            return true;
        }
        player.getScheduler().run(plugin, task -> {
            player.teleportAsync(location).thenRun(() -> player.sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."));
            sender.sendMessage(Component.text("Randomly teleported " + player.getName() + " to " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ".", NamedTextColor.GREEN));
        }, () -> {});

        return true;
    }
}
