package ca.xshade.bukkit.towny.object;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.DailyTimerTask;
import ca.xshade.bukkit.towny.EmptyNationException;
import ca.xshade.bukkit.towny.EmptyTownException;
import ca.xshade.bukkit.towny.HealthRegenTimerTask;
import ca.xshade.bukkit.towny.IConomyException;
import ca.xshade.bukkit.towny.MobRemovalTimerTask;
import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.TownyFormatter;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.db.TownyDataSource;
import ca.xshade.bukkit.towny.db.TownyFlatFileSource;
import ca.xshade.bukkit.towny.db.TownyHModFlatFileSource;
import ca.xshade.bukkit.towny.war.War;
import ca.xshade.bukkit.towny.war.WarSpoils;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;
import ca.xshade.bukkit.util.MinecraftTools;
import ca.xshade.util.FileMgmt;


public class TownyUniverse extends TownyObject {
	private Towny plugin;
	private Hashtable<String, Resident> residents = new Hashtable<String, Resident>();
	private Hashtable<String, Town> towns = new Hashtable<String, Town>();
	private Hashtable<String, Nation> nations = new Hashtable<String, Nation>();
	private Hashtable<String, TownyWorld> worlds = new Hashtable<String, TownyWorld>();
	// private List<Election> elections;
	private TownyFormatter formatter = new TownyFormatter(); //TODO : Make static
	private TownyDataSource dataSource;
	private int dailyTask = -1;
	private int mobRemoveTask = -1;
	private int healthRegenTask = -1;
	private War warEvent;
	private String rootFolder;
	
	public TownyUniverse() {
		setName("");
		rootFolder = "";
	}
	
	public TownyUniverse(String rootFolder) {
		setName("");
		this.rootFolder = rootFolder;
	}
	
	public TownyUniverse(Towny plugin) {
		setName("");
		this.plugin = plugin;
	}
	
	public void newDay() {
		if (!isDailyTimerRunning())
			toggleDailyTimer(true);
		//dailyTimer.schedule(new DailyTimerTask(this), 0);
		if (getPlugin().getServer().getScheduler().scheduleAsyncDelayedTask(getPlugin(), new DailyTimerTask(this)) == -1)
			plugin.sendErrorMsg("Could not schedule newDay.");
	}
	
	public void toggleMobRemoval(boolean on) {
		if (on && !isMobRemovalRunning()) {
			//mobRemoveTimer = new Timer();
			//mobRemoveTimer.scheduleAtFixedRate(new MobRemovalTimerTask(this, plugin.getServer()), 0, TownySettings.getMobRemovalSpeed());
			mobRemoveTask = getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(getPlugin(), new MobRemovalTimerTask(this, plugin.getServer()), 0, MinecraftTools.convertToTicks(TownySettings.getMobRemovalSpeed()));
			if (mobRemoveTask == -1)
				plugin.sendErrorMsg("Could not schedule mob removal loop.");
		} else if (!on && isMobRemovalRunning())
			//mobRemoveTimer.cancel();
			//mobRemoveTimer = null;
			getPlugin().getServer().getScheduler().cancelTask(mobRemoveTask);
	}
	
	public void toggleDailyTimer(boolean on) {
		if (on && !isDailyTimerRunning()) {
			//dailyTimer = new Timer();
			long timeTillNextDay = TownySettings.getDayInterval() - System.currentTimeMillis() % TownySettings.getDayInterval();
			//dailyTimer.scheduleAtFixedRate(new DailyTimerTask(this), timeTillNextDay, TownySettings.getDayInterval());
			dailyTask = getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(getPlugin(), new DailyTimerTask(this), MinecraftTools.convertToTicks(timeTillNextDay), MinecraftTools.convertToTicks(TownySettings.getDayInterval()));
			if (dailyTask == -1)
				plugin.sendErrorMsg("Could not schedule new day loop.");
		} else if (!on && isDailyTimerRunning())
			//dailyTimer.cancel();
			//dailyTimer = null;
			getPlugin().getServer().getScheduler().cancelTask(dailyTask);
	}
	
	public void toggleHealthRegen(boolean on) {
		if (on && !isHealthRegenRunning()) {
			//healthRegenTimer = new Timer();
			//healthRegenTimer.scheduleAtFixedRate(new HealthRegenTimerTask(this, plugin.getServer()), 0, TownySettings.getHealthRegenSpeed());
			dailyTask = getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(getPlugin(), new HealthRegenTimerTask(this, plugin.getServer()), 0, MinecraftTools.convertToTicks(TownySettings.getHealthRegenSpeed()));
			if (dailyTask == -1)
				plugin.sendErrorMsg("Could not schedule health regen loop.");
		} else if (!on && isHealthRegenRunning())
			getPlugin().getServer().getScheduler().cancelTask(healthRegenTask);
	}
	
	public boolean isMobRemovalRunning() {
		return mobRemoveTask != -1;
		//return mobRemoveTimer != null;
	}
	
	public boolean isDailyTimerRunning() {
		return dailyTask != -1;
		//return dailyTimer != null;
	}
	
	public boolean isHealthRegenRunning() {
		return healthRegenTask != -1;
		//return healthRegenTimer != null;
	}

	public void onLogin(Player player) throws AlreadyRegisteredException, NotRegisteredException {
		Resident resident;
		if (!hasResident(player.getName())) {
			newResident(player.getName());
			resident = getResident(player.getName());
			sendMessage(player, TownySettings.getRegistrationMsg());
			resident.setRegistered(System.currentTimeMillis());
			if (!TownySettings.getDefaultTownName().equals(""))
				try {
					getTown(TownySettings.getDefaultTownName()).addResident(resident);
				} catch (NotRegisteredException e) {
				} catch (AlreadyRegisteredException e) {
				}
			getDataSource().saveResidentList();
		} else
			resident = getResident(player.getName());

		resident.setLastOnline(System.currentTimeMillis());
		getDataSource().saveResident(resident);

		try {
			sendTownBoard(player, resident.getTown());
		} catch (NotRegisteredException e) {
		}

		if (isWarTime())
			getWarEvent().sendScores(player, 3);
	}

	public void onLogout(Player player) {
		try {
			Resident resident = getResident(player.getName());
			resident.setLastOnline(System.currentTimeMillis());
			getDataSource().saveResident(resident);
		} catch (NotRegisteredException e) {
		}
	}
	
	/**
	 * Teleports the player to his town's spawn location. If town doesn't have a
	 * spawn or player has no town, and teleport is forced, then player is sent
	 * to the world's spawn location.
	 * 
	 * @param player
	 * @param forceTeleport
	 */

	public void townSpawn(Player player, boolean forceTeleport) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			player.teleportTo(town.getSpawn());
		} catch (TownyException x) {
			if (forceTeleport) {
				player.teleportTo(player.getWorld().getSpawnLocation());
				plugin.sendDebugMsg("onTownSpawn: [forced] "+player.getName());
			} else
				plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	public Location getTownSpawnLocation(Player player, boolean forceTeleport) throws TownyException {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			return town.getSpawn();
		} catch (TownyException x) {
			if (forceTeleport)
				return player.getWorld().getSpawnLocation();
			else
				throw new TownyException("Unable to get spawn location");
		}
	}

	public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException {
		if (residents.containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException("The resident " + name + " is already in use.");

		residents.put(name.toLowerCase(), new Resident(name));
	}

	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {
		if (towns.containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException("The town " + name + " is already in use.");

		towns.put(name.toLowerCase(), new Town(name));
	}

	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {
		if (nations.containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException("The nation " + name + " is already in use.");

		nations.put(name.toLowerCase(), new Nation(name));
	}

	public void newWorld(String name) throws AlreadyRegisteredException {
		if (worlds.containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException("The world " + name + " is already in use.");

		worlds.put(name.toLowerCase(), new TownyWorld(name));
	}

	public boolean hasResident(String name) {
		return residents.containsKey(name.toLowerCase());
	}

	public boolean hasTown(String name) {
		return towns.containsKey(name.toLowerCase());
	}
	
	public boolean hasNation(String name) {
		return nations.containsKey(name.toLowerCase());
	}

	public void renameTown(Town town, String newName) throws AlreadyRegisteredException {
		if (hasTown(newName))
			throw new AlreadyRegisteredException("The town " + newName + " is already in use.");

		// TODO: Delete/rename any invites.

		String oldName = town.getName();
		towns.put(newName.toLowerCase(), town);
		towns.remove(oldName.toLowerCase());
		town.setName(newName);
		try {
			Town oldTown = new Town(oldName);
			oldTown.pay(oldTown.getIConomyBalance(), town);
		} catch (IConomyException e) {
		}
		getDataSource().saveTown(town);
		getDataSource().saveTownList();
	}
	
	public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException {
		if (hasNation(newName))
			throw new AlreadyRegisteredException("The nation " + newName + " is already in use.");

		// TODO: Delete/rename any invites.

		String oldName = nation.getName();
		nations.put(newName.toLowerCase(), nation);
		nations.remove(oldName.toLowerCase());
		nation.setName(newName);
		try {
			Nation oldNation = new Nation(oldName);
			oldNation.pay(oldNation.getIConomyBalance(), nation);
		} catch (IConomyException e) {
		}
		getDataSource().saveNation(nation);
		getDataSource().saveNationList();
	}

	public Resident getResident(String name) throws NotRegisteredException {
		Resident resident = residents.get(name.toLowerCase());
		if (resident == null)
			throw new NotRegisteredException();
		return resident;
	}

	public void sendMessage(Player player, List<String> lines) {
		sendMessage(player, lines.toArray(new String[0]));
	}

	public void sendTownMessage(Town town, List<String> lines) {
		sendTownMessage(town, lines.toArray(new String[0]));
	}

	public void sendNationMessage(Nation nation, List<String> lines) {
		sendNationMessage(nation, lines.toArray(new String[0]));
	}

	public void sendGlobalMessage(List<String> lines) {
		sendGlobalMessage(lines.toArray(new String[0]));
	}

	public void sendGlobalMessage(String line) {
		for (Player player : getOnlinePlayers())
			player.sendMessage(line);
	}
	
	public void sendMessage(Player player, String[] lines) {
		for (String line : lines)
			player.sendMessage(line);
	}

	public Player getPlayer(Resident resident) throws TownyException {
		for (Player player : getOnlinePlayers())
			if (player.getName().equals(resident.getName()))
				return player;
		throw new TownyException("Resident is not online");
	}

	public void sendResidentMessage(Resident resident, String[] lines)
			throws TownyException {
		Player player = getPlayer(resident);
		for (String line : lines)
			player.sendMessage(line);
	}

	public void sendTownMessage(Town town, String[] lines) {
		for (Player player : getOnlinePlayers(town))
			for (String line : lines)
				player.sendMessage(line);
	}

	public void sendNationMessage(Nation nation, String[] lines) {
		for (Player player : getOnlinePlayers(nation))
			for (String line : lines)
				player.sendMessage(line);
	}

	public void sendGlobalMessage(String[] lines) {
		for (Player player : getOnlinePlayers())
			for (String line : lines)
				player.sendMessage(line);
	}

	public void sendResidentMessage(Resident resident, String line) throws TownyException {
		Player player = getPlayer(resident);
		player.sendMessage(line);
	}

	public void sendTownMessage(Town town, String line) {
		for (Player player : getOnlinePlayers(town))
			player.sendMessage(line);
	}
	
	public void sendNationMessage(Nation nation, String line) {
		for (Player player : getOnlinePlayers(nation))
			player.sendMessage(line);
	}

	public void sendTownBoard(Player player, Town town) {
		for (String line : ChatTools.color(Colors.Gold + "[" + town.getName() + "] " + Colors.Yellow + town.getTownBoard()))
			player.sendMessage(line);
	}

	public Player[] getOnlinePlayers() {
		return plugin.getServer().getOnlinePlayers();
	}

	public List<Player> getOnlinePlayers(ResidentList residents) {
		ArrayList<Player> players = new ArrayList<Player>();
		for (Player player : getOnlinePlayers())
			if (residents.hasResident(player.getName()))
				players.add(player);
		return players;
	}
	
	public List<Player> getOnlinePlayers(Town town) {
		ArrayList<Player> players = new ArrayList<Player>();
		for (Player player : getOnlinePlayers())
			if (town.hasResident(player.getName()))
				players.add(player);
		return players;
	}

	public List<Player> getOnlinePlayers(Nation nation) {
		ArrayList<Player> players = new ArrayList<Player>();
		for (Town town : nation.getTowns())
			players.addAll(getOnlinePlayers(town));
		return players;
	}

	public List<Resident> getResidents() {
		return new ArrayList<Resident>(residents.values());
	}

	public Set<String> getResidentKeys() {
		return residents.keySet();
	}

	public List<Town> getTowns() {
		return new ArrayList<Town>(towns.values());
	}

	public List<Nation> getNations() {
		return new ArrayList<Nation>(nations.values());
	}

	public List<TownyWorld> getWorlds() {
		return new ArrayList<TownyWorld>(worlds.values());
	}
	
	public List<Town> getTownsWithoutNation() {
		List<Town> townFilter = new ArrayList<Town>();
		for (Town town : getTowns())
			if (!town.hasNation())
				townFilter.add(town);
		return townFilter;
	}
	
	public List<Resident> getResidentsWithoutTown() {
		List<Resident> residentFilter = new ArrayList<Resident>();
		for (Resident resident : getResidents())
			if (!resident.hasTown())
				residentFilter.add(resident);
		return residentFilter;
	}

	public List<Resident> getActiveResidents() {
		List<Resident> activeResidents = new ArrayList<Resident>();
		for (Resident resident : getResidents())
			if (isActiveResident(resident))
				activeResidents.add(resident);
		return activeResidents;
	}

	public boolean isActiveResident(Resident resident) {
		return System.currentTimeMillis() - resident.getLastOnline() < TownySettings.getInactiveAfter();
	}
	
	public List<Resident> getResidents(String[] names) {
		List<Resident> matches = new ArrayList<Resident>();
		for (String name : names)
			try {
				matches.add(getResident(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}
	
	public List<Town> getTowns(String[] names) {
		List<Town> matches = new ArrayList<Town>();
		for (String name : names)
			try {
				matches.add(getTown(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}
	
	public List<Nation> getNations(String[] names) {
		List<Nation> matches = new ArrayList<Nation>();
		for (String name : names)
			try {
				matches.add(getNation(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}
	
	public List<String> getStatus(Resident resident) {
		return getFormatter().getStatus(resident);
	}

	public List<String> getStatus(Town town) {
		return getFormatter().getStatus(town);
	}

	public List<String> getStatus(Nation nation) {
		return getFormatter().getStatus(nation);
	}
	
	public List<String> getStatus(TownyWorld world) {
		return getFormatter().getStatus(world);
	}

	public Town getTown(String name) throws NotRegisteredException {
		Town town = towns.get(name.toLowerCase());
		if (town == null)
			throw new NotRegisteredException();
		return town;
	}

	public Nation getNation(String name) throws NotRegisteredException {
		Nation nation = nations.get(name.toLowerCase());
		if (nation == null)
			throw new NotRegisteredException();
		return nation;
	}
	
	public String getRootFolder() {
		if (plugin != null)
			return plugin.getDataFolder().getPath();
		else
			return rootFolder;
	}

	public boolean loadSettings() {
		try {
			FileMgmt.checkFolders(new String[]{getRootFolder(), getRootFolder() + "/settings"});
			FileMgmt.checkFiles(new String[]{
					getRootFolder() + "/settings/config.properties",
					getRootFolder() + "/settings/town-levels.csv",
					getRootFolder() + "/settings/nation-levels.csv"});
			TownySettings.loadConfig(getRootFolder() + "/settings/config.properties");
			TownySettings.loadTownLevelConfig(getRootFolder() + "/settings/town-levels.csv");
			TownySettings.loadNationLevelConfig(getRootFolder() + "/settings/nation-levels.csv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return true;
	}

	public boolean loadDatabase(String databaseType) {
		try {
			setDataSource(databaseType);
		} catch (UnsupportedOperationException e) {
			return false;
		}

		getDataSource().initialize(plugin, this);
		return getDataSource().loadAll();
	}

	public TownyWorld getWorld(String name) throws NotRegisteredException {
		TownyWorld world = worlds.get(name.toLowerCase());
		if (world == null) {
			try {
				newWorld(name);
			} catch (AlreadyRegisteredException e) {
				throw new NotRegisteredException("Not registered, but already registered when trying to register.");
			}
			world = worlds.get(name.toLowerCase());
			if (world == null)
				throw new NotRegisteredException();
		}
		return world;
	}
	
	public boolean isAlly(String a, String b) {
		try {
			Resident residentA = getResident(a);
			Resident residentB = getResident(b);
			if (residentA.getTown() == residentB.getTown())
				return true;
			if (residentA.getTown().getNation() == residentB.getTown().getNation())
				return true;
			if (residentA.getTown().getNation().hasAlly(residentB.getTown().getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	public boolean isAlly(Town a, Town b) {
		try {
			if (a == b)
				return true;
			if (a.getNation() == b.getNation())
				return true;
			if (a.getNation().hasAlly(b.getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	public void setDataSource(String databaseType) throws UnsupportedOperationException {
		if (databaseType.equalsIgnoreCase("flatfile"))
			setDataSource(new TownyFlatFileSource());
		else if (databaseType.equalsIgnoreCase("flatfile-hmod"))
			setDataSource(new TownyHModFlatFileSource());
		else
			throw new UnsupportedOperationException();
	}
	
	public void setDataSource(TownyDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public TownyDataSource getDataSource() {
		return dataSource;
	}

	public void setFormatter(TownyFormatter formatter) {
		this.formatter = formatter;
	}

	public TownyFormatter getFormatter() {
		return formatter;
	}

	public boolean isWarTime() {
		return warEvent != null ? warEvent.isWarTime() : false;
	}

	public void collectNationTaxes() throws IConomyException {
		for (Nation nation : nations.values())
			collectNationTaxe(nation);
	}

	public void collectNationTaxe(Nation nation) throws IConomyException {
		for (Town town : nation.getTowns()) {
			if (town.isCapital())
				continue;
			if (!town.pay(nation.getTaxes(), nation)) {
				try {
					sendNationMessage(nation, TownySettings.getCouldntPayTaxesMsg(town, ", and was kicked from the nation."));
					nation.removeTown(town);
				} catch (EmptyNationException e) {
					// Always has 1 town (capital) so ignore
				} catch (NotRegisteredException e) {
				}
				getDataSource().saveTown(town);
				getDataSource().saveNation(nation);
			} else
				sendTownMessage(town, "Payed town tax of " + nation.getTaxes());
		}
	}

	public void collectTownTaxes() throws IConomyException {
		for (Town town : towns.values())
			collectTownTaxe(town);
	}

	public void collectTownTaxe(Town town) throws IConomyException {
		//Resident Tax
		for (Resident resident : town.getResidents())
			if (town.isMayor(resident) || town.hasAssistant(resident)) {
				try {
					sendResidentMessage(resident, "Town staff are exempt from taxes.");
				} catch (TownyException e) {
				}
				continue;
			} else if (!resident.pay(town.getTaxes(), town)) {
				sendTownMessage(town, TownySettings.getCouldntPayTaxesMsg(resident, ", and was kicked from town."));
				try {
					town.removeResident(resident);
				} catch (NotRegisteredException e) {
				} catch (EmptyTownException e) {
				}
				getDataSource().saveResident(resident);
				getDataSource().saveTown(town);
			} else
				try {
					sendResidentMessage(resident, "Payed resident tax of " + town.getTaxes());
				} catch (TownyException e1) {
				}
				
		
		//Plot Tax
		Hashtable<Resident,Integer> townPlots = new Hashtable<Resident,Integer>();
		for (TownBlock townBlock : town.getTownBlocks()) {
			if (!townBlock.hasResident())
				continue;
			try {
				Resident resident = townBlock.getResident();
				if (town.isMayor(resident) || town.hasAssistant(resident))
					continue;
				if (!resident.pay(town.getPlotTax(), town)) {
					sendTownMessage(town, TownySettings.getCouldntPayTaxesMsg(resident, ", and lost ownership over a plot."));
					townBlock.setResident(null);
					getDataSource().saveResident(resident);
					getDataSource().saveWorld(townBlock.getWorld());
				} else
					townPlots.put(resident, (townPlots.containsKey(resident) ? townPlots.get(resident) : 0) + 1);
			} catch (NotRegisteredException e) {
			}
		}
		for (Resident resident : townPlots.keySet())
			try {
				int numPlots = townPlots.get(resident);
				int totalCost = town.getPlotTax() * numPlots;
				sendResidentMessage(resident, "Payed " + totalCost + " for " + numPlots + " plots in " + town.getName());
			} catch (TownyException e) {
			}
	}

	public void startWarEvent() {
		this.warEvent = new War(plugin, TownySettings.getWarTimeWarningDelay());
	}
	
	public void endWarEvent() {
		if (isWarTime())
			warEvent.toggleEnd();
		// Automatically makes warEvent null
	}
	
	public void clearWarEvent() {
		getWarEvent().cancleTasks(getPlugin().getServer().getScheduler());
		setWarEvent(null);
	}
	
	//TODO: throw error if null
	public War getWarEvent() {
		return warEvent;
	}

	public void setWarEvent(War warEvent) {
		this.warEvent = warEvent;
	}
	
	public Towny getPlugin() {
		return plugin;
	}

	public void setPlugin(Towny plugin) {
		this.plugin = plugin;
	}

	public void removeNation(Nation nation) {
		List<Town> toSave = new ArrayList<Town>(nation.getTowns());
		nation.clear();
		try {
			nation.pay(nation.getIConomyBalance(), new WarSpoils());
		} catch (IConomyException e) {
		}
		nations.remove(nation.getName().toLowerCase());
		plugin.updateCache();
		for (Town town : toSave)
			getDataSource().saveTown(town);
		getDataSource().saveNationList();
	}

	public void removeTown(Town town) {
		List<Resident> toSave = new ArrayList<Resident>(town.getResidents());
		try {
			town.clear();
		} catch (EmptyNationException e) {
			removeNation(e.getNation());
		}
		try {
			town.pay(town.getIConomyBalance(), new WarSpoils());
		} catch (IConomyException e) {
		}
		towns.remove(town.getName().toLowerCase());
		plugin.updateCache();
		for (Resident resident : toSave)
			getDataSource().saveResident(resident);
		getDataSource().saveTownList();
	}

	public void removeResident(Resident resident) {
		try {
			resident.clear();
		} catch (EmptyTownException e) {
			removeTown(e.getTown());
		}
		String name = resident.getName();
		residents.remove(name.toLowerCase());
		plugin.deleteCache(name);
		getDataSource().saveResidentList();
	}
	
	public void sendUniverseTree(CommandSender sender) {
		for (String line : getTreeString(0))
			sender.sendMessage(line);
	}

	public void removeTownBlock(TownBlock townBlock) {
		Resident resident = null;
		Town town = null;
		try {
			resident = townBlock.getResident();
		} catch (NotRegisteredException e) {
		}
		try {
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
		}
		TownyWorld world = townBlock.getWorld();
		world.removeTownBlock(townBlock);
		getDataSource().saveWorld(world);
		if (resident != null)
			getDataSource().saveResident(resident);
		if (town != null)
			getDataSource().saveTown(town);
	}
	
	public void removeTownBlocks(Town town) {
		for (TownBlock townBlock : town.getTownBlocks())
			removeTownBlock(townBlock);
	}

	public void collectTownCosts() throws IConomyException {
		for (Town town : towns.values())
			if (!town.pay(TownySettings.getTownUpkeepCost())) {
				removeTown(town);
				sendGlobalMessage(town.getName() + " couldn't afford to remain a town.");
			}
	}
	
	public void collectNationCosts() throws IConomyException {
		for (Nation nation : nations.values()) {
			if (!nation.pay(TownySettings.getNationUpkeepCost())) {
				removeNation(nation);
				sendGlobalMessage(nation.getName() + " couldn't afford to remain a nation.");
			}
			if (nation.isNeutral())
				if (!nation.pay(TownySettings.getNationNeutralityCost())) {
					nation.setNeutral(false);
					getDataSource().saveNation(nation);
					sendNationMessage(nation, "Nation couldn't afford it's neutral state.");
				}
		}
		
	}
	
	public List<TownBlock> getAllTownBlocks() {
		List<TownBlock> townBlocks = new ArrayList<TownBlock>();
		for (TownyWorld world : getWorlds())
			townBlocks.addAll(world.getTownBlocks());
		return townBlocks;
	}
	
	@Override
	public List<String> getTreeString(int depth) {
		List<String> out = new ArrayList<String>();
		out.add(getTreeDepth(depth) + "Universe ("+getName()+")");
		if (plugin != null) {
			out.add(getTreeDepth(depth+1) + "Server ("+plugin.getServer().getName()+")");
			out.add(getTreeDepth(depth+2) + "Version: " + plugin.getServer().getVersion());
			out.add(getTreeDepth(depth+2) + "Players: " + plugin.getServer().getOnlinePlayers().length + "/" + plugin.getServer().getMaxPlayers());
			out.add(getTreeDepth(depth+2) + "Worlds (" + plugin.getServer().getWorlds().size() + "): " + Arrays.toString(plugin.getServer().getWorlds().toArray(new World[0])));
		}
		out.add(getTreeDepth(depth+1) + "Worlds (" + getWorlds().size() + "):");
		for (TownyWorld world : getWorlds())
			out.addAll(world.getTreeString(depth+2));
		
		out.add(getTreeDepth(depth+1) + "Nations (" + getNations().size() + "):");
		for (Nation nation : getNations())
			out.addAll(nation.getTreeString(depth+2));
		
		Collection<Town> townsWithoutNation = getTownsWithoutNation();
		out.add(getTreeDepth(depth+1) + "Towns (" + townsWithoutNation.size() + "):");
		for (Town town : townsWithoutNation)
			out.addAll(town.getTreeString(depth+2));
		
		Collection<Resident> residentsWithoutTown = getResidentsWithoutTown();
		out.add(getTreeDepth(depth+1) + "Residents (" + residentsWithoutTown.size() + "):");
		for (Resident resident : residentsWithoutTown)
			out.addAll(resident.getTreeString(depth+2));
		return out;
	}

	public boolean areAllAllies(List<Nation> possibleAllies) {
		if (possibleAllies.size() <= 1)
			return true;
		else {
			for (int i = 0; i < possibleAllies.size() - 1; i++)
				if (!possibleAllies.get(i).hasAlly(possibleAllies.get(i+1)))
					return false;
			return true;
		}
	}

	public void sendMessageTo(ResidentList residents, String msg, String modeRequired) {
		for (Player player : getOnlinePlayers(residents))
			if (plugin.hasPlayerMode(player, modeRequired))
				player.sendMessage(msg);
	}
	
	public List<Resident> getOnlineResidents(Player player, String[] names) {
		List<Resident> invited = new ArrayList<Resident>();
		for (String name : names) {
			List<Player> matches = plugin.getServer().matchPlayer(name);
			if (matches.size() > 1) {
				String line = "Multiple players selected";
				for (Player p : matches)
					line += ", " + p.getName();
				plugin.sendErrorMsg(player, line);
			} else if (matches.size() == 1)
				try {
					Resident target = plugin.getTownyUniverse().getResident(matches.get(0).getName());
					invited.add(target);
				} catch (TownyException x) {
					plugin.sendErrorMsg(player, x.getError());
				}
		}
		return invited;
	}
}
