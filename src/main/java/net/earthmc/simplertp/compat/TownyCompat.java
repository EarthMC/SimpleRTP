package net.earthmc.simplertp.compat;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TownyCompat {
    private static boolean enabled = false;

    public static void enable() {
        enabled = true;
    }

    public static boolean isWilderness(World world, int x, int z) {
        return !enabled || TownyAPI.getInstance().isWilderness(WorldCoord.parseWorldCoord(world.getName(), x, z));
    }

    public static boolean handlesRespawn(Player player, Location deathLocation) {
        if (!enabled) return false;
        if (!TownySettings.isTownRespawning() || TownyAPI.getInstance().getTown(player) == null)
            return false;

        if (TownySettings.isTownRespawningInOtherWorlds())
            return true;
        else {
            final Location townyRespawnLocation = TownyAPI.getInstance().getTownSpawnLocation(player);
            if (townyRespawnLocation == null)
                return false;

            // When town respawning in other worlds is disabled, towny will only handle the respawn if the worlds are the same
            return deathLocation.getWorld().equals(townyRespawnLocation.getWorld());
        }
    }
}
