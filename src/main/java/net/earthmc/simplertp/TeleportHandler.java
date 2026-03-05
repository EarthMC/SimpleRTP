package net.earthmc.simplertp;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
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
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportHandler implements Listener {

    private final SimpleRTP plugin;
    private final Map<UUID, ScheduledTask> teleports = new ConcurrentHashMap<>();
    private final Duration cooldownTime;
    private final Cache<UUID, Instant> teleportCooldowns;

    public TeleportHandler(SimpleRTP plugin) {
        this.plugin = plugin;

        this.cooldownTime = Duration.ofMinutes(plugin.config().getTeleportCooldownMinutes());
        this.teleportCooldowns = CacheBuilder.newBuilder().expireAfterWrite(this.cooldownTime).build();
    }

    public void addTeleport(Player player, Region region, Location location) {
        final UUID uuid = player.getUniqueId();
        final Runnable retired = () -> cancelPendingTeleport(uuid);

        final Runnable teleport = () -> {
            final RandomTeleportEvent event = new RandomTeleportEvent(player.getConnection(), region, location, cooldownTime, false);
            if (!event.callEvent()) {
                return;
            }

            final Region region1 = event.getRegion();
            final Location location1 = event.getLocation();

            player.teleportAsync(location1).thenRunAsync(() -> {
                player.sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location1.getBlockX() + ", " + location1.getBlockY() + ", " + location1.getBlockZ() + " in " + region1.name() + ".");

                if (!player.hasPermission("simplertp.teleport.nocooldown")) {
                    teleportCooldowns.asMap().put(uuid, Instant.now().plus(event.getCooldownTime()));
                }

                teleports.remove(uuid);
            }, task -> player.getScheduler().run(plugin, t -> task.run(), retired));
        };

        int delay = player.hasPermission("simplertp.teleport.nowarmup") ? 0 : 60;
        if (delay == 0 && plugin.getServer().isOwnedByCurrentRegion(player)) {
            teleport.run();
            return;
        }

        ScheduledTask task = player.getScheduler().runDelayed(plugin, t -> teleport.run(), retired, Math.max(1, delay));

        teleports.put(player.getUniqueId(), task);
    }

    public boolean hasTeleport(Player player) {
        return teleports.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (cancelPendingTeleport(player.getUniqueId())) {
            player.sendMessage(Component.text("Teleportation cancelled due to taking damage.", NamedTextColor.DARK_RED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!event.hasChangedBlock()) {
            return;
        }

        if (cancelPendingTeleport(player.getUniqueId())) {
            player.sendMessage(Component.text("Teleportation cancelled due to movement.", NamedTextColor.DARK_RED));
        }
    }

    @EventHandler
    public void on(PlayerConnectionCloseEvent event) {
        cancelPendingTeleport(event.getPlayerUniqueId());
    }

    private boolean cancelPendingTeleport(final UUID uuid) {
        final ScheduledTask task = this.teleports.remove(uuid);
        if (task != null) {
            final boolean cancelled = task.getExecutionState() == ScheduledTask.ExecutionState.IDLE;
            task.cancel();

            return cancelled;
        }

        return false;
    }

    @Nullable
    public Instant getCooldownTime(final UUID uuid) {
        return this.teleportCooldowns.asMap().get(uuid);
    }
}
