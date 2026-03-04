package net.earthmc.simplertp.listener;

import net.earthmc.simplertp.SimpleRTP;
import net.earthmc.simplertp.event.RandomTeleportEvent;
import net.earthmc.simplertp.model.GeneratedLocation;
import net.earthmc.simplertp.model.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.time.Duration;

@SuppressWarnings({"removal", "UnstableApiUsage"})
public class LegacySpawnLocationListener implements Listener {
    private final SimpleRTP plugin;

    public LegacySpawnLocationListener(final SimpleRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawnLocationEvent(PlayerSpawnLocationEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPlayedBefore() || !plugin.config().rtpFirstJoin())
            return;

        final GeneratedLocation generatedLoc = plugin.generator().getAndRemove();
        Region region = generatedLoc.region();
        Location location = generatedLoc.location();

        final RandomTeleportEvent rtpEvent = new RandomTeleportEvent(player.getConnection(), region, location, Duration.ZERO, event.isAsynchronous());
        if (!rtpEvent.callEvent()) {
            return;
        }

        region = rtpEvent.getRegion();
        location = rtpEvent.getLocation();

        event.setSpawnLocation(location);

        final String message = "<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + " in " + region.name() + ".";
        player.getScheduler().runDelayed(plugin, task -> player.sendRichMessage(message), null, 1L);
    }
}
