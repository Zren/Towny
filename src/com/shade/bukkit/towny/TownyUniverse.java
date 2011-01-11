package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Player;

public class TownyUniverse extends TownyObject {
	private Towny plugin;
	private HashMap<String,Resident> residents;
	private HashMap<String,TownyWorld> worlds;
	//private List<Election> elections;
	private TownySettings settings = new TownySettings();
	
	
	public TownyUniverse(Towny plugin) {
		this.plugin = plugin;
	}
	
	public void onLogin(Player player) throws AlreadyRegisteredException, NotRegisteredException {
		if (!isRegisteredResident(player.getName())) {
			registerResident(player.getName());
			sendMessage(player, settings.getRegistrationMsg());
			if (settings.getDefaultTown() != null)
				sendMessage(player, settings.getRegistrationMsg());
				
		}
	}
	
	public void onLogout(Player player) {
		
	}
	
	public void registerResident(String name) throws AlreadyRegisteredException, NotRegisteredException {
		if (residents.containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException();
		Resident resident = getResident(name);
		if (settings.getDefaultTown() != null)
			settings.getDefaultTown().addResident(resident);
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
		List<Town> towns = new ArrayList<Town>();
		for (TownyWorld world : worlds.values()) {
			towns.addAll(world.getTowns());
		}
		return towns;
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
}
