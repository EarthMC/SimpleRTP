package net.earthmc.simplertp;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Locale;

public class RTPConfig {
    private final FileConfiguration config;
    private final EnumSet<Biome> blacklistedBiomes = EnumSet.noneOf(Biome.class);
    private final EnumSet<Material> blacklistedBlocks = EnumSet.noneOf(Material.class);

    public RTPConfig(FileConfiguration config) {
        this.config = config;

        for (String biomeName : config.getStringList("blacklisted-biomes")) {
            try {
                blacklistedBiomes.add(Biome.valueOf(biomeName.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }

        for (String materialName : config.getStringList("blacklisted-blocks")) {
            String name = materialName.toUpperCase(Locale.ROOT);
            try {
                blacklistedBlocks.add(Material.valueOf(name));
            } catch (IllegalArgumentException e) {
                // No material with this name found, check if it's a valid tag
                try {
                    Field field = Tag.class.getDeclaredField(name);
                    field.setAccessible(true);

                    Tag<Material> tag = (Tag<Material>) field.get(null);
                    blacklistedBlocks.addAll(tag.getValues());
                } catch (NoSuchFieldException nsfe) {
                    SimpleRTP.instance().getLogger().warning("Invalid material/tag name: " + materialName);
                } catch (Exception ignored) {}
            }
        }
    }

    public int getMinX() {
        return config.getInt("minX");
    }

    public int getMaxX() {
        return config.getInt("maxX");
    }

    public int getMinZ() {
        return config.getInt("minZ");
    }

    public int getMaxZ() {
        return config.getInt("maxZ");
    }

    public int getMaxY() {
        return config.getInt("maxY");
    }

    public boolean isBiomeAllowed(Biome biome) {
        return !blacklistedBiomes.contains(biome);
    }

    public boolean isBlockAllowed(Material material) {
        return !blacklistedBlocks.contains(material);
    }
}
