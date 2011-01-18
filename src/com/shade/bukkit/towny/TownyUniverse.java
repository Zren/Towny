package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;

import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;

public class TownyUniverse extends TownyObject {
	private Towny plugin;
	private HashMap<String,Resident> residents = new HashMap<String,Resident>();
	private HashMap<String,Town> towns = new HashMap<String,Town>();
	private HashMap<String,Nation> nations = new HashMap<String,Nation>();
	private HashMap<String,TownyWorld> worlds = new HashMap<String,TownyWorld>();
	//private List<Election> elections;
	private TownySettings settings = new TownySettings();
	private TownyFormatter formatter = new TownyFormatter(settings);
	private TownyDataSource dataSource;
	private boolean warTime = false;
	
	//TODO: lastOnline, an onEnable check to see if a new day has stated to collect taxes.
	//TODO: Timer to start/stop war time, collect taxes, delete old users.
	//TODO: Timer/Thread like Minigames that turns on during war time. Checks each player every second.
	
	
	public TownyUniverse(Towny plugin) {
		this.plugin = plugin;
	}
	
	public void onLogin(Player player) throws AlreadyRegisteredException, NotRegisteredException {
		Resident resident;
		if (!hasResident(player.getName())) {
			newResident(player.getName());
			resident = getResident(player.getName());
			if (settings.getDefaultTown() != null)
				settings.getDefaultTown().addResident(resident);
			sendMessage(player, settings.getRegistrationMsg());
			if (settings.getDefaultTown() != null)
				sendMessage(player, settings.getRegistrationMsg());
			getDataSource().saveResidentList();
		} else {
			resident = getResident(player.getName());
		}
		
		resident.setLastOnline(System.currentTimeMillis());
		getDataSource().saveResident(resident);
	}
	
	public void onLogout(Player player) {
		//TODO: Proceduralize updating last online
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
		return residents.containsKey(name.toLowerCase());
	}
	
	public void renameTown(Town town, String newName) throws AlreadyRegisteredException {
		if (hasTown(newName))
			throw new AlreadyRegisteredException("The town " + newName + " is already in use.");
		
		//TODO: Delete/rename any invites.
		
		String oldName = town.getName();
		towns.put(newName.toLowerCase(), town);
		towns.remove(oldName.toLowerCase());
		town.setName(newName);
		getDataSource().saveTown(town);
		getDataSource().saveTownList();
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
	
	public void sendTownMessage(Town town, String line) {
		for (Player player : getOnlinePlayers(town))
			player.sendMessage(line);
	}
	
	public void sendTownBoard(Player player, Town town) {
		for (String line : ChatTools.color(Colors.Gold + "[" + town.getName() + "] " + town.getTownBoard()))
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
		for (Resident resident : getResidents()) {
			if (isActiveResident(resident))
				activeResidents.add(resident);
		}
		return activeResidents;
	}
	
	public boolean isActiveResident(Resident resident) {
		return (System.currentTimeMillis() - resident.getLastOnline() < settings.getInactiveAfter());
	}
	
	public TownySettings getSettings() {
		return settings;
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

	public boolean loadSettings() {
		// TODO Load settings from file
		
		return true;
	}
	
	public boolean loadDatabase() {
		if (settings.getLoadDatabase().equalsIgnoreCase("flatfile"))
			setDataSource(new TownyFlatFileSource()); 
		else
			return false;
		
		getDataSource().initialize(plugin, this, settings);
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
			if (residentA.getTown() == residentB.getTown()) return true;
			if (residentA.getTown().getNation() == residentB.getTown().getNation()) return true;
			if (residentA.getTown().getNation().hasAlly(residentB.getTown().getNation())) return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}
	
	public boolean isAlly(Town a, Town b) {
		try {
			if (a == b) return true;
			if (a.getNation() == b.getNation()) return true;
			if (a.getNation().hasAlly(b.getNation())) return true;
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
		return warTime ;
	}
}
