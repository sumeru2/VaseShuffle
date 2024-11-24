package com.sumeru.vaseshuffle;

import com.sumeru.vaseshuffle.commands.MainCommand;
import com.sumeru.vaseshuffle.game.Minigame;
import com.sumeru.vaseshuffle.listener.EventListener;
import com.sumeru.vaseshuffle.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class VaseShuffle extends JavaPlugin {
    public static Logger LOGGER;
    public static boolean usingViaAPI = false;
    public static VaseShuffle instance;
    public static Minigame minigame;
    @Override
    public void onEnable() {
        LOGGER = this.getLogger();
        instance = this;
        saveDefaultConfig();
        LOGGER.info("Plugin enabled!");
        Location vSpawn = Utils.readLocation("spawn");
        getCommand("vshuffle").setExecutor(new MainCommand());
        if (vSpawn==null) {
            LOGGER.info("Coordinates of the spawn of the vases are not specified!(/vshuffle setspawn)");
            return;
        }
        minigame = new Minigame(vSpawn.getWorld(), vSpawn, getConfig().getInt("cooldown"), getConfig().getInt("time-to-think"));
        Bukkit.getPluginManager().registerEvents(new EventListener(), this);
        if (Bukkit.getPluginManager().getPlugin("ViaVersion") != null) {
            usingViaAPI = true;
        }
    }

    @Override
    public void onDisable() {
        if (minigame!=null && !minigame.players.isEmpty()) {
            minigame.cancelGame(minigame.players.getFirst());
        }
        LOGGER.info("Plugin disabled!");
    }
}
