package com.shade.bukkit.towny;

import java.util.ArrayList;
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
	
	public TownBlock getTownBlock(Coord coord) throws NotRegisteredException {
		TownBlock townBlock = townBlocks.get(coord);
		if (townBlock == null)
			throw new NotRegisteredException();
		else
			return townBlock; 
	}
	
	public void newTownBlock(int x, int z) throws AlreadyRegisteredException {
		if (townBlocks.containsKey(new Coord(x, z)))
			throw new AlreadyRegisteredException();
		
		townBlocks.put(new Coord(x, z), new TownBlock(x, z));
	}
	
	public TownBlock getTownBlock(int x, int z) throws NotRegisteredException {
		return getTownBlock(new Coord(x, z));
	}
}
