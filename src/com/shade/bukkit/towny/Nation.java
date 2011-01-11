package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class Nation extends TownyObject {
	private List<Resident> assistants = new ArrayList<Resident>();
	private List<Town> towns = new ArrayList<Town>();
	//private List<Nation> allies, enemies;
	private Town capital;
	
	public List<Town> getTowns() {
		return towns;
	}
	
	public boolean isKing(Resident resident) {
		return (hasCapital() ? capital.isMayor(resident) : false);
	}
	
	public boolean hasCapital() {
		return !(capital == null);
	}
	
	public boolean hasAssistant(Resident resident) {
		if (resident == null)
			return false;
		else
			return assistants.contains(resident);
	}
}
