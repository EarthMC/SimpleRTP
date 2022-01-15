package net.earthmc.simplertp;

import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class LocationGenerator {
    private final SimpleRTP plugin;
    private final Queue<Location> safeLocations = new ConcurrentLinkedQueue<>();
    private int runningTasks = 0;
    private int taskID = -1;

    public LocationGenerator(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getLogger().info("Location generator has been started.");

        taskID = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (safeLocations.size() + runningTasks >= 5)
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

        World world = Bukkit.getWorld(plugin.config().getDefaultWorld());

        if (plugin.townyCompat() != null && !plugin.townyCompat().isWilderness(new Location(world, x, 0, z)))
            return null;

        final int chunkX = x % 16;
        final int chunkZ = z % 16;
        return world.getChunkAtAsync(x / 16, z / 16).thenApplyAsync(chunk -> {
            LevelChunk levelChunk = ((CraftChunk) chunk).getHandle();
            int highestY = levelChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, chunkX, chunkZ);
            Block block = chunk.getBlock(chunkX, highestY, chunkZ);

            if (block.getY() >= plugin.config().getMaxY() || !plugin.config().isBiomeAllowed(block.getBiome()) || !plugin.config().isBlockAllowed(block.getType()) || !block.isSolid())
                return null;

            return block.getLocation().add(0.5, 1, 0.5);
        });
    }

    public Location generateRandomLocationImmediately() {
        while (true) {
            final int x = ThreadLocalRandom.current().nextInt(plugin.config().getMinX(), plugin.config().getMaxX());
            final int z = ThreadLocalRandom.current().nextInt(plugin.config().getMinZ(), plugin.config().getMaxZ());

            World world = Bukkit.getWorld(plugin.config().getDefaultWorld());

            Block block = world.getHighestBlockAt(x, z);

            if (block.getY() >= plugin.config().getMaxY() || !plugin.config().isBiomeAllowed(block.getBiome()) || !plugin.config().isBlockAllowed(block.getType()) || !block.isSolid())
                continue;

            return block.getLocation().add(0.5, 1, 0.5);
        }
    }

    public Location getAndRemove() {
        if (safeLocations.isEmpty())
            return generateRandomLocationImmediately();
        else
            return safeLocations.remove();
    }
}
