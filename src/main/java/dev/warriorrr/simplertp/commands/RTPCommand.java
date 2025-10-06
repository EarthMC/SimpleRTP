package dev.warriorrr.simplertp.commands;

import dev.warriorrr.simplertp.RTPConfig;
import dev.warriorrr.simplertp.SimpleRTP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RTPCommand implements TabExecutor {
    private final SimpleRTP plugin;

    public RTPCommand(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("simplertp.command.rtp")) {
            sender.sendMessage(Component.text("You do not have permissions to use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("tp")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command cannot be used by console! See also: /rtp reload, /rtp player {name}", NamedTextColor.RED));
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
        final Location location = plugin.generator().getAndRemove();
        if (location == null) {
            sender.sendMessage(Component.text("Could not find a suitable location.").color(NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("simplertp.command.rtp.reload")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(Component.text("Reloaded config.", NamedTextColor.GREEN));
                return true;
            }
            case "player" -> {
                if (!sender.hasPermission("simplertp.command.rtp.others")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /rtp player {name}", NamedTextColor.RED));
                    return true;
                }
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                player.getScheduler().run(plugin, task -> {
                    player.teleportAsync(location).thenRun(() -> player.sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."));
                    sender.sendMessage(Component.text("Randomly teleported " + player.getName() + " to " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ".", NamedTextColor.GREEN));
                }, () -> {});
            }
            case "region" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("This command cannot be used by console! See also: /rtp reload, /rtp player {name}", NamedTextColor.RED));
                    return true;
                }
                if (!sender.hasPermission("simplertp.command.rtp.region")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /rtp region {name}", NamedTextColor.RED));
                    return true;
                }
                String name = args[1];
                RTPConfig.Region region = plugin.config().getRegions().stream().filter(r -> r.name().equalsIgnoreCase(name)).toList().getFirst();
                if (region == null) {
                    sender.sendMessage(Component.text("Could not find region with that name.", NamedTextColor.RED));
                    return true;
                }
                Location regionLoc = plugin.generator().getSpawnForRegion(region);
                if (regionLoc == null) {
                    sender.sendMessage(Component.text("Could not find a suitable spawn", NamedTextColor.RED));
                    plugin.getLogger().warning("Could not find suitable spawn for region " + region.name());
                    return true;
                }
                player.teleportAsync(regionLoc).thenRun(() -> player.sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."));
            }
        }
        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        List<String> choices = new ArrayList<>();
        if (commandSender.hasPermission("simplertp.command.rtp.reload")) choices.add("reload");
        if (commandSender.hasPermission("simplertp.command.rtp.others")) choices.add("others");
        if (commandSender.hasPermission("simplertp.command.rtp.region")) choices.add("region");
        if (strings.length == 1) {
            return choices.stream().filter(str -> str.startsWith(strings[0])).toList();
        }
        return List.of();
    }
}
