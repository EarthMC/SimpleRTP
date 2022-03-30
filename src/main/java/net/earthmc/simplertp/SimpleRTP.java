package net.earthmc.simplertp;

import net.earthmc.simplertp.commands.RTPCommand;
import net.earthmc.simplertp.compat.TownyCompat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleRTP extends JavaPlugin {
    private static SimpleRTP instance;
    private RTPConfig config;
    private LocationGenerator generator;
    private TownyCompat townyCompat;

    @Override
    public void onEnable() {
        SimpleRTP.instance = this;
        this.saveDefaultConfig();
        FileConfiguration configuration = getConfig();
        this.config = new RTPConfig(configuration);

        this.generator = new LocationGenerator(this);
        generator.start();

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginCommand("rtp").setExecutor(new RTPCommand(this));

        if (Bukkit.getPluginManager().isPluginEnabled("Towny"))
            townyCompat = new TownyCompat();
    }

    @Override
    public void onDisable() {
        instance = null;

        if (generator != null)
            generator.stop();
    }

    public static SimpleRTP instance() {
        return instance;
    }

    public RTPConfig config() {
        return config;
    }

    public LocationGenerator generator() {
        return generator;
    }

    public void reload() {
        this.reloadConfig();
        this.config = new RTPConfig(getConfig());
    }

    public TownyCompat townyCompat() {
        return townyCompat;
    }
}
