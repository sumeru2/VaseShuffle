package com.sumeru.vaseshuffle.utils;

import com.sumeru.vaseshuffle.VaseShuffle;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Set;

public class Utils {
    public static void writeLocation(String name, Location location) {
        if (VaseShuffle.instance.getConfig().getConfigurationSection(name)==null) {
            VaseShuffle.instance.getConfig().createSection(name);
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        String world = location.getWorld().getName();

        VaseShuffle.instance.getConfig().set(name+".x", x);
        VaseShuffle.instance.getConfig().set(name+".y", y);
        VaseShuffle.instance.getConfig().set(name+".z", z);
        VaseShuffle.instance.getConfig().set(name+".world", world);
        VaseShuffle.instance.saveConfig();
    }
    public static Location readLocation(String name) {
        Set<String> keys = VaseShuffle.instance.getConfig().getConfigurationSection(name).getKeys(false);
        if (keys.size()==4) {
            int x = VaseShuffle.instance.getConfig().getInt(name+".x");
            int y = VaseShuffle.instance.getConfig().getInt(name+".y");
            int z = VaseShuffle.instance.getConfig().getInt(name+".z");
            String world = VaseShuffle.instance.getConfig().getString(name+".world");
            return new Location(Bukkit.getWorld(world), x, y, z);
        } else {
            return null;
        }
    }
}
