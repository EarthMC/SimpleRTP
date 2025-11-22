package net.earthmc.simplertp.event;

import net.earthmc.simplertp.model.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RandomTeleportEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Region region;

    public RandomTeleportEvent(Player player, Region region) {
        this.player = player;
        this.region = region;
    }

    public Player getPlayer() {
        return player;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
