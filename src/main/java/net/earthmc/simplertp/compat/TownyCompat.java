package net.earthmc.simplertp.compat;

import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Location;

public class TownyCompat {
    public boolean isWilderness(Location location) {
        return TownyAPI.getInstance().isWilderness(location);
    }
}
