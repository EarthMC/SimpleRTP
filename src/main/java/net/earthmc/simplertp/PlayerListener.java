package net.earthmc.simplertp;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onSpawnLocationEvent(PlayerSpawnLocationEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) {
            Location location = SimpleRTP.instance().generator().getAndRemove();
            event.setSpawnLocation(location);

            Bukkit.getScheduler().runTaskLaterAsynchronously(SimpleRTP.instance(), () -> {
                event.getPlayer().sendMessage(MiniMessage.miniMessage().parse("<gradient:blue:aqua>You have been randomly teleported to: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "."));
            }, 5L);
        }
    }
}
