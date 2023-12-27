package dev.warriorrr.simplertp;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

public class PlayerListener implements Listener {

    private final SimpleRTP plugin;
    private static final MethodHandle HAD_INVALID_WORLD;

    public PlayerListener(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawnLocationEvent(PlayerSpawnLocationEvent event) {
        boolean hadInvalidWorld = false;
        if (HAD_INVALID_WORLD != null) {
            try {
                hadInvalidWorld = (boolean) HAD_INVALID_WORLD.invokeExact(event.getPlayer());

                if (hadInvalidWorld)
                    plugin.getLogger().info("Randomly teleporting " + event.getPlayer().getName() + " because their world was invalid.");
            } catch (Throwable ignored) {}
        }

        if (!event.getPlayer().hasPlayedBefore() || hadInvalidWorld) {
            Location location = plugin.generator().getAndRemove();
            event.setSpawnLocation(location);

            plugin.getServer().getAsyncScheduler().runDelayed(plugin, task -> {
                event.getPlayer().sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ".");
            }, 1L, TimeUnit.SECONDS);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Return if the player is already being teleported to a respawn anchor or bed, or the player has a town
        if (event.isAnchorSpawn() || event.isBedSpawn() || (plugin.townyCompat() != null && plugin.townyCompat().hasTown(event.getPlayer())))
            return;

        event.setRespawnLocation(plugin.generator().generateRespawnLocation(event.getPlayer().getLocation()));
    }

    static {
        MethodHandle temp = null;
        try {
            //noinspection JavaReflectionMemberAccess
            temp = MethodHandles.publicLookup().unreflect(Player.class.getMethod("hadInvalidWorld"));
        } catch (Throwable ignored) {}

        HAD_INVALID_WORLD = temp;
    }
}
