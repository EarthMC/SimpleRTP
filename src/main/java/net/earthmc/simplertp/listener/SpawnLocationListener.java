package net.earthmc.simplertp.listener;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import net.earthmc.simplertp.SimpleRTP;
import net.earthmc.simplertp.model.GeneratedLocation;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public class SpawnLocationListener implements Listener {
    private final SimpleRTP plugin;
    private final Map<UUID, String> pendingMessages = new ConcurrentHashMap<>();

    public SpawnLocationListener(final SimpleRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(final AsyncPlayerSpawnLocationEvent event) {
        if (!event.isNewPlayer() || !plugin.config().rtpFirstJoin())
            return;

        final GeneratedLocation generatedLoc = plugin.generator().getAndRemove();
        final Location location = generatedLoc.location();
        event.setSpawnLocation(location);

        pendingMessages.put(event.getConnection().getProfile().getId(), "<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " in " + generatedLoc.region().name() + ".");
    }

    @EventHandler
    public void onClientLoadedWorld(final PlayerClientLoadedWorldEvent event) {
        final String message = pendingMessages.remove(event.getPlayer().getUniqueId());
        if (message != null) {
            event.getPlayer().sendRichMessage(message);
        }
    }

    @EventHandler
    public void onConnectionClose(final PlayerConnectionCloseEvent event) {
        pendingMessages.remove(event.getPlayerUniqueId());
    }
}
