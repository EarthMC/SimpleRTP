package net.earthmc.simplertp.commands;

import net.earthmc.simplertp.SimpleRTP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
            if (!(sender instanceof Player player))
                sender.sendMessage(Component.text("This command cannot be used by console! See also: /rtp reload, /rtp [name]", NamedTextColor.RED));
            else {
                if (!player.hasPermission("simplertp.command.rtp"))
                    player.sendMessage(Component.text("You do not have enough permissions to use this command.", NamedTextColor.RED));
                else {
                    Location location = plugin.generator().getAndRemove();
                    player.teleportAsync(location);
                    player.sendMessage(MiniMessage.miniMessage().parse("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."));
                }
            }
        } else {
            if ("reload".equalsIgnoreCase(args[0])) {
                if (!sender.hasPermission("simplertp.command.rtp.reload"))
                    sender.sendMessage(Component.text("You do not have enough permission to use this command.", NamedTextColor.RED));
                else {
                    plugin.reload();
                    sender.sendMessage(Component.text("Reloaded config.", NamedTextColor.GREEN));
                }
            } else {
                if (!sender.hasPermission("simplertp.command.rtp.others"))
                    sender.sendMessage(Component.text("You do not have enough permission to use this command.", NamedTextColor.RED));
                else if (Bukkit.getPlayerExact(args[0]) != null) {
                    Player player = Bukkit.getPlayerExact(args[0]);
                    if (player == null)
                        return true;

                    Location location = plugin.generator().getAndRemove();
                    player.teleportAsync(location);
                    player.sendMessage(MiniMessage.miniMessage().parse("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."));
                } else {
                    sender.sendMessage(Component.text("Invalid subcommand: '" + args[0] + "'.", NamedTextColor.RED));
                }
            }
        }
        return true;
    }
}
