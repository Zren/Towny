package ca.xshade.bukkit.towny;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import ca.xshade.bukkit.towny.PlayerCache.TownBlockStatus;
import ca.xshade.bukkit.towny.command.TownyCommand;
import ca.xshade.bukkit.towny.command.TownyCommandMap;
import ca.xshade.bukkit.towny.event.TownyBlockListener;
import ca.xshade.bukkit.towny.event.TownyEntityListener;
import ca.xshade.bukkit.towny.event.TownyEntityMonitorListener;
import ca.xshade.bukkit.towny.event.TownyPlayerListener;
import ca.xshade.bukkit.towny.event.TownyPlayerLowListener;
import ca.xshade.bukkit.towny.object.Coord;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.towny.object.TownBlock;
import ca.xshade.bukkit.towny.object.TownyIConomyObject;
import ca.xshade.bukkit.towny.object.TownyPermission;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.object.WorldCoord;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;
import ca.xshade.util.JavaUtil;
import ca.xshade.util.StringMgmt;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.nijiko.coelho.iConomy.iConomy;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * Towny Plugin for Bukkit
 * 
 * Website: towny.xshade.ca
 * Source: https://github.com/Zren/Towny
 * 
 * @author Shade
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
	private iConomy iconomy = null;
	private Permissions permissions = null;
	//private GroupManager groupManager = null;
	
	@Override
	public void onEnable() {
		version = this.getDescription().getVersion();
		
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
		
		checkPlugins();
		onLoad();
		
		if (TownySettings.isFirstRun()) {
			firstRun();
			TownySettings.setBoolean(getDataFolder().getPath() + "/settings/config.properties", TownySettings.Bool.FIRST_RUN, false);
			townyUniverse.loadSettings();
		}
		
		if (TownySettings.isTownyUpdating(getVersion()))
			update();
		
		System.out.println("[Towny] Version: " + version + " - Mod Enabled");
		
		// Re login anyone online. (In case of plugin reloading)
		for (Player player : getServer().getOnlinePlayers())
			try {
				getTownyUniverse().onLogin(player);
			} catch (TownyException x) {
				sendErrorMsg(player, x.getError());
			}
	}

	private void checkPlugins() {
		List<String> using = new ArrayList<String>();
		Plugin test = getServer().getPluginManager().getPlugin("GroupManager");
		//if(test != null)
		//	groupManager = (GroupManager)test;
		//else {
			test = getServer().getPluginManager().getPlugin("Permissions");
			if(test != null) {
				permissions = (Permissions)test;
				using.add("Permissions");
			} else
				System.out.println("[Towny] Neither Permissions nor GroupManager was found. Towny Admins not loaded. Ops only.");
		//}		
		
		test = getServer().getPluginManager().getPlugin("iConomy");
		if (test == null)
			setSetting(TownySettings.Bool.USING_ICONOMY, false);
		else {
			iconomy = (iConomy)test;
			if (TownySettings.isUsingIConomy())
				using.add("iConomy");
		}
		
		test = getServer().getPluginManager().getPlugin("Essentials");
		if (test == null)
			setSetting(TownySettings.Bool.USING_ESSENTIALS, false);
		
		test = getServer().getPluginManager().getPlugin("EssentialsTele");
		if (test == null)
			setSetting(TownySettings.Bool.USING_ESSENTIALS, false);
		else if (TownySettings.isUsingIConomy())
			using.add("Essentials");
		
		if (using.size() > 0)
			System.out.println("[Towny] Using: " + StringMgmt.join(using, ", "));
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
		if (TownySettings.isForcingPvP())
			for (Town town : townyUniverse.getTowns())
				town.setPVP(true);
		
		townyUniverse.toggleDailyTimer(true);
		townyUniverse.toggleMobRemoval(TownySettings.isRemovingMobs());
		townyUniverse.toggleHealthRegen(TownySettings.hasHealthRegen());
		updateCache();
	}

	private void registerEvents() {
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, playerLowListener, Priority.Low, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_ITEM, playerListener, Priority.Normal, this);

		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.BLOCK_INTERACT, blockListener, Priority.Normal, this);

		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGED, entityListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGED, entityMonitorListener, Priority.Monitor, this);
		getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Normal, this);
	}
	
	private void firstRun() {
		System.out.println("------------------------------------");
		System.out.println("[Towny] Detected first run");
		
		try {
			String newLine = System.getProperty("line.separator");
			BufferedWriter fout = new BufferedWriter(new FileWriter(getDataFolder().getPath() + "/settings/town-levels.csv"));
			fout.write("0,, Ruin,Spirit ,,1" + newLine);
			fout.write("1,, Hamlet,,,16" + newLine);
			fout.write("2,, Village,Mayor ,,64" + newLine);
			fout.write("6,, Town,Lord ,,128" + newLine);
			fout.write("12,, City,Lord ,,256");
			fout.close();
			System.out.println("[Towny] Registered default town levels.");
		} catch (Exception e) {
			System.out.println("[Towny] Error: Could not write default town levels file.");
		}
		try {
			String newLine = System.getProperty("line.separator");
			BufferedWriter fout = new BufferedWriter(new FileWriter(getDataFolder().getPath() + "/settings/nation-levels.csv"));
			fout.write("0,, Wilderness,, Lands,Leader ," + newLine);
			fout.write("1,Dominion of ,,, Center,Leader," + newLine);
			fout.write("2,Lands of ,,, Center,Leader ," + newLine);
			fout.write("3,, Country,, Lands,King ," + newLine);
			fout.write("6,, Kingdom,, Lands,King ," + newLine);
			fout.write("12,, Empire,, Lands,Emperor ,");
			fout.close();
			System.out.println("[Towny] Registered default nation levels.");
		} catch (Exception e) {
			System.out.println("[Towny] Error: Could not write default nation levels file.");
		}
		System.out.println("------------------------------------");
	}
	
	public void update() {
		try {
			List<String> changeLog = JavaUtil.readTextFromJar("/ChangeLog.txt");
			boolean display = false;
			System.out.println("------------------------------------");
			System.out.println("[Towny] ChangeLog up until v" + getVersion());
			String lastVersion = TownySettings.getLastRunVersion();
			for (String line : changeLog) { //TODO: crawl from the bottom, then past from that index.
				if (line.startsWith("v" + lastVersion))
					display = true;
				if (display && line.replaceAll(" ", "").replaceAll("\t", "").length() > 0)
					System.out.println(line);
			}
			System.out.println("------------------------------------");
		} catch (IOException e) {
			sendDebugMsg("Could not read ChangeLog.txt");
		}
		setSetting(TownySettings.Str.LAST_RUN_VERSION, getVersion());
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
		deleteCache(player.getName());
	}
	
	public void deleteCache(String name) {
		playerCache.remove(name.toLowerCase());
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
		sendDebugMsg("Perm Check: " + player.getName() + ": " + node);
		if (permissions != null) {
			sendDebugMsg("    Permissions installed.");
			boolean perm = Permissions.Security.permission(player, node);
			sendDebugMsg("    Permissions says "+perm+".");
			return perm;
		// } else if (groupManager != null)
		//	return groupManager.getHandler().permission(player, node);
		} else {
			sendDebugMsg("    Does not have permission.");
			return false;
		}
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
		else if (key instanceof TownySettings.Str && value instanceof String)
			TownySettings.setString(getConfigPath(), (TownySettings.Str)key, (String)value);
		//TODO: the rest
	}
	

	public TownBlockStatus getStatusCache(Player player, WorldCoord worldCoord) {
		if (isTownyAdmin(player))
			return TownBlockStatus.ADMIN;
		
		if (!worldCoord.getWorld().isUsingTowny())
			return TownBlockStatus.OFF_WORLD;
		
		TownyUniverse universe = getTownyUniverse();
		TownBlock townBlock;
		Town town;
		try {
			townBlock = worldCoord.getTownBlock();
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
			// Unclaimed Zone switch rights
			return TownBlockStatus.UNCLAIMED_ZONE;
		}

		try {
			Resident resident = universe.getResident(player.getName());
			
			// War Time switch rights
			if (universe.isWarTime())
				try {
					if (!resident.getTown().getNation().isNeutral() && !town.getNation().isNeutral())
						return TownBlockStatus.WARZONE;	
				} catch (NotRegisteredException e) {
				}
			
			// Resident Plot switch rights
			try {
				Resident owner = townBlock.getResident();
				if (resident == owner)
					return TownBlockStatus.PLOT_OWNER;
				else if (owner.hasFriend(resident))
					return TownBlockStatus.PLOT_FRIEND;
				else if (resident.hasTown() && townyUniverse.isAlly(resident.getTown(), owner.getTown()) )
					return TownBlockStatus.PLOT_ALLY;
				else
					// Exit out and use town permissions
					throw new TownyException();
			} catch (NotRegisteredException x) {
			} catch (TownyException x) {
			}

			// Town resident destroy rights
			if (!resident.hasTown())
				throw new TownyException();

			if (resident.getTown() != town) {
				// Allied destroy rights
				if (universe.isAlly(resident.getTown(), town))
					return TownBlockStatus.TOWN_ALLY;
				else
					return TownBlockStatus.OUTSIDER;
			} else if (resident.isMayor() || resident.getTown().hasAssistant(resident))
				return TownBlockStatus.TOWN_OWNER;
			else
				return TownBlockStatus.TOWN_RESIDENT;
		} catch (TownyException e) {
			// Outsider destroy rights
			return TownBlockStatus.OUTSIDER;
		}
	}
	
	public TownBlockStatus cacheStatus(Player player, WorldCoord worldCoord, TownBlockStatus townBlockStatus) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(worldCoord);
		cache.setStatus(townBlockStatus);

		sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Status: " + townBlockStatus);
		return townBlockStatus;
	}


	public void cacheBuild(Player player, WorldCoord worldCoord, boolean buildRight) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(worldCoord);
		cache.setBuildPermission(buildRight);

		sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Build: " + buildRight);
	}

	public void cacheDestroy(Player player, WorldCoord worldCoord, boolean destroyRight) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(worldCoord);
		cache.setDestroyPermission(destroyRight);

		sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Destroy: " + destroyRight);
	}
	
	public void cacheSwitch(Player player, WorldCoord worldCoord, boolean switchRight) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(worldCoord);
		cache.setSwitchPermission(switchRight);

		sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Switch: " + switchRight);
	}
	
	public void cacheItemUse(Player player, WorldCoord worldCoord, boolean itemUseRight) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(worldCoord);
		cache.setItemUsePermission(itemUseRight);

		sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Item Use: " + itemUseRight);
	}
	
	public void cacheBlockErrMsg(Player player, String msg) {
		PlayerCache cache = getCache(player);
		cache.setBlockErrMsg(msg);
	}
	
	public boolean getPermission(Player player, TownBlockStatus status, WorldCoord pos, TownyPermission.ActionType actionType) {
		if (status == TownBlockStatus.OFF_WORLD ||
			status == TownBlockStatus.ADMIN ||
			status == TownBlockStatus.WARZONE ||
			status == TownBlockStatus.PLOT_OWNER ||
			status == TownBlockStatus.TOWN_OWNER)
				return true;
		
		TownBlock townBlock;
		Town town;
		try {
			townBlock = pos.getTownBlock();
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
			// Wilderness Permissions
			if (status == TownBlockStatus.UNCLAIMED_ZONE)
				if (hasPermission(player, "towny.wild." + actionType.toString()))
					return true;
				else if (!TownyPermission.getUnclaimedZone(actionType, pos.getWorld())) {
					// TODO: Have permission to destroy here
					cacheBlockErrMsg(player, "Not allowed to " + actionType.toString() + " in the wild.");
					return false;
				} else
					return true;
			else {
				sendErrorMsg(player, "Error updating destroy permission.");
				return false;
			}
		}
		
		// Plot Permissions
		try {
			Resident owner = townBlock.getResident();
			
			if (status == TownBlockStatus.PLOT_FRIEND) {
				if (owner.getPermissions().getResident(actionType))
					return true;
				else {
					cacheBlockErrMsg(player, "Owner doesn't allow friends to " + actionType.toString() + " here.");
					return false;
				}
			} else if (status == TownBlockStatus.PLOT_ALLY)
				if (owner.getPermissions().getAlly(actionType))
					return true;
				else {
					cacheBlockErrMsg(player, "Owner doesn't allow allies to " + actionType.toString() + " here.");
					return false;
				}
			else if (status == TownBlockStatus.OUTSIDER)
				if (owner.getPermissions().getOutsider(actionType))
					return true;
				else {
					cacheBlockErrMsg(player, "Owner doesn't allow outsiders to " + actionType.toString() + " here.");
					return false;
				}
		} catch (NotRegisteredException x) {
		}
	
		// Town Permissions
		if (status == TownBlockStatus.TOWN_RESIDENT) {
			if (town.getPermissions().getResident(actionType))
				return true;
			else {
				cacheBlockErrMsg(player, "Residents aren't allowed to " + actionType.toString() + ".");
				return false;
			}
		} else if (status == TownBlockStatus.TOWN_ALLY)
			if (town.getPermissions().getAlly(actionType))
				return true;
			else {
				cacheBlockErrMsg(player, "Allies aren't allowed to " + actionType.toString() + " here.");
				return false;
			}
		else if (status == TownBlockStatus.OUTSIDER)
			if (town.getPermissions().getOutsider(actionType))
				return true;
			else {
				cacheBlockErrMsg(player, "Outsiders aren't allowed to " + actionType.toString() + " here.");
				return false;
			}
		
		sendErrorMsg(player, "Error updating " + actionType.toString() + " permission.");
		return false;
	}	

	public iConomy getIConomy() throws IConomyException {
		if (iconomy == null)
			throw new IConomyException("iConomy is not installed");
		else
			return iconomy;
		
	}
}
