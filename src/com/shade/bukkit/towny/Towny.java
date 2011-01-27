package com.shade.bukkit.towny;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;

import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;

/**
 * Towny Plugin for Bukkit
 * 
 * Website: https://sites.google.com/site/townymod/ Source:
 * https://github.com/Zren/Towny
 * 
 * @author Shade
 */

/*
 * TODO
 * 
 * Commenting
 * Unclaim townblocks
 * flatfile-old data source and replace getDataSource() with a manager class
 * log chat
 * permissions
 * update cache when adding/removing people
 * fix chat messages to not edit display name but format string
 * save/load Nation.isNeutral, and make a /nation set command
 * prevent command use during war
 * allies
 * replace "/resident" with settings.getFirstCommand(settings.getResidentCommands()) etc in help messages
 * access and withdraw from bank accounts
 * more iconomy prices (new nation, claim block, etc)
 * delete townyobject
 * update map for resident owned plots (yellow?)
 * 
 * iconomy
 * plot taxe & resident tax
 * new nation cost
 */
public class Towny extends JavaPlugin {
	private String version = "2.0.0";

	private final TownyPlayerListener playerListener = new TownyPlayerListener(this);
	private final TownyBlockListener blockListener = new TownyBlockListener(this);
	private final TownyEntityListener entityListener = new TownyEntityListener(this);
	private TownyUniverse townyUniverse;
	private Map<String, PlayerCache> playerCache = Collections.synchronizedMap(new HashMap<String, PlayerCache>());

	public Towny(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
		version = this.getDescription().getVersion();
	}

	@Override
	public void onEnable() {
		registerEvents();

		PluginDescriptionFile pdfFile = this.getDescription();
		pdfFile.getVersion();

		townyUniverse = new TownyUniverse(this);
		System.out.print("[Towny] Database: [" + TownySettings.getLoadDatabase() + "] ");
		if (townyUniverse.loadDatabase())
			System.out.println("Loaded database");
		else {
			System.out.println("Failed to load!");
			getServer().getPluginManager().disablePlugin(this);
		}
		
		onLoad();

		townyUniverse.getDataSource().saveAll();
		
		if (TownySettings.isFirstRun()) {
			firstRun();
			TownySettings.setBoolean(getDataFolder().getPath() + "/settings/config.properties", TownySettings.Bool.FIRST_RUN, false);
		}

		System.out.println("[Towny] Version: " + version + " - Mod Enabled");
	}

	@Override
	public void onDisable() {
		townyUniverse.getDataSource().saveAll();
		
		if (getTownyUniverse().isWarTime())
			getTownyUniverse().getWarEvent().end();
		System.out.println("[Towny] Version: " + version + " - Mod Disabled");
	}
	
	public void onLoad() {
		townyUniverse.loadSettings();
		Coord.setCellSize(TownySettings.getTownBlockSize());
		TownyIConomyObject.setPlugin(this);
	}

	private void registerEvents() {
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Normal, this);

		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);

		getServer().getPluginManager().registerEvent( Event.Type.ENTITY_DAMAGEDBY_ENTITY, entityListener, Priority.Normal, this);
	}
	
	private void firstRun() {
		System.out.println("------------------------------");
		System.out.println("[Towny] Detected first run");
		System.out.println("[Towny] Registering default");
		System.out.println("------------------------------");
	}

	public TownyUniverse getTownyUniverse() {
		return townyUniverse;
	}

	public void sendErrorMsg(Player player, String msg) {
		for (String line : ChatTools.color(Colors.Gold + "[Towny] " + Colors.Rose + msg))
			player.sendMessage(line);
		if (TownySettings.getDebug())
			System.out.println("[Towny] UserError: " + player.getName() + ": " + msg);
	}
	
	public void sendErrorMsg(String msg) {
		System.out.println("[Towny] Error: " + msg);
	}

	public void sendErrorMsg(Player player, String[] msg) {
		for (String line : msg)
			sendErrorMsg(player, line);
	}

	public void sendMsg(Player player, String msg) {
		for (String line : ChatTools.color(Colors.Gold + "[Towny] " + Colors.Green + msg))
			player.sendMessage(line);
	}

	public String getVersion() {
		return version;
	}

	public World getServerWorld(String name) throws NotRegisteredException {
		for (World world : getServer().getWorlds())
			if (world.getName().equals(name))
				return world;

		throw new NotRegisteredException();
	}
	
	public boolean hasCache(Player player) {
		return playerCache.containsKey(player.getName().toLowerCase());
	}
	
	public void newCache(Player player) {
		playerCache.put(player.getName().toLowerCase(), new PlayerCache(player));
	}
	
	public void deleteCache(Player player) {
		playerCache.remove(player.getName().toLowerCase());
	}
	
	public PlayerCache getCache(Player player) {
		if (!hasCache(player))
			newCache(player);
		
		return playerCache.get(player.getName().toLowerCase());
	}
	
	public void updateCache(Coord coord) {
		for (Player player : getServer().getOnlinePlayers())
			if (Coord.parseCoord(player).equals(coord))
				getCache(player).setLastTownBlock(coord); //Automatically resets permissions.
	}
	
	public void updateCache() {
		for (Player player : getServer().getOnlinePlayers())
			getCache(player).setLastTownBlock(Coord.parseCoord(player));
	}
}
