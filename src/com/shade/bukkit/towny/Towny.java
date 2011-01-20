package com.shade.bukkit.towny;

import java.io.File;

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
 */
public class Towny extends JavaPlugin {
	private String version = "2.0.0";

	private final TownyPlayerListener playerListener = new TownyPlayerListener(this);
	private final TownyBlockListener blockListener = new TownyBlockListener(this);
	private final TownyEntityListener entityListener = new TownyEntityListener(this);
	private TownyUniverse townyUniverse;

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
		townyUniverse.loadSettings();
		System.out.print("[Towny] Database: [" + townyUniverse.getSettings().getLoadDatabase() + "] ");
		if (townyUniverse.loadDatabase())
			System.out.println("Loaded database");
		else
			System.out.println("Failed to load!\n[Towny] Error: .!.!.!.!.!.!.");

		Coord.setCellSize(townyUniverse.getSettings().getTownBlockSize());
		TownyIConomyObject.setPlugin(this);

		townyUniverse.getDataSource().saveAll();

		System.out.println("[Towny] Version: " + version + " - Mod Enabled");
	}

	@Override
	public void onDisable() {
		if (getTownyUniverse().isWarTime())
			getTownyUniverse().getWarEvent().end();
		System.out.println("[Towny] Version: " + version + " - Mod Disabled");
	}

	private void registerEvents() {
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent( Event.Type.PLAYER_TELEPORT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Normal, this);

		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);

		getServer().getPluginManager().registerEvent( Event.Type.ENTITY_DAMAGEDBY_ENTITY, entityListener, Priority.Normal, this);
	}

	public TownyUniverse getTownyUniverse() {
		return townyUniverse;
	}

	public void sendErrorMsg(Player player, String msg) {
		for (String line : ChatTools.color(Colors.Gold + "[Towny] " + Colors.Rose + msg))
			player.sendMessage(line);
		if (getTownyUniverse().getSettings().getDebug())
			System.out.println("[Towny] UserError: " + player.getName() + ": " + msg);
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

	public void updatePlayerCache() {
		clearPlayerCache();
		for (Player player : getServer().getOnlinePlayers())
			updatePlayerCache(player);
	}

	public void clearPlayerCache() {
		blockListener.clearCache();
	}

	public void updatePlayerCache(Player player) {
		blockListener.updatePlayerCache(player);
	}

	public void updatePlayerCache(Coord coord) {
		for (Player player : getServer().getOnlinePlayers())
			if (Coord.parseCoord(player).equals(coord))
				updatePlayerCache(player);
	}
}
