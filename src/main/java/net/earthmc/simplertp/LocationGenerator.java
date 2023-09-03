package net.earthmc.simplertp;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LocationGenerator {
    private final SimpleRTP plugin;
    private final Queue<Location> safeLocations = new ConcurrentLinkedQueue<>();
    private final World world;
    private final AtomicInteger runningTasks = new AtomicInteger();
    private ScheduledTask task;

    public LocationGenerator(SimpleRTP plugin, World world) {
        this.plugin = plugin;
        this.world = world;
    }

    public void start() {
        plugin.getLogger().info("Location generator has been started.");
        runningTasks.set(0);

        task = plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, t -> {
            if (safeLocations.size() + runningTasks.get() >= 10)
                return;

            runningTasks.incrementAndGet();

            this.generateRandomLocation()
                    .thenAccept(location -> {
                        if (location != null)
                            safeLocations.add(location);
                    })
                    .whenComplete((r, e) -> runningTasks.decrementAndGet());
        }, 1L, 40L, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        plugin.getLogger().info("Location generator has been stopped.");

        if (task != null)
            task.cancel();

        runningTasks.set(0);
        safeLocations.clear();
    }

    @NotNull
    public CompletableFuture<Location> generateRandomLocation() {
        final int x = ThreadLocalRandom.current().nextInt(plugin.config().getMinX(), plugin.config().getMaxX());
        final int z = ThreadLocalRandom.current().nextInt(plugin.config().getMinZ(), plugin.config().getMaxZ());

        if (plugin.townyCompat() != null && !plugin.townyCompat().isWilderness(world, x, z))
            return CompletableFuture.completedFuture(null);

        final CompletableFuture<Location> future = new CompletableFuture<>();

        plugin.getServer().getRegionScheduler().run(plugin, world, x, z, task -> {
            world.getChunkAtAsync(x >> 4, z >> 4).thenRun(() -> {
                final Block block = world.getHighestBlockAt(x, z);

                if (!isBlockSafe(block) || !isBlockAllowed(block))
                    future.complete(null);

                future.complete(block.getLocation().add(0.5, 1, 0.5));
            });
        });

        return future;
    }

    public Location generateRespawnLocation(Location deathLocation) {
        if (deathLocation.getWorld().getEnvironment() == World.Environment.NORMAL) {
            for (int i = 0; i < 50; i++) {
                final int xOffset = ThreadLocalRandom.current().nextInt(40, 100) * (ThreadLocalRandom.current().nextBoolean() ? -1 : 1);
                final int zOffset = ThreadLocalRandom.current().nextInt(40, 100) * (ThreadLocalRandom.current().nextBoolean() ? -1 : 1);

                final Block respawnBlock = deathLocation.getWorld().getHighestBlockAt(deathLocation.getBlockX() + xOffset, deathLocation.getBlockZ() + zOffset, HeightMap.MOTION_BLOCKING);

                if (isBlockSafe(respawnBlock) && isBlockAllowed(respawnBlock))
                    return respawnBlock.getLocation().add(0.5, 1, 0.5);
            }
        }

        // Return a random tp location if no valid nearby location was found
        return getAndRemove();
    }

    public boolean isBlockSafe(Block block) {
        return block.getY() < plugin.config().getMaxY()
                && !block.isLiquid()
                && block.getWorld().getWorldBorder().isInside(block.getLocation());
    }

    public boolean isBlockAllowed(Block block) {
        return plugin.config().isBiomeAllowed(block.getBiome())
                && plugin.config().isBlockAllowed(block.getType());
    }

    public Location getAndRemove() {
        if (safeLocations.isEmpty())
            return world.getSpawnLocation();
        else
            return safeLocations.remove();
    }
}
