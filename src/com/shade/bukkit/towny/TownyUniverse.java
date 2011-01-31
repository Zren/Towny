package com.shade.bukkit.towny;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import org.bukkit.entity.Player;

import com.shade.bukkit.towny.db.TownyDataSource;
import com.shade.bukkit.towny.db.TownyFlatFileSource;
import com.shade.bukkit.towny.war.War;
import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;
import com.shade.util.FileMgmt;

public class TownyUniverse extends TownyObject {
	private Towny plugin;
	private Hashtable<String, Resident> residents = new Hashtable<String, Resident>();
	private Hashtable<String, Town> towns = new Hashtable<String, Town>();
	private Hashtable<String, Nation> nations = new Hashtable<String, Nation>();
	private Hashtable<String, TownyWorld> worlds = new Hashtable<String, TownyWorld>();
	// private List<Election> elections;
	private TownyFormatter formatter = new TownyFormatter(); //TODO : MAke static
	private TownyDataSource dataSource;
	private Timer dailyTimer = null;
	private Timer mobRemoveTimer = null;
	private Timer healthRegenTimer = null;
	private War warEvent;
	private final int oneDay = 24 * 60 * 60 * 1000;
	private String rootFolder;
	
	
	// TODO: lastOnline, an onEnable check to see if a new day has stated to
	// collect taxes.
	// TODO: Timer to start/stop war time, collect taxes, delete old users.
	// TODO: Timer/Thread like Minigames that turns on during war time. Checks
	// each player every second.
	
	public TownyUniverse() {
		rootFolder = "";
	}
	
	public TownyUniverse(String rootFolder) {
		this.rootFolder = rootFolder;
	}
	
	public TownyUniverse(Towny plugin) {
		this.plugin = plugin;
	}
	
	public void newDay() {
		if (dailyTimer == null)
			toggleDailyTimer(true);
		dailyTimer.schedule(new DailyTimerTask(this), 0);
	}
	
	public void toggleMobRemoval(boolean on) {
		if (on && mobRemoveTimer == null) {
			mobRemoveTimer = new Timer();
			mobRemoveTimer.scheduleAtFixedRate(new MobRemovalTimerTask(this, plugin.getServer()), 0, TownySettings.getMobRemovalSpeed());
		} else if (!on && mobRemoveTimer != null) {
			mobRemoveTimer.cancel();
			mobRemoveTimer = null;
		}
	}
	
	public void toggleDailyTimer(boolean on) {
		if (on && dailyTimer == null) {
			dailyTimer = new Timer();
			Calendar tomorrow = new GregorianCalendar();
			tomorrow.add(Calendar.DATE, oneDay);
			Calendar result = new GregorianCalendar(tomorrow.get(Calendar.YEAR), tomorrow.get(Calendar.MONTH), tomorrow.get(Calendar.DATE), 0, 0);
			dailyTimer.scheduleAtFixedRate(new DailyTimerTask(this), result.getTime(), oneDay);
		} else if (!on && dailyTimer != null) {
			dailyTimer.cancel();
			dailyTimer = null;
		}
	}
	
	public void toggleHealthRegen(boolean on) {
		if (on && healthRegenTimer == null) {
			healthRegenTimer = new Timer();
			healthRegenTimer.scheduleAtFixedRate(new HealthRegenTimerTask(this, plugin.getServer()), 0, TownySettings.getHealthRegenSpeed());
		} else if (!on && healthRegenTimer != null) {
			healthRegenTimer.cancel();
			healthRegenTimer = null;
		}
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
			if (forceTeleport)
				player.teleportTo(player.getWorld().getSpawnLocation());
			else
				plugin.sendErrorMsg(player, x.getError());
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

	public void sendResidentMessage(Resident resident, String line)
			throws TownyException {
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
		for (String line : ChatTools.color(Colors.Gold + "[" + town.getName()
				+ "] " + Colors.Yellow + town.getTownBoard()))
			player.sendMessage(line);
	}

	public Player[] getOnlinePlayers() {
		return plugin.getServer().getOnlinePlayers();
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

	public Collection<Resident> getResidents() {
		return residents.values();
	}

	public Set<String> getResidentKeys() {
		return residents.keySet();
	}

	public Collection<Town> getTowns() {
		return towns.values();
	}

	public Collection<Nation> getNations() {
		return nations.values();
	}

	public Collection<TownyWorld> getWorlds() {
		return worlds.values();
	}

	public Collection<Resident> getActiveResidents() {
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

	public boolean loadDatabase() {
		if (TownySettings.getLoadDatabase().equalsIgnoreCase("flatfile"))
			setDataSource(new TownyFlatFileSource());
		else
			return false;

		getDataSource().initialize(plugin, this);
		getDataSource().loadAll();
		return true;
	}

	public TownyWorld getWorld(String name) throws NotRegisteredException {
		TownyWorld world = worlds.get(name.toLowerCase());
		if (world == null)
			throw new NotRegisteredException();
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
					nation.removeTown(town);
				} catch (EmptyNationException e) {
					// Always has 1 town (capital) so ignore
				} catch (NotRegisteredException e) {
				}
				getDataSource().saveTown(town);
				getDataSource().saveNation(nation);
			}
		}
	}

	public void collectTownTaxes() throws IConomyException {
		for (Town town : towns.values())
			collectTownTaxe(town);
	}

	public void collectTownTaxe(Town town) throws IConomyException {
		//Resident Tax
		for (Resident resident : town.getResidents())
			if (!town.hasAssistant(resident) || !town.isMayor(resident))
				if (!resident.pay(town.getTaxes(), town)) {
					sendTownMessage(town, TownySettings.getCouldntPayTaxesMsg(resident, ", and was kicked from town."));
					try {
						town.removeResident(resident);
					} catch (NotRegisteredException e) {
					} catch (EmptyTownException e) {
					}
					getDataSource().saveResident(resident);
					getDataSource().saveTown(town);
				}
		
		//Plot Tax
		for (TownBlock townBlock : town.getTownBlocks()) {
			if (!townBlock.hasResident())
				continue;
			try {
				Resident resident = townBlock.getResident();
				if (!town.hasAssistant(resident) || !town.isMayor(resident))
					if (!resident.pay(town.getPlotTax(), town)) {
						sendTownMessage(town, TownySettings.getCouldntPayTaxesMsg(resident, ", and lost ownership over a plot."));
						townBlock.setResident(null);
						getDataSource().saveResident(resident);
						getDataSource().saveWorld(townBlock.getWorld());
					}
			} catch (NotRegisteredException e) {
			}
		}
	}

	public void startWarEvent() {
		this.warEvent = new War(plugin, TownySettings.getWarTimeWarningDelay());
	}
	
	public void endWarEvent() {
		if (isWarTime())
			warEvent.end();
		// Automatically makes warEvent null
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
		nations.remove(nation.getName().toLowerCase());
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
		towns.remove(town.getName().toLowerCase());
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
		residents.remove(resident.getName().toLowerCase());
		getDataSource().saveResidentList();
	}
	
	public void printUniverseTree() {
		System.out.println("|-Universe");
		for (TownyWorld world : getWorlds()) {
			System.out.println("|---World: " + world.getName());
			for (TownBlock townBlock : world.getTownBlocks())
				try {
					System.out.println("|------TownBlock: " + townBlock.getX() + "," + townBlock.getZ() + " "
							+ "Town: " + (townBlock.hasTown() ? townBlock.getTown().getName() : "None") + " : "
							+ "Owner: " + (townBlock.hasResident() ? townBlock.getResident().getName() : "None"));
				} catch (TownyException e) {
				}
			for (Resident resident : getResidents()) {
				try {
					System.out.println("|---Resident: " + resident.getName()
							+ " " + (resident.hasTown() ? resident.getTown().getName() : "")
							+ (resident.hasNation() ? resident.getTown().getNation().getName() : ""));
				} catch (TownyException e) {
				}
				for (TownBlock townBlock : resident.getTownBlocks())
					try {
						System.out.println("|------TownBlock: " + townBlock.getX() + "," + townBlock.getZ() + " "
								+ "Town: " + (townBlock.hasTown() ? townBlock.getTown().getName() : "None") + " : "
								+ "Owner: " + (townBlock.hasResident() ? townBlock.getResident().getName() : "None"));
					} catch (TownyException e) {
					}
			}
			for (Town town : getTowns()) {
				try {
					System.out.println("|---Town: " + town.getName() + " " + (town.hasNation() ? town.getNation().getName() : ""));
				} catch (TownyException e) {
				}
				for (TownBlock townBlock : town.getTownBlocks())
					try {
						System.out.println("|------TownBlock: "  + "," + townBlock.getZ() + " "
								+ "Town: " + (townBlock.hasTown() ? townBlock.getTown().getName() : "None") + " : "
								+ "Owner: " + (townBlock.hasResident() ? townBlock.getResident().getName() : "None"));
					} catch (TownyException e) {
					}
			}
		}
	}
}
