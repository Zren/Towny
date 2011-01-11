package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class TownyWorld extends TownyObject {
	private List<Town> towns = new ArrayList<Town>();
	
	//private List<TownBlock> townBlocks;
	
	public List<Town> getTowns() {
		return towns;
	}
}
