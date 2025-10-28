package dev.warriorrr.simplertp;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class RTPConfig {
    private final SimpleRTP plugin;

    private final Set<Biome> blacklistedBiomes = new HashSet<>();
    private final Set<Material> blacklistedBlocks = new HashSet<>();
    private final Map<Region, Integer> regions = new HashMap<>();

    public RTPConfig(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        blacklistedBlocks.clear();
        blacklistedBiomes.clear();
        plugin.reloadConfig();

        final FileConfiguration config = plugin.getConfig();

        for (final String biomeName : plugin.getConfig().getStringList("blacklisted-biomes")) {
            final NamespacedKey key = NamespacedKey.fromString(biomeName.toLowerCase(Locale.ROOT));

            if (key == null) {
                plugin.getLogger().warning("Invalid biome namespaced key '" + biomeName + "'.");
                continue;
            }

            final Biome biome = Registry.BIOME.get(key);
            if (biome != null) {
                blacklistedBiomes.add(biome);

            } else {
                plugin.getLogger().warning("Could not find a biome with key " + key);
            }
        }

        for (final String block : plugin.getConfig().getStringList("blacklisted-blocks")) {
            final NamespacedKey key = NamespacedKey.fromString(block.toLowerCase(Locale.ROOT));

            if (key == null) {
                plugin.getLogger().warning("Invalid material/tag name: " + block);
                continue;
            }

            final Material mat = Registry.MATERIAL.get(key);
            if (mat != null)
                blacklistedBlocks.add(mat);
            else {
                // No material with this name found, check if it's a valid tag
                final Tag<Material> tag = plugin.getServer().getTag(Tag.REGISTRY_BLOCKS, key, Material.class);

                if (tag != null) {
                    blacklistedBlocks.addAll(tag.getValues());
                } else {
                    plugin.getLogger().warning("Could not find a material/tag for block '" +block + "'.");
                }
            }
        }

        ConfigurationSection regions = config.getConfigurationSection("regions");
        if (regions == null) {
            regions = config.createSection("regions");
        }

        for (final String regionName : regions.getKeys(false)) {
            final ConfigurationSection regionConfig = regions.getConfigurationSection(regionName);

            final List<Map<?, ?>> rawAreas = regionConfig.getMapList("areas");
            final List<Area> areas = new ArrayList<>();

            for (final Map<?, ?> area : rawAreas) {
                areas.add(new Area(
                    (int) area.get("minX"),
                    (int) area.get("maxX"),
                    (int) area.get("minZ"),
                    (int) area.get("maxZ")
                ));
            }
            if (areas.isEmpty()) return;

            this.regions.put(new Region(regionName, areas), 0);
        }
    }

    public record Region(String name, List<Area> areas) {
        public Area getRandomArea() {
            int totalWeight = 0;
            for (Area a : areas) {
                totalWeight += a.size();
            }

            int random = ThreadLocalRandom.current().nextInt(totalWeight);
            int c = 0;
            for (Area a : areas) {
                c += a.size();
                if (c > random) return a;
            }
            return areas.get(0);
        }
        public int size() {
            AtomicInteger size = new AtomicInteger();
            areas.forEach(area -> size.addAndGet(area.size()));
            return size.get();
        }
    }

    public record Area(int minX, int maxX, int minZ, int maxZ) {
        public int size() {
            return (maxX - minX + 1) * (maxZ - minZ + 1);
        }
    }

    public Region getRandomRegion() {
        var regionList = regions.keySet().stream().toList();
        int totalWeight = 0;
        for (Region r : regionList) {
            totalWeight += r.size();
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int c = 0;
        for (Region r : regionList) {
            c += r.size();
            if (c > random) return r;
        }
        return regionList.get(0);
    }

    public List<Region> getRegions() {
        return new ArrayList<>(regions.keySet());
    }

    public int getMaxY() {
        return plugin.getConfig().getInt("maxY");
    }

    public boolean isBiomeAllowed(Biome biome) {
        return !blacklistedBiomes.contains(biome);
    }

    public boolean isBlockAllowed(Material material) {
        return !blacklistedBlocks.contains(material);
    }

    public boolean rtpFirstJoin() {
        return plugin.getConfig().getBoolean("random-teleport-on-first-join", true);
    }

    public boolean respawnNearbyOnDeath() {
        return plugin.getConfig().getBoolean("respawn-nearby-on-death", false);
    }
}
