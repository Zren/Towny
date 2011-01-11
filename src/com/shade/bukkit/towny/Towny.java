package com.shade.bukkit.towny;

import java.io.File;
import org.bukkit.Player;
import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;

import com.shade.bukkit.towny.TownyUniverse;

/**
 * Towny
 * Plugin for Bukkit
 * 
 * Website: https://sites.google.com/site/townymod/
 * Source: https://github.com/Zren/Towny
 * 
 * @author Shade
 */
public class Towny extends JavaPlugin {
    private final TownyPlayerListener playerListener = new TownyPlayerListener(this);
    private TownyUniverse townyUniverse;
    
    public Towny(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File plugin, ClassLoader cLoader) {
        super(pluginLoader, instance, desc, plugin, cLoader);
        
        registerEvents();
        townyUniverse = new TownyUniverse(this);
    }

    public void onDisable() {
        System.out.println("[Towny] Mod Disabled");
    }

    public void onEnable() {
        System.out.println("[Towny] Mod Enabled");
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
    }
    
    public TownyUniverse getTownyUniverse() {
    	return townyUniverse;
    }
    
    public void sendErrorMsg(Player player, String msg) {
    	player.sendMessage("[Towny] " + msg);
    	System.out.print("[Towny] " + msg);
    }
}
