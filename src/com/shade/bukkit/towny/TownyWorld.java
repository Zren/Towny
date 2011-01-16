package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TownyWorld extends TownyObject {
	private List<Town> towns = new ArrayList<Town>();
	
	private HashMap<Coord,TownBlock> townBlocks = new HashMap<Coord,TownBlock>();
	
	public TownyWorld(String name) {
		setName(name);
	}

	public List<Town> getTowns() {
		return towns;
	}
	
	public boolean hasTown(String name) {
		for (Town town : towns)
			if (town.getName().equalsIgnoreCase(name))
				return true;
		return false;
	}
	
	public boolean hasTown(Town town) {
		return towns.contains(town);
	}
	
	public void addTown(Town town) throws AlreadyRegisteredException {
		if (hasTown(town)) {
			throw new AlreadyRegisteredException();
		} else {
			towns.add(town);
			town.setWorld(this);
		}
	}
	
	public TownBlock getTownBlock(Coord coord) throws NotRegisteredException {
		TownBlock townBlock = townBlocks.get(coord);
		if (townBlock == null)
			throw new NotRegisteredException();
		else
			return townBlock; 
	}
	
	public void newTownBlock(int x, int z) throws AlreadyRegisteredException {
		newTownBlock(new Coord(x, z));
	}
	
	public void newTownBlock(Coord key) throws AlreadyRegisteredException {
		if (hasTownBlock(key))
			throw new AlreadyRegisteredException();
		
		townBlocks.put(key, new TownBlock(key.getX(), key.getZ(), this));
	}
	
	public boolean hasTownBlock(Coord key) {
		return townBlocks.containsKey(key);
	}
	
	public TownBlock getTownBlock(int x, int z) throws NotRegisteredException {
		return getTownBlock(new Coord(x, z));
	}

	public Collection<TownBlock> getTownBlocks() {
		return townBlocks.values();
	}
}
