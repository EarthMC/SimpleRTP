package net.earthmc.simplertp.compat;

import com.palmergames.bukkit.towny.TownyAPI;
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
}
