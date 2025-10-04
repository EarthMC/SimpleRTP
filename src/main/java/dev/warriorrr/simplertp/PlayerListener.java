package dev.warriorrr.simplertp;

import dev.warriorrr.simplertp.compat.TownyCompat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
        if (event.getPlayer().hasPlayedBefore() || !plugin.config().rtpFirstJoin())
            return;

        final Location location = plugin.generator().getAndRemove();
        if (location == null) return;
        event.setSpawnLocation(location);

        final Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, task -> player.sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."), () -> {}, 1L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.config().respawnNearbyOnDeath())
            return;

        // Return if the player is already being teleported to a respawn anchor or bed, or the respawn is already being handled by towny
        if (event.isAnchorSpawn() || event.isBedSpawn() || (TownyCompat.getEnabled() && TownyCompat.handlesRespawn(event.getPlayer(), event.getPlayer().getLocation())))
            return;

        event.setRespawnLocation(plugin.generator().generateRespawnLocation(event.getPlayer().getLocation()));
    }
}
