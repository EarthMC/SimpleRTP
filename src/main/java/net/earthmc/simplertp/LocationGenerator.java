package net.earthmc.simplertp;

import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class LocationGenerator {
    private final SimpleRTP plugin;
    private final Queue<Location> safeLocations = new ConcurrentLinkedQueue<>();
    private final World world = Bukkit.getWorld(NamespacedKey.minecraft("overworld"));
    private int runningTasks = 0;
    private int taskID = -1;

    public LocationGenerator(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getLogger().info("Location generator has been started.");

        taskID = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (safeLocations.size() + runningTasks >= 10)
                return;

            runningTasks++;
            try {
                CompletableFuture<Location> loc = generateRandomLocation();
                if (loc != null) {
                    loc.thenAcceptAsync(location -> {
                        if (location != null)
                            safeLocations.add(location);
                    });
                }
            } finally {
                runningTasks--;
            }
        }, 0, 2L).getTaskId();
    }

    public void stop() {
        plugin.getLogger().info("Location generator has been stopped.");

        if (taskID != -1)
            Bukkit.getScheduler().cancelTask(taskID);

        runningTasks = 0;
        safeLocations.clear();
    }

    @Nullable
    public CompletableFuture<Location> generateRandomLocation() {
        final int x = ThreadLocalRandom.current().nextInt(plugin.config().getMinX(), plugin.config().getMaxX());
        final int z = ThreadLocalRandom.current().nextInt(plugin.config().getMinZ(), plugin.config().getMaxZ());

        if (plugin.townyCompat() != null && !plugin.townyCompat().isWilderness(new Location(world, x, 0, z)))
            return null;

        Location location = new Location(world, x, 0, z);
        return world.getChunkAtAsync(x / 16, z / 16).thenApplyAsync(chunk -> {
            Block block = world.getHighestBlockAt(location);

            if (!isBlockSafe(block) || !isBlockAllowed(block))
                return null;

            return block.getLocation().add(0.5, 1, 0.5);
        });
    }

    public Location generateRespawnLocation(Location deathLocation) {
        if (deathLocation.getWorld().getEnvironment() == World.Environment.NORMAL) {
            for (int i = 0; i < 50; i++) {
                final int xOffset = ThreadLocalRandom.current().nextInt(40, 100) * (ThreadLocalRandom.current().nextBoolean() ? -1 : 1);
                final int zOffset = ThreadLocalRandom.current().nextInt(40, 100) * (ThreadLocalRandom.current().nextBoolean() ? -1 : 1);

                Block respawnBlock = deathLocation.getWorld().getHighestBlockAt(deathLocation.getBlockX() + xOffset, deathLocation.getBlockZ() + zOffset, HeightMap.MOTION_BLOCKING);

                if (isBlockSafe(respawnBlock))
                    return respawnBlock.getLocation().add(0.5, 1, 0.5);
            }
        }

        // Return a random tp location if no valid nearby location was found
        return getAndRemove();
    }

    public Location generateRandomLocationImmediately() {
        while (true) {
            final int x = ThreadLocalRandom.current().nextInt(plugin.config().getMinX(), plugin.config().getMaxX());
            final int z = ThreadLocalRandom.current().nextInt(plugin.config().getMinZ(), plugin.config().getMaxZ());

            World world = Bukkit.getWorld(NamespacedKey.minecraft("overworld"));

            Block block = world.getHighestBlockAt(x, z);

            if (block.getY() >= plugin.config().getMaxY() || !plugin.config().isBiomeAllowed(block.getBiome()) || !plugin.config().isBlockAllowed(block.getType()) || !block.isSolid())
                continue;

            return block.getLocation().add(0.5, 1, 0.5);
        }
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
            return Bukkit.getWorld(NamespacedKey.minecraft("overworld")).getSpawnLocation();
        else
            return safeLocations.remove();
    }
}
