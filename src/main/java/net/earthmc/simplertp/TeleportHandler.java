package net.earthmc.simplertp;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.earthmc.simplertp.event.RandomTeleportEvent;
import net.earthmc.simplertp.model.Region;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportHandler implements Listener {

    private final SimpleRTP plugin;
    private final Map<UUID, ScheduledTask> teleports = new HashMap<>();
    private final Duration cooldownTime;
    private final Cache<UUID, Instant> teleportCooldowns;

    public TeleportHandler(SimpleRTP plugin) {
        this.plugin = plugin;

        this.cooldownTime = Duration.ofMinutes(plugin.config().getTeleportCooldownMinutes());
        this.teleportCooldowns = CacheBuilder.newBuilder().expireAfterWrite(this.cooldownTime).build();
    }

    public void addTeleport(Player player, Region region, Location location) {
        final Runnable teleport = () -> player.teleportAsync(location).thenRun(() -> {
            player.sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " in " + region.name() + ".");

            if (!player.hasPermission("simplertp.teleport.nocooldown")) {
                teleportCooldowns.asMap().put(player.getUniqueId(), Instant.now().plus(cooldownTime));
            }

            teleports.remove(player.getUniqueId());
            new RandomTeleportEvent(player, region).callEvent();
        });

        int delay = player.hasPermission("simplertp.teleport.nowarmup") ? 0 : 60;
        if (delay == 0 && plugin.getServer().isOwnedByCurrentRegion(player)) {
            teleport.run();
            return;
        }

        ScheduledTask task = player.getScheduler().runDelayed(plugin, t -> teleport.run(), null, Math.max(1, delay));

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

    @EventHandler
    public void on(PlayerQuitEvent event) {
        teleports.remove(event.getPlayer().getUniqueId());
    }

    @Nullable
    public Instant getCooldownTime(final UUID uuid) {
        return this.teleportCooldowns.asMap().get(uuid);
    }
}
