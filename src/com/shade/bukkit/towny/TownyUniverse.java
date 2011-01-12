package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Player;

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
	
	
	public TownyUniverse(Towny plugin) {
		this.plugin = plugin;
	}
	
	public void onLogin(Player player) throws AlreadyRegisteredException, NotRegisteredException {
		Resident resident;
		if (!isRegisteredResident(player.getName())) {
			newResident(player.getName());
			resident = getResident(player.getName());
			if (settings.getDefaultTown() != null)
				settings.getDefaultTown().addResident(resident);
			sendMessage(player, settings.getRegistrationMsg());
			if (settings.getDefaultTown() != null)
				sendMessage(player, settings.getRegistrationMsg());
			dataSource.saveResidentList();
		} else {
			resident = getResident(player.getName());
		}
		
		resident.setLastOnline(System.currentTimeMillis());
		dataSource.saveResident(resident);
	}
	
	public void onLogout(Player player) {
		
	}
	
	public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException {
		if (residents.containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException();
		
		residents.put(name.toLowerCase(), new Resident(name));
	}
	
	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {
		if (towns.containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException();
		
		towns.put(name.toLowerCase(), new Town(name));
	}
	
	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {
		if (nations.containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException();
		
		nations.put(name.toLowerCase(), new Nation(name));
	}
	
	public void newWorld(String name) throws AlreadyRegisteredException {
		if (worlds.containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException();
		
		worlds.put(name.toLowerCase(), new TownyWorld(name));
	}
	
	public boolean isRegisteredResident(String name) {
		return residents.containsKey(name.toLowerCase());
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
		return formatter.getStatus(resident);
	}
	
	public List<String> getStatus(Town town) {
		return formatter.getStatus(town);
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
			dataSource = new TownyFlatFileSource(); 
		else
			return false;
		
		dataSource.initialize(plugin, this,	settings);
		dataSource.loadAll();
		return true;
	}
}
