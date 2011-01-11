package com.shade.bukkit.towny;

import java.util.Collection;
import java.util.HashMap;
//import java.util.List;

public class TownyWorld extends TownyObject {
	private HashMap<String,Town> towns = new HashMap<String,Town>();
	private HashMap<String,Nation> nations = new HashMap<String,Nation>();
	
	//private List<TownBlock> townBlocks;
	
	public Collection<Town> getTowns() {
		return towns.values();
	}
	
	public Collection<Nation> getNations() {
		return nations.values();
	}
}
