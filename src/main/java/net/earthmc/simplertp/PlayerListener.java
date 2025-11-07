package net.earthmc.simplertp;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import net.earthmc.simplertp.compat.TownyCompat;
import net.earthmc.simplertp.model.GeneratedLocation;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    private final SimpleRTP plugin;

    public PlayerListener(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler @SuppressWarnings("UnstableApiUsage")
    public void onSpawnLocationEvent(AsyncPlayerSpawnLocationEvent event) {
        if (!event.isNewPlayer() || !plugin.config().rtpFirstJoin())
            return;

        final GeneratedLocation generated = plugin.generator().getAndRemove();
        final Location loc = generated.location();
        event.setSpawnLocation(loc);

        event.getConnection().getAudience().sendMessage(
                MiniMessage.miniMessage().deserialize(
                        "<gradient:blue:aqua>You have been randomly teleported to: "
                                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                                + " in " + generated.region().name() + "."
                )
        );
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
