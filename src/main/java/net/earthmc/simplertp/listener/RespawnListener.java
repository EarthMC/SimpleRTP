package net.earthmc.simplertp.listener;

import net.earthmc.simplertp.SimpleRTP;
import net.earthmc.simplertp.compat.TownyCompat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {
    private final SimpleRTP plugin;

    public RespawnListener(final SimpleRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.config().respawnNearbyOnDeath())
            return;

        // Return if the player is already being teleported to a respawn anchor or bed, or the respawn is already being handled by towny
        if (event.isAnchorSpawn() || event.isBedSpawn() || TownyCompat.handlesRespawn(event.getPlayer(), event.getPlayer().getLocation()))
            return;

        event.setRespawnLocation(plugin.generator().generateRespawnLocation(event.getPlayer().getLocation()));
    }
}
