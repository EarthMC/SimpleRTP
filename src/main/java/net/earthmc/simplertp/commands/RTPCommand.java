package net.earthmc.simplertp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.earthmc.simplertp.SimpleRTP;
import net.earthmc.simplertp.TeleportHandler;
import net.earthmc.simplertp.model.GeneratedLocation;
import net.earthmc.simplertp.model.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class RTPCommand {

    public static LiteralCommandNode<CommandSourceStack> createCommand(String rootName, SimpleRTP plugin, TeleportHandler teleportHandler) {
        return Commands.literal(rootName)
                .requires(ctx -> ctx.getSender().hasPermission("simplertp.command.rtp"))
                .executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player player)) {
                        ctx.getSource().getSender().sendMessage(Component.text("This command cannot be used by console! See also: /rtp reload, /rtp player {name}", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    final GeneratedLocation location = plugin.generator().getAndRemove();

                    if (teleportHandler.hasTeleport(player)) {
                        player.sendMessage(Component.text("You already have a pending teleport!", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (isOnTeleportCooldown(plugin, player)) {
                        return Command.SINGLE_SUCCESS;
                    }

                    player.sendRichMessage("<gradient:blue:aqua>Waiting to teleport...");
                    teleportHandler.addTeleport(player, location.region(), location.location());
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .requires(ctx -> ctx.getSender().hasPermission("simplertp.command.rtp.reload"))
                        .executes(ctx -> {
                            plugin.reload();
                            plugin.generator().reload();
                            ctx.getSource().getSender().sendMessage(Component.text("Reloaded config and cleared locations.", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("config")
                                .executes(ctx -> {
                                    plugin.reload();
                                    ctx.getSource().getSender().sendMessage(Component.text("Reloaded config.", NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("locations")
                                .executes(ctx -> {
                                    plugin.generator().reload();
                                    ctx.getSource().getSender().sendMessage(Component.text("Cleared locations. Will be repopulated with new settings shortly.", NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("player")
                        .requires(ctx -> ctx.getSender().hasPermission("simplertp.command.rtp.others"))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> {
                                    teleportPlayer(plugin, ctx, null);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("region", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> filterByStart(ctx, builder, plugin.config().getRegionNames()))
                                        .executes(ctx -> {
                                            teleportPlayer(plugin, ctx, ctx.getArgument("region", String.class));
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(Commands.literal("debug")
                        .requires(ctx -> ctx.getSender().hasPermission("simplertp.command.rtp.debug"))
                        .executes(ctx -> {
                            final CommandSender sender = ctx.getSource().getSender();
                            sender.sendRichMessage("<gradient:blue:aqua>SimpleRTP Info");
                            sender.sendRichMessage("<blue>- Regions: <aqua>%d".formatted(plugin.config().getRegions().size()));
                            sender.sendRichMessage("<blue>- Locations: <aqua>%d".formatted(plugin.generator().getLocationsSize()));
                            sender.sendRichMessage("<blue>- Tasks: <aqua>%d".formatted(plugin.generator().getTasksSize()));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("to")
                        .requires(ctx -> ctx.getSender() instanceof Player player && player.hasPermission("simplertp.command.rtp.region"))
                        .then(Commands.argument("region", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> filterByStart(ctx, builder, plugin.config().getRegionNames()))
                                .executes(ctx -> {
                                    final String regionName = ctx.getArgument("region", String.class);
                                    final Player player = (Player) ctx.getSource().getSender();

                                    if (teleportHandler.hasTeleport(player)) {
                                        player.sendMessage(Component.text("You already have a pending teleport!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    if (isOnTeleportCooldown(plugin, player)) {
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    final Region region = plugin.config().getRegionByName(regionName);
                                    if (region == null) {
                                        player.sendMessage(Component.text("No region with name '" + regionName + "' exists.", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    GeneratedLocation regionLoc = plugin.generator().getSpawnForRegion(region);
                                    if (regionLoc == null) {
                                        player.sendMessage(Component.text("Could not find a suitable spawn", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    player.sendRichMessage("<gradient:blue:aqua>Waiting to teleport...");
                                    teleportHandler.addTeleport(player, regionLoc.region(), regionLoc.location());
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    private static boolean isOnTeleportCooldown(final SimpleRTP plugin, final Player player) {
        final Instant cooldownEndTime = plugin.teleportHandler().getCooldownTime(player.getUniqueId());
        if (cooldownEndTime == null || Instant.now().isAfter(cooldownEndTime) || player.hasPermission("simplertp.teleport.nocooldown")) {
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

    private static void teleportPlayer(final SimpleRTP plugin, final CommandContext<CommandSourceStack> ctx, final @Nullable String regionName) throws CommandSyntaxException {
        final Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();

        if (plugin.teleportHandler().hasTeleport(player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Player already has a pending teleport!", NamedTextColor.RED));
            return;
        }

        player.sendRichMessage("<gradient:blue:aqua>Waiting to teleport...");

        final GeneratedLocation generatedLoc;
        if (regionName != null) {
            final Region region = plugin.config().getRegionByName(regionName);
            if (region == null) {
                ctx.getSource().getSender().sendMessage(Component.text("No region with name '" + regionName + "' exists.", NamedTextColor.RED));
                return;
            }

            generatedLoc = plugin.generator().getSpawnForRegion(region);
            if (generatedLoc == null) {
                ctx.getSource().getSender().sendMessage(Component.text("Could not find a suitable spawn location in region " + region.name() + ".", NamedTextColor.RED));
                return;
            }
        } else {
            generatedLoc = plugin.generator().getAndRemove();
        }

        final Location location = generatedLoc.location();

        plugin.teleportHandler().addTeleport(player, generatedLoc.region(), location);
        ctx.getSource().getSender().sendMessage(Component.text("Randomly teleported " + player.getName() + " to " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ".", NamedTextColor.GREEN));
    }

    public static CompletableFuture<Suggestions> filterByStart(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, Iterable<String> suggestions) {
        final String argument = builder.getRemaining();

        for (String suggestion : suggestions) {
            if (suggestion.regionMatches(true, 0, argument, 0, argument.length())) {
                builder.suggest(suggestion);
            }
        }

        return builder.buildFuture();
    }
}
