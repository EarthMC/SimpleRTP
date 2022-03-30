package net.earthmc.simplertp;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class PlayerListener implements Listener {

    private final SimpleRTP plugin;

    public PlayerListener(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawnLocationEvent(PlayerSpawnLocationEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) {
            Location location = plugin.generator().getAndRemove();
            event.setSpawnLocation(location);

            Bukkit.getScheduler().runTaskLaterAsynchronously(SimpleRTP.instance(), () -> {
                event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."));
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Return if the player is already being teleported to a respawn anchor or bed, or the player has a town
        if (event.isAnchorSpawn() || event.isBedSpawn() || (plugin.townyCompat() != null && plugin.townyCompat().hasTown(event.getPlayer())))
            return;

        event.setRespawnLocation(plugin.generator().generateRespawnLocation(event.getPlayer().getLocation()));
    }
}
