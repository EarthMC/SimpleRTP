package net.earthmc.simplertp.event;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Preconditions;
import io.papermc.paper.connection.PlayerCommonConnection;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConnection;
import io.papermc.paper.connection.PlayerGameConnection;
import net.earthmc.simplertp.model.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Called before a player is about to be randomly teleported.
 */
@SuppressWarnings("UnstableApiUsage")
public class RandomTeleportEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final PlayerCommonConnection connection;
    private Region region;
    private Location location;
    private Duration cooldownTime;
    private boolean cancelled;

    @ApiStatus.Internal
    public RandomTeleportEvent(final PlayerCommonConnection connection, final Region region, final Location location, final Duration cooldownTime, final boolean async) {
        super(async);
        this.connection = connection;
        this.region = region;
        this.location = location;
    }

    @Deprecated(forRemoval = true)
    public @Nullable Player getPlayer() {
        if (this.connection instanceof PlayerGameConnection gameConnection) {
            return gameConnection.getPlayer();
        } else {
            return null;
        }
    }

    public PlayerConnection getPlayerConnection() {
        return this.connection;
    }

    public PlayerProfile getPlayerProfile() {
        if (this.connection instanceof PlayerGameConnection gameConnection) {
            return gameConnection.getPlayer().getPlayerProfile();
        } else {
            return ((PlayerConfigurationConnection) this.connection).getProfile();
        }
    }

    public Region getRegion() {
        return region;
    }

    public Location getLocation() {
        return location;
    }

    public Duration getCooldownTime() {
        return cooldownTime;
    }

    /**
     * Updates the cooldown time, ignored if the player is not manually teleporting themselves (i.e. on login).
     *
     * @param cooldownTime The new cooldown time that a player must wait for before manually teleporting again.
     * @throws IllegalArgumentException if cooldown time is negative.
     */
    public void setCooldownTime(Duration cooldownTime) {
        Preconditions.checkArgument(!cooldownTime.isNegative(), "cooldownTime must not be negative");
        this.cooldownTime = cooldownTime;
    }

    /**
     * Changes the region and location where this player is being teleported to.
     *
     * @param region The new region.
     * @param location The new location.
     * @see Region#custom(String) creating a new region instance with a custom name
     */
    public void modifyLocation(final Region region, final Location location) {
        this.region = region;
        this.location = location;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
