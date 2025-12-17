package net.earthmc.simplertp;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.earthmc.simplertp.commands.RTPCommand;
import net.earthmc.simplertp.compat.TownyCompat;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleRTP extends JavaPlugin {
    private final RTPConfig config = new RTPConfig(this);
    private LocationGenerator generator;
    private TeleportHandler teleportHandler;

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
        getServer().getAsyncScheduler().runNow(this, task -> {
            generator.loadGeneratedLocations(getDataPath().resolve("locations.json"));
            generator.start();
        });

        teleportHandler = new TeleportHandler(this);
        Bukkit.getPluginManager().registerEvents(teleportHandler, this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(RTPCommand.createCommand("rtp", this, teleportHandler), "Teleports the player to a random location.");
            event.registrar().register(RTPCommand.createCommand("tprandom", this, teleportHandler), "Teleports the player to a random location.");
        });

        if (Bukkit.getPluginManager().isPluginEnabled("Towny")) {
            TownyCompat.enable();
        }
    }

    @Override
    public void onDisable() {
        if (generator != null) {
            generator.stop();
            generator.persistGeneratedLocations(getDataPath().resolve("locations.json"));
        }
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

    public TeleportHandler teleportHandler() {
        return teleportHandler;
    }
}
