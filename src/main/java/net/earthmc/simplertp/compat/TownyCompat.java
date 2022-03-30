package net.earthmc.simplertp.compat;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TownyCompat {
    public boolean isWilderness(Location location) {
        return TownyAPI.getInstance().isWilderness(location);
    }

    public boolean hasTown(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);

        return resident != null && resident.hasTown();
    }
}
