package com.shade.bukkit.towny;

import java.util.Collection;
import java.util.HashMap;
//import java.util.List;

public class TownyWorld extends TownyObject {
	private HashMap<String,Town> towns;
	private HashMap<String,Nation> nations;
	
	//private List<TownBlock> townBlocks;
	
	public Collection<Town> getTowns() {
		return towns.values();
	}
	
	public Collection<Nation> getNations() {
		return nations.values();
	}
}
