package net.earthmc.simplertp;

import net.earthmc.simplertp.commands.RTPCommand;
import net.earthmc.simplertp.compat.TownyCompat;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleRTP extends JavaPlugin {
    private RTPConfig config;
    private LocationGenerator generator;
    private TownyCompat townyCompat;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration configuration = getConfig();
        this.config = new RTPConfig(this, configuration);

        World world = Bukkit.getWorld(NamespacedKey.minecraft("overworld"));

        if (world == null) {
            getLogger().severe("Unable to find a world with namespace minecraft:overworld, disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.generator = new LocationGenerator(this, world);
        generator.start();

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginCommand("rtp").setExecutor(new RTPCommand(this));

        if (Bukkit.getPluginManager().isPluginEnabled("Towny"))
            townyCompat = new TownyCompat();
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
        this.reloadConfig();
        this.config = new RTPConfig(this, getConfig());
    }

    public TownyCompat townyCompat() {
        return townyCompat;
    }
}
