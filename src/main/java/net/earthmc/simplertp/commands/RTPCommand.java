package net.earthmc.simplertp.commands;

import net.earthmc.simplertp.SimpleRTP;
import net.earthmc.simplertp.TeleportHandler;
import net.earthmc.simplertp.model.GeneratedLocation;
import net.earthmc.simplertp.model.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RTPCommand implements TabExecutor {
    private final SimpleRTP plugin;
    private final TeleportHandler teleportHandler;

    public RTPCommand(SimpleRTP plugin, TeleportHandler teleportHandler) {
        this.plugin = plugin;
        this.teleportHandler = teleportHandler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("simplertp.command.rtp")) {
            sender.sendMessage(Component.text("You do not have permissions to use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command cannot be used by console! See also: /rtp reload, /rtp player {name}", NamedTextColor.RED));
                return true;
            }

            final GeneratedLocation location = plugin.generator().getAndRemove();

            if (teleportHandler.hasTeleport(player)) {
                player.sendMessage(Component.text("You already have a pending teleport!", NamedTextColor.RED));
                return true;
            }

            if (isOnTeleportCooldown(player)) {
                return true;
            }

            player.sendRichMessage("<gradient:blue:aqua>Waiting to teleport...");
            teleportHandler.addTeleport(player, location.region(), location.location());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("simplertp.command.rtp.reload")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                    return true;
                }
                if (args.length > 1) {
                    String choice = args[1];
                    switch (choice.toLowerCase()) {
                        case "config" -> {
                            plugin.reload();
                            sender.sendMessage(Component.text("Reloaded config.", NamedTextColor.GREEN));
                            return true;
                        }
                        case "locations" -> {
                            plugin.generator().reload();
                            sender.sendMessage(Component.text("Cleared locations. Will be repopulated with new settings shortly.", NamedTextColor.GREEN));
                            return true;
                        }
                    }
                }
                plugin.reload();
                plugin.generator().reload();
                sender.sendMessage(Component.text("Reloaded config and cleared locations.", NamedTextColor.GREEN));
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

                Player player = Bukkit.getPlayerExact(args[0]);
                if (player == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }

                if (teleportHandler.hasTeleport(player)) {
                    sender.sendMessage(Component.text("Player already has a pending teleport!", NamedTextColor.RED));
                    return true;
                }

                player.sendRichMessage("<gradient:blue:aqua>Waiting to teleport...");

                final GeneratedLocation generatedLoc = plugin.generator().getAndRemove();
                final Location location = generatedLoc.location();

                teleportHandler.addTeleport(player, generatedLoc.region(), location);
                sender.sendMessage(Component.text("Randomly teleported " + player.getName() + " to " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ".", NamedTextColor.GREEN));
            }
            case "debug" -> {
                if (!sender.hasPermission("simplertp.command.rtp.debug")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                    return true;
                }

                sender.sendRichMessage("<gradient:blue:aqua>SimpleRTP Info");
                sender.sendRichMessage("<blue>- Regions: <aqua>%d".formatted(plugin.config().getRegions().size()));
                sender.sendRichMessage("<blue>- Locations: <aqua>%d".formatted(plugin.generator().getLocationsSize()));
                sender.sendRichMessage("<blue>- Tasks: <aqua>%d".formatted(plugin.generator().getTasksSize()));
            }
            default -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("This command cannot be used by console! See also: /rtp reload, /rtp player {name}", NamedTextColor.RED));
                    return true;
                }

                if (!sender.hasPermission("simplertp.command.rtp.region")) {
                    sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                    return true;
                }

                if (teleportHandler.hasTeleport(player)) {
                    player.sendMessage(Component.text("You already have a pending teleport!", NamedTextColor.RED));
                    return true;
                }

                if (isOnTeleportCooldown(player)) {
                    return true;
                }

                String name = String.join(" ", args);
                final Region region = plugin.config().getRegionByName(name);
                if (region == null) {
                    sender.sendMessage(Component.text("Could not find region with that name.", NamedTextColor.RED));
                    return true;
                }

                GeneratedLocation regionLoc = plugin.generator().getSpawnForRegion(region);
                if (regionLoc == null) {
                    sender.sendMessage(Component.text("Could not find a suitable spawn", NamedTextColor.RED));
                    return true;
                }

                player.sendRichMessage("<gradient:blue:aqua>Waiting to teleport...");
                teleportHandler.addTeleport(player, regionLoc.region(), regionLoc.location());
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        List<String> choices = new ArrayList<>();
        if (commandSender.hasPermission("simplertp.command.rtp.reload")) choices.add("reload");
        if (commandSender.hasPermission("simplertp.command.rtp.others")) choices.add("others");
        if (commandSender.hasPermission("simplertp.command.rtp.region")) {
            choices.addAll(plugin.config().getRegions().stream().map(Region::name).toList());
        }

        if (commandSender.hasPermission("simplertp.command.rtp.debug")) choices.add("debug");

        if (choices.isEmpty()) return List.of();
        if (strings.length == 1) {
            return choices.stream().filter(str -> str.startsWith(strings[0])).toList();
        }

        if (strings.length == 2) {
            if (!choices.contains(strings[0].toLowerCase())) return List.of();
            switch (strings[0].toLowerCase()) {
                case "others" -> {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(str -> str.startsWith(strings[1])).toList();
                }
                case "reload" -> {
                    return Stream.of("config", "locations", "all").filter(str -> str.startsWith(strings[1])).toList();
                }
            }
        }
        return List.of();
    }

    private boolean isOnTeleportCooldown(final Player player) {
        final Instant cooldownEndTime = plugin.teleportHandler().getCooldownTime(player.getUniqueId());
        if (cooldownEndTime == null || Instant.now().isAfter(cooldownEndTime)) {
            return false;
        }

        final Duration between = Duration.between(Instant.now(), cooldownEndTime);
        final int minutes = between.toMinutesPart();
        final int seconds = between.toSecondsPart();

        final StringBuilder timeBuilder = new StringBuilder();
        if (minutes > 0) {
            timeBuilder.append(minutes).append(" minute").append(minutes == 1 ? "" : "s").append(" ");
        }

        if (seconds > 0) {
            timeBuilder.append(seconds).append(" second").append(seconds == 1 ? "" : "s").append(" ");
        }

        player.sendMessage(Component.text("You must wait another " + timeBuilder.toString().trim() + " before teleporting again.", NamedTextColor.RED));
        return true;
    }
}
