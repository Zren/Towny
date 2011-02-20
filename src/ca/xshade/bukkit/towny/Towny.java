package ca.xshade.bukkit.towny;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;

import ca.xshade.bukkit.towny.command.TownyCommand;
import ca.xshade.bukkit.towny.command.TownyCommandMap;
import ca.xshade.bukkit.towny.event.TownyBlockListener;
import ca.xshade.bukkit.towny.event.TownyEntityListener;
import ca.xshade.bukkit.towny.event.TownyEntityMonitorListener;
import ca.xshade.bukkit.towny.event.TownyPlayerListener;
import ca.xshade.bukkit.towny.event.TownyPlayerLowListener;
import ca.xshade.bukkit.towny.object.Coord;
import ca.xshade.bukkit.towny.object.TownyIConomyObject;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.object.WorldCoord;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * Towny Plugin for Bukkit
 * 
 * Website: https://sites.google.com/site/townymod/
 * Source: https://github.com/Zren/Towny
 * 
 * @author Shade
 */

/*
 * TODO
 * 
 * Commenting
 * flatfile-old data source and replace getDataSource() with a manager class
 * Log nation/town chat
 * Log town/nation messages with timestamps. On login, see if there's been any events. Make command: /town log [page] to see messages.
 * Update cache when adding/removing people
 * Replace "/resident" with settings.getFirstCommand(settings.getResidentCommands()) etc in help messages
 * When adding allying another nation, ask that nation and add this nation to their ally list.
 * Make the formatting/wording for [nation] .. [nation] etc, better.
 * Re-register a player after res delete
 * Comply with the updated API onCommand. Move commands to onCommand (at least console type ones).
 * Organise PlayerListener. Functions. Functions EVERYWHERE!
 * Use unclaim/claim selections with the /plot command
 * SortedList<Integer> townLevels to efficiently find the keys for the level hashmap.
 * Claim circle [radius] 
 * 
 * When a town/nation is deleted, the money leftover will be put in the war pot.
 * At the begining of war, all towns/nations enter the buyin.
 * Need new TownyObject. 
 * Iconomy name: towny-war-spoils
 * 
 * Managed to claim 5 out of the 16 selected (x,z) .. (x1,z2).
 * 
 * Permissions:
 * towny.claim
 * towny.newtown
 * towny.newnation
 * 
 * 
 * --- Cool Concepts ---
 * 
 * Town spawn will spawn a obsidian portal. When spawning, the portal activates for a few seconds.
 * In the wild, users who've registered less than X time are exempt to permissions. 
 * 
 * 
 * --- Timeline ---
 * 
 * Unclaim
 * Npc
 * Flatfile
 * 
 */
public class Towny extends JavaPlugin {
	private String version = "2.0.0";

	private final TownyPlayerListener playerListener = new TownyPlayerListener(this);
	private final TownyBlockListener blockListener = new TownyBlockListener(this);
	private final TownyEntityListener entityListener = new TownyEntityListener(this);
	private final TownyPlayerLowListener playerLowListener = new TownyPlayerLowListener(this);
	private final TownyEntityMonitorListener entityMonitorListener = new TownyEntityMonitorListener(this);
	private TownyUniverse townyUniverse;
	private Map<String, PlayerCache> playerCache = Collections.synchronizedMap(new HashMap<String, PlayerCache>());
	private Map<String, List<String>> playerMode = Collections.synchronizedMap(new HashMap<String, List<String>>());
	private Permissions permissions = null;
	
	public Towny(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
		version = this.getDescription().getVersion();
	}
	
	public static void main(String[] args) {
		TownyUniverse universe = new TownyUniverse("C:\\Users\\Admin\\Desktop\\Bukkit\\Server\\plugins\\Towny");
		universe.loadSettings();
		System.out.println("[Towny] Database: [" + TownySettings.getLoadDatabase() + "] ");
		universe.setDataSource(TownySettings.getSaveDatabase());
		universe.getDataSource().initialize(null, universe);
		universe.getDataSource().loadAll();
        for (String line : universe.getTreeString(0))
        	System.out.println(line);
	}

	@Override
	public void onEnable() {
		registerEvents();
		
		PluginDescriptionFile pdfFile = this.getDescription();
		pdfFile.getVersion();
		
		townyUniverse = new TownyUniverse(this);
		loadSettings();
		
		System.out.println("[Towny] Database: [Load] " + TownySettings.getLoadDatabase() + " [Save] " + TownySettings.getSaveDatabase());
		if (!townyUniverse.loadDatabase(TownySettings.getLoadDatabase())) {
			System.out.println("[Towny] Error: Failed to load!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		try {
			townyUniverse.setDataSource(TownySettings.getSaveDatabase());
			townyUniverse.getDataSource().initialize(this, townyUniverse);
			try {
				townyUniverse.getDataSource().backup();
			} catch (IOException e) {
				System.out.println("[Towny] Error: Could not create backup.");
				e.printStackTrace();
			}
			
			townyUniverse.getDataSource().saveAll();
		} catch (UnsupportedOperationException e) {
			System.out.println("[Towny] Error: Unsupported save format!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		Plugin test = getServer().getPluginManager().getPlugin("Permissions");
		if(permissions == null)
			if(test != null)
				permissions = (Permissions)test;
			else
				System.out.println("[Towny] Permission system not enabled. Towny Admins not loaded.");
		
		test = getServer().getPluginManager().getPlugin("iConomy");
		if (test == null)
			setSetting(TownySettings.Bool.USING_ICONOMY, false);
		test = getServer().getPluginManager().getPlugin("Essentials");
		if (test == null)
			setSetting(TownySettings.Bool.USING_ESSENTIALS, false);
		
		onLoad();
		
		if (TownySettings.isFirstRun()) {
			firstRun();
			TownySettings.setBoolean(getDataFolder().getPath() + "/settings/config.properties", TownySettings.Bool.FIRST_RUN, false);
			townyUniverse.loadSettings();
		}
		
		System.out.println("[Towny] Version: " + version + " - Mod Enabled");
		
		// Re login anyone online. (In case of plugin reloading)
		for (Player player : getServer().getOnlinePlayers())
			try {
				getTownyUniverse().onLogin(player);
			} catch (TownyException x) {
				sendErrorMsg(player, x.getError());
			}
	}

	@Override
	public void onDisable() {
		if (townyUniverse.getDataSource() != null)
			townyUniverse.getDataSource().saveAll();
		
		if (getTownyUniverse().isWarTime())
			getTownyUniverse().getWarEvent().toggleEnd();
		townyUniverse.toggleDailyTimer(false);
		townyUniverse.toggleMobRemoval(false);
		townyUniverse.toggleHealthRegen(false);
		
		playerCache.clear();
		playerMode.clear();
		
		townyUniverse = null;
		
		System.out.println("[Towny] Version: " + version + " - Mod Disabled");
	}
	
	public void loadSettings() {
		townyUniverse.loadSettings();
		Coord.setCellSize(TownySettings.getTownBlockSize());
		TownyIConomyObject.setPlugin(this);
		TownyCommand.setUniverse(townyUniverse);
	}
	
	public void onLoad() {
		loadSettings();
		townyUniverse.toggleDailyTimer(true);
		townyUniverse.toggleMobRemoval(TownySettings.isRemovingMobs());
		townyUniverse.toggleHealthRegen(TownySettings.hasHealthRegen());
	}

	private void registerEvents() {
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, playerLowListener, Priority.Low, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Normal, this);

		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_INTERACT, blockListener, Priority.Normal, this);

		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGEDBY_ENTITY, entityListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGEDBY_ENTITY, entityMonitorListener, Priority.Monitor, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Normal, this);
	}
	
	private void firstRun() {
		System.out.println("------------------------------");
		System.out.println("[Towny] Detected first run");
		
		try {
			String newLine = System.getProperty("line.separator");
			BufferedWriter fout = new BufferedWriter(new FileWriter(getDataFolder().getPath() + "/settings/town-levels.csv"));
			fout.write("0,, Ruin:Spirit ,,1" + newLine);
			fout.write("1,, Hamlet,,,16" + newLine);
			fout.write("2,, Village,Mayor ,,64" + newLine);
			fout.write("6,, Town,Lord ,,128" + newLine);
			fout.write("12,, City,Lord ,,256" + newLine);
			fout.close();
			System.out.println("[Towny] Registered default town levels.");
		} catch (Exception e) {
			System.out.println("[Towny] Error: Could not write default town levels file.");
		}
		try {
			String newLine = System.getProperty("line.separator");
			BufferedWriter fout = new BufferedWriter(new FileWriter(getDataFolder().getPath() + "/settings/nation-levels.csv"));
			fout.write("0,, Wilderness,, Lands,Leader ," + newLine);
			fout.write("1,Dominion of , ,, Center,Leader," + newLine);
			fout.write("2,Lands of ,,, Center,Leader ," + newLine);
			fout.write("3,, Country,, Lands,King ," + newLine);
			fout.write("6,, Kingdom,, Lands,King ," + newLine);
			fout.write("12,, Empire,, Lands,Emperor ," + newLine);
			fout.close();
			System.out.println("[Towny] Registered default nation levels.");
		} catch (Exception e) {
			System.out.println("[Towny] Error: Could not write default nation levels file.");
		}
		System.out.println("------------------------------");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		String command = cmd.getName().toLowerCase();
		if (command.equals("towny"))
			return new TownyCommandMap(getServer()).execute(sender, commandLabel, args);
		return false;	
	}

	public TownyUniverse getTownyUniverse() {
		return townyUniverse;
	}

	public void sendErrorMsg(Player player, String msg) {
		for (String line : ChatTools.color(Colors.Gold + "[Towny] " + Colors.Rose + msg))
			player.sendMessage(line);
		sendDevMsg(msg);
	}
	
	public void sendErrorMsg(String msg) {
		System.out.println("[Towny] Error: " + msg);
	}
	
	public void sendDevMsg(String msg) {
		if (TownySettings.isDevMode()) {
			Player townyDev = getServer().getPlayer("Shadeness");
			if (townyDev == null)
				return;
			for (String line : ChatTools.color(Colors.Gold + "[Towny] DevMode: " + Colors.Rose + msg))
				townyDev.sendMessage(line);
		}
	}
	
	public void sendDebugMsg(String msg) {
		if (TownySettings.getDebug())
			System.out.println("[Towny] Debug: " + msg);
		sendDevMsg(msg);
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
		try {
			playerCache.put(player.getName().toLowerCase(), new PlayerCache(getTownyUniverse().getWorld(player.getWorld().getName()), player));
		} catch (NotRegisteredException e) {
			sendErrorMsg(player, "Could not create permission cache for this world.");
		}
		
	}
	
	public void deleteCache(Player player) {
		playerCache.remove(player.getName().toLowerCase());
	}
	
	public PlayerCache getCache(Player player) {
		if (!hasCache(player))
			newCache(player);
		
		return playerCache.get(player.getName().toLowerCase());
	}
	
	public void updateCache(WorldCoord worldCoord) {
		for (Player player : getServer().getOnlinePlayers())
			if (Coord.parseCoord(player).equals(worldCoord))
				getCache(player).setLastTownBlock(worldCoord); //Automatically resets permissions.
	}
	
	public void updateCache() {
		for (Player player : getServer().getOnlinePlayers())
			try {
				getCache(player).setLastTownBlock(new WorldCoord(getTownyUniverse().getWorld(player.getWorld().getName()), Coord.parseCoord(player)));
			} catch (NotRegisteredException e) {
				deleteCache(player);
			}
	}
	
	public boolean isTownyAdmin(Player player) {
		if (player.isOp())
			return true;
		return hasPermission(player, "towny.admin");
	}
	
	public void setPlayerMode(Player player, String[] modes) {
		playerMode.put(player.getName(), Arrays.asList(modes));
	}
	
	public void removePlayerMode(Player player) {
		playerMode.remove(player.getName());
	}
	
	public List<String> getPlayerMode(Player player) {
		return playerMode.get(player.getName());
	}
	
	public boolean hasPlayerMode(Player player, String mode) {
		List<String> modes = getPlayerMode(player);
		if (modes == null)
			return false;
		else
			return modes.contains(mode); 
	}
	
	public List<String> getPlayerMode(String name) {
		return playerMode.get(name);
	}
	
	public boolean hasPlayerMode(String name, String mode) {
		List<String> modes = getPlayerMode(name);
		if (modes == null)
			return false;
		else
			return modes.contains(mode); 
	}

	@SuppressWarnings("static-access")
	public boolean checkEssentialsTeleport(Player player) {
		if (!TownySettings.isUsingEssentials())
			return true;
		
		Plugin test = getServer().getPluginManager().getPlugin("Essentials");
		if (test == null)
			return true;
		Essentials essentials = (Essentials)test;
		essentials.loadClasses();
		sendDebugMsg("Using Essenitials");
		
		
		test = getServer().getPluginManager().getPlugin("EssentialsTele");
		if (test == null)
			return true;
		sendDebugMsg("Using EssenitialsTele");
		
		try {
			User user = User.get(player, getServer());
			user.teleportCooldown();
			user.charge("tp");
		} catch (Exception e) {
			sendErrorMsg(player, "Error: " + e.getMessage());
			return false;
		}
		
			
		return true;
	}

	public boolean hasPermission(Player player, String node) {
		if (permissions != null)
			return Permissions.Security.permission(player, node);
		else
			return false;
	}

	public void sendMsg(String msg) {
		System.out.println("[Towny] " + msg);
	}
	
	public String getConfigPath() {
		return getDataFolder().getPath() + "/settings/config.properties";
	}
	
	public void setSetting(Object key, Object value) {
		if (key instanceof TownySettings.Bool && value instanceof Boolean)
			TownySettings.setBoolean(getConfigPath(), (TownySettings.Bool)key, (Boolean)value);
		
		//TODO: the rest
	}
}
