package dev.warriorrr.simplertp;

import dev.warriorrr.simplertp.model.Area;
import dev.warriorrr.simplertp.model.Region;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class RTPConfig {
    private final SimpleRTP plugin;

    private final Set<Biome> blacklistedBiomes = new HashSet<>();
    private final Set<Material> blacklistedBlocks = new HashSet<>();

    private final Map<String, Region> regions = new HashMap<>();
    private final List<Region> regionsList = new ArrayList<>();
    private final List<Region> regionsView = Collections.unmodifiableList(regionsList);

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

            final Region region = new Region(regionName, areas);
            this.regions.put(regionName.toLowerCase(Locale.ROOT), region);
        }

        this.regionsList.clear();
        this.regionsList.addAll(this.regions.values());
    }

    public Region getRandomRegion() {
        return this.regionsList.get(ThreadLocalRandom.current().nextInt(this.regionsList.size()));
    }

    @UnmodifiableView
    public List<Region> getRegions() {
        return this.regionsView;
    }

    public @Nullable Region getRegionByName(String regionName) {
        return regions.get(regionName.toLowerCase(Locale.ROOT));
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
