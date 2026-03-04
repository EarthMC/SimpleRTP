package net.earthmc.simplertp.listener;

import net.earthmc.simplertp.SimpleRTP;
import net.earthmc.simplertp.model.GeneratedLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@SuppressWarnings("removal")
public class LegacySpawnLocationListener implements Listener {
    private final SimpleRTP plugin;

    public LegacySpawnLocationListener(final SimpleRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawnLocationEvent(PlayerSpawnLocationEvent event) {
        if (event.getPlayer().hasPlayedBefore() || !plugin.config().rtpFirstJoin())
            return;

        final GeneratedLocation generatedLoc = plugin.generator().getAndRemove();
        final Location location = generatedLoc.location();
        event.setSpawnLocation(location);

        final Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, task -> player.sendRichMessage("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " in " + generatedLoc.region().name() + "."), () -> {}, 1L);
    }
}
