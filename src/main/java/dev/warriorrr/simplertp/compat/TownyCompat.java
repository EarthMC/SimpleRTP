package dev.warriorrr.simplertp.compat;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TownyCompat {
    public boolean isWilderness(Location location) {
        return TownyAPI.getInstance().isWilderness(location);
    }

    public boolean isWilderness(World world, int x, int z) {
        return TownyAPI.getInstance().isWilderness(WorldCoord.parseWorldCoord(world.getName(), x, z));
    }

    public boolean hasTown(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);

        return resident != null && resident.hasTown();
    }

    public boolean handlesRespawn(Player player, Location deathLocation) {
        if (!TownySettings.isTownRespawning() || !hasTown(player))
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
