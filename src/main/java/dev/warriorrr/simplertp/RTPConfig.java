package dev.warriorrr.simplertp;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.block.Biome;

import java.util.*;

public class RTPConfig {
    private final SimpleRTP plugin;

    private final Set<Biome> blacklistedBiomes = new HashSet<>();
    private final Set<Material> blacklistedBlocks = new HashSet<>();
    private final Map<Region, Integer> regions = new HashMap<>();
    private static int blocksNeededPerUse = 1000;

    public RTPConfig(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        blacklistedBlocks.clear();
        blacklistedBiomes.clear();
        plugin.reloadConfig();

        blocksNeededPerUse = plugin.getConfig().getInt("surfaceAreaPerUse");

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

        for (final String region : plugin.getConfig().getStringList("allowed-regions")) { // E.g. 100,50,45,20;minX,maxX,minZ,maxZ
            for (String coords : region.split(";")) {
                String[] coord = coords.split(",");
                int minX, maxX, minZ, maxZ;
                try {
                    minX = Integer.parseInt(coord[0]);
                    maxX = Integer.parseInt(coord[1]);
                    minZ = Integer.parseInt(coord[2]);
                    maxZ = Integer.parseInt(coord[3]);
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    plugin.getLogger().warning("Could not parse %s into integers".formatted(coords) + "\n" + e.getMessage());
                    continue;
                }
                regions.put(new Region(minX, maxX, minZ, maxZ), 0);
            }
        }
    }

    public record Region(int minX, int maxX, int minZ, int maxZ) {
        public int maxUses() {
            return ((maxX - minX) * (maxZ - minZ)) / blocksNeededPerUse; // For every 1000 blocks of surface area, region can be used once.
        }
    }

    public Region getNextRegion() {
        Iterator<Region> it = regions.keySet().iterator();
        Region next = it.next();
        while (next.maxUses() <= regions.get(next)) {
            regions.remove(next);
            next = it.next();
        }
        return next;
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
