package net.earthmc.simplertp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.earthmc.simplertp.collection.ImmutableQueue;
import net.earthmc.simplertp.compat.TownyCompat;
import net.earthmc.simplertp.model.Area;
import net.earthmc.simplertp.model.GeneratedLocation;
import net.earthmc.simplertp.model.Region;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LocationGenerator {
    private static final int SAVED_LOCATIONS_PER_REGIONS = 10;
    private static final Queue<Location> EMPTY_QUEUE = ImmutableQueue.instance();

    private final SimpleRTP plugin;
    private final Map<Region, Queue<Location>> regionMap = new ConcurrentHashMap<>();
    private int totalSavedLocations = 0;
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
            if (runningTasks.get() + totalSavedLocations >= SAVED_LOCATIONS_PER_REGIONS * plugin.config().getRegions().size()) {
                return;
            }

            Region region = plugin.config().getRandomRegion();
            if (region == null || regionMap.getOrDefault(region, EMPTY_QUEUE).size() >= SAVED_LOCATIONS_PER_REGIONS) {
                return;
            }

            Area area = region.getRandomArea();
            if (area == null) {
                return;
            }

            runningTasks.incrementAndGet();
            this.generateRandomLocation(area)
                    .thenAccept(location -> {
                        if (location != null) {
                            regionMap.computeIfAbsent(region, list -> new ConcurrentLinkedQueue<>()).add(location);
                            totalSavedLocations++;
                        }
                    })
                    .whenComplete((r, e) -> runningTasks.decrementAndGet());
        }, 1L, 50, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        plugin.getLogger().info("Location generator has been stopped.");

        if (task != null)
            task.cancel();

        runningTasks.set(0);
    }

    public void reload() {
        regionMap.clear();
        totalSavedLocations = 0;
        // Allow locations to be repopulated with newly configured regions
    }

    @NotNull
    public CompletableFuture<Location> generateRandomLocation(Area area) {
        final int x = ThreadLocalRandom.current().nextInt(area.minX(), area.maxX());
        final int z = ThreadLocalRandom.current().nextInt(area.minZ(), area.maxZ());

        if (!TownyCompat.isWilderness(world, x, z)) {
            return CompletableFuture.completedFuture(null);
        }

        final CompletableFuture<Location> future = new CompletableFuture<>();

        plugin.getServer().getRegionScheduler().run(plugin, world, x >> 4, z >> 4, task -> world.getChunkAtAsync(x >> 4, z >> 4).thenRun(() -> {
            final Block block = world.getHighestBlockAt(x, z);

            if (!isBlockSafe(block) || !isBlockAllowed(block)) {
                future.complete(null);
            }

            future.complete(block.getLocation().add(0.5, 1, 0.5));
        }));

        return future;
    }

    public Location generateRespawnLocation(Location deathLocation) {
        final World world = deathLocation.getWorld();

        if (world.getEnvironment() == World.Environment.NORMAL) {
            for (int i = 0; i < 50; i++) {
                final int xOffset = ThreadLocalRandom.current().nextInt(40, 100) * (ThreadLocalRandom.current().nextBoolean() ? -1 : 1);
                final int zOffset = ThreadLocalRandom.current().nextInt(40, 100) * (ThreadLocalRandom.current().nextBoolean() ? -1 : 1);

                final int blockX = deathLocation.getBlockX() + xOffset;
                final int blockZ = deathLocation.getBlockZ() + zOffset;

                if (!plugin.getServer().isOwnedByCurrentRegion(world, blockX >> 4, blockZ >> 4)) continue;

                final Block respawnBlock = deathLocation.getWorld().getHighestBlockAt(blockX, blockZ, HeightMap.MOTION_BLOCKING);

                if (isBlockSafe(respawnBlock) && isBlockAllowed(respawnBlock)) {
                    return respawnBlock.getLocation().add(0.5, 1, 0.5);
                }
            }
        }

        // Return a random tp location if no valid nearby location was found
        return getAndRemove().location();
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

    public @NotNull GeneratedLocation getAndRemove() {
        for (int i = 0; i < 50; i++) {
            final GeneratedLocation randomLocation = getSpawnForRegion(plugin.config().getRandomRegion());

            if (randomLocation != null) {
                return randomLocation;
            }
        }

        plugin.getLogger().warning("Could not find a safe locations! Falling back to world spawn.");
        return new GeneratedLocation(new Region("Spawn", List.of()), world.getSpawnLocation());
    }

    public @Nullable GeneratedLocation getSpawnForRegion(Region region) {
        Queue<Location> locations = regionMap.get(region);
        if (locations == null) {
            return null;
        }

        final Location location = locations.poll();
        if (location == null) {
            return null;
        }

        totalSavedLocations--;
        return new GeneratedLocation(region, location);
    }

    public int getTasksSize() {
        return runningTasks.get();
    }

    public int getLocationsSize() {
        return totalSavedLocations;
    }

    public void persistGeneratedLocations(Path filePath) {
        final JsonObject object = new JsonObject();

        for (final Map.Entry<Region, Queue<Location>> entry : regionMap.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            final JsonArray locationsArray = new JsonArray();
            for (final Location location : entry.getValue()) {
                final JsonObject locationObject = new JsonObject();

                locationObject.addProperty("x", location.getX());
                locationObject.addProperty("y", location.getY());
                locationObject.addProperty("z", location.getZ());
                locationsArray.add(locationObject);
            }

            object.add(entry.getKey().name(), locationsArray);
        }

        try {
            Files.writeString(filePath, new GsonBuilder().setPrettyPrinting().create().toJson(object), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            plugin.getSLF4JLogger().warn("Failed to persist generated locations", e);
        }
    }

    public void loadGeneratedLocations(Path filePath) {
        if (!Files.exists(filePath)) {
            return;
        }

        final JsonObject object;
        try {
            object = new Gson().fromJson(Files.readString(filePath), JsonObject.class);
            Files.deleteIfExists(filePath); // prevent re-using the same locations if server exits unexpectedly
        } catch (IOException e) {
            plugin.getSLF4JLogger().warn("Failed to read persisted generated locations from disk", e);
            return;
        }

        for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
            final Region region = plugin.config().getRegionByName(entry.getKey());
            if (region == null) {
                continue;
            }

            for (final JsonElement locationElement : entry.getValue().getAsJsonArray()) {
                final JsonObject locationObject = locationElement.getAsJsonObject();

                final Location location = new Location(world, locationObject.get("x").getAsDouble(), locationObject.get("y").getAsDouble(), locationObject.get("z").getAsDouble());

                regionMap.computeIfAbsent(region, k -> new ConcurrentLinkedQueue<>()).add(location);
                totalSavedLocations++;
            }
        }

        plugin.getSLF4JLogger().info("Loaded {} location{} from disk.", totalSavedLocations, totalSavedLocations == 1 ? "" : "s");
    }
}
