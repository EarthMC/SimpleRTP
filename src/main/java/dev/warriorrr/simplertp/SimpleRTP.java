package dev.warriorrr.simplertp;

import dev.warriorrr.simplertp.commands.RTPCommand;
import dev.warriorrr.simplertp.compat.TownyCompat;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SimpleRTP extends JavaPlugin {
    private final RTPConfig config = new RTPConfig(this);
    private LocationGenerator generator;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.config.loadConfig();

        final World world = Bukkit.getWorld(NamespacedKey.minecraft("overworld"));

        if (world == null) {
            getLogger().severe("Unable to find a world with namespace minecraft:overworld, disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.generator = new LocationGenerator(this, world);
        generator.start();

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Objects.requireNonNull(getCommand("rtp")).setExecutor(new RTPCommand(this));

        if (Bukkit.getPluginManager().isPluginEnabled("Towny")) {
            TownyCompat.enable();
        }
    }

    @Override
    public void onDisable() {
        if (generator != null)
            generator.stop();
    }

    public RTPConfig config() {
        return config;
    }

    public LocationGenerator generator() {
        return generator;
    }

    public void reload() {
        this.config.loadConfig();
    }
}
