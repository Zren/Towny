package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class Nation extends TownyObject {
	private List<Resident> assistants = new ArrayList<Resident>();
	private List<Town> towns = new ArrayList<Town>();
	private List<Nation> allies  = new ArrayList<Nation>();
	private List<Nation> enemies  = new ArrayList<Nation>();
	private Town capital;
	
	public Nation(String name) {
		setName(name);
	}
	
	public void addAlly(Nation nation) throws AlreadyRegisteredException {
		if (hasAlly(nation)) {
			throw new AlreadyRegisteredException();
		} else {
			getAllies().add(nation);
		}
	}
	
	public boolean removeAlly(Nation nation) {
		return getAllies().remove(nation);
	}
	
	public boolean hasAlly(Nation nation) {
		return getAllies().contains(nation);
	}
	
	public void addEnemy(Nation nation) throws AlreadyRegisteredException {
		if (hasEnemy(nation)) {
			throw new AlreadyRegisteredException();
		} else {
			getEnemies().add(nation);
		}
	}
	
	public boolean removeEnemy(Nation nation) {
		return getEnemies().remove(nation);
	}
	
	public boolean hasEnemy(Nation nation) {
		return getEnemies().contains(nation);
	}
	
	public List<Town> getTowns() {
		return towns;
	}
	
	public boolean isKing(Resident resident) {
		return (hasCapital() ? getCapital().isMayor(resident) : false);
	}
	
	public boolean hasCapital() {
		return !(getCapital() == null);
	}
	
	public boolean hasAssistant(Resident resident) {
		return getAssistants().contains(resident);
	}
	
	public boolean isCapital(Town town) {
		return (town == getCapital());
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
			town.setNation(this);
		}
	}
	
	public void addAssistant(Resident resident) throws AlreadyRegisteredException {
		if (hasAssistant(resident)) {
			throw new AlreadyRegisteredException();
		} else {
			getAssistants().add(resident);
		}
	}

	public void setCapital(Town capital) {
		this.capital = capital;
	}

	public Town getCapital() {
		return capital;
	}
	
	public boolean setAliegeance(String type, Nation nation) {
		try {
	        if (type.equalsIgnoreCase("ally")) {
	            removeEnemy(nation);
	            addAlly(nation);
	            if (!hasEnemy(nation) && hasAlly(nation))
	                return true;
	        } else if (type.equalsIgnoreCase("neutral")) {
	        	removeEnemy(nation);
	            removeAlly(nation);
	            if (!hasEnemy(nation) && !hasAlly(nation))
	                return true;
	        } else if (type.equalsIgnoreCase("enemy")) {
	        	removeAlly(nation);
	            addEnemy(nation);
	            if (hasEnemy(nation) && !hasAlly(nation))
	                return true;
	        }
		} catch (AlreadyRegisteredException x) {
			return false;
		}
        
        return false;
    }

	public void setAssistants(List<Resident> assistants) {
		this.assistants = assistants;
	}

	public List<Resident> getAssistants() {
		return assistants;
	}

	public void setEnemies(List<Nation> enemies) {
		this.enemies = enemies;
	}

	public List<Nation> getEnemies() {
		return enemies;
	}

	public void setAllies(List<Nation> allies) {
		this.allies = allies;
	}

	public List<Nation> getAllies() {
		return allies;
	}
}
