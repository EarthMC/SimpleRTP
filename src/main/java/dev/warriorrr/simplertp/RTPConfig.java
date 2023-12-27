package dev.warriorrr.simplertp;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.block.Biome;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class RTPConfig {
    private final SimpleRTP plugin;

    private final Set<Biome> blacklistedBiomes = new HashSet<>();
    private final Set<Material> blacklistedBlocks = new HashSet<>();

    public RTPConfig(SimpleRTP plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        blacklistedBlocks.clear();
        blacklistedBiomes.clear();
        plugin.reloadConfig();

        for (final String biomeName : plugin.getConfig().getStringList("blacklisted-biomes")) {
            final NamespacedKey key = NamespacedKey.fromString(biomeName.toLowerCase(Locale.ROOT));

            if (key == null) {
                plugin.getLogger().warning("Invalid biome namespaced key '" + biomeName + "'.");
                continue;
            }

            final Biome biome = Registry.BIOME.get(key);
            if (biome != null)
                blacklistedBiomes.add(biome);
            else
                plugin.getLogger().warning("Could not find a biome with key " + key);
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

                if (tag != null)
                    blacklistedBlocks.addAll(tag.getValues());
                else
                    plugin.getLogger().warning("Could not find a material/tag for block '" +block + "'.");
            }
        }
    }

    public int getMinX() {
        return plugin.getConfig().getInt("minX");
    }

    public int getMaxX() {
        return plugin.getConfig().getInt("maxX");
    }

    public int getMinZ() {
        return plugin.getConfig().getInt("minZ");
    }

    public int getMaxZ() {
        return plugin.getConfig().getInt("maxZ");
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
}
