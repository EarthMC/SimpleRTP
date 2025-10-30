package dev.warriorrr.simplertp;

import dev.warriorrr.simplertp.model.Region;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportHandler implements Listener {
    private final SimpleRTP plugin;
    private final Map<UUID, ScheduledTask> teleports = new HashMap<>();

    public TeleportHandler(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    public void addTeleport(Player player, Region region, Location location) {
        final Runnable teleport = () -> player.teleportAsync(location).thenRun(() -> {
            player.sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " in " + region.name() + ".");
            teleports.remove(player.getUniqueId());
        });

        if (player.hasPermission("simplertp.teleport.nowarmup")) {
            teleport.run();
            return;
        }

        ScheduledTask task = plugin.getServer().getRegionScheduler().runDelayed(plugin, location, t -> teleport.run(), 3 * 20);

        teleports.put(player.getUniqueId(), task);
    }

    public boolean hasTeleport(Player player) {
        return teleports.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ScheduledTask task = teleports.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
            player.sendMessage(Component.text("Teleportation cancelled due to taking damage.", NamedTextColor.DARK_RED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!event.hasChangedBlock()) return;
        ScheduledTask task = teleports.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
            player.sendMessage(Component.text("Teleportation cancelled due to movement.", NamedTextColor.DARK_RED));
        }
    }
}
