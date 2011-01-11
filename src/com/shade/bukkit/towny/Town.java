package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class Town extends TownyObject {
	private List<Resident> residents = new ArrayList<Resident>();
	private List<Resident> assistants = new ArrayList<Resident>();
	private Resident mayor;
	private int bonusBlocks;
	private Nation nation;
	private boolean isPVP, hasMobs;
	private String townBoard;
	private TownyPermission permissions = new TownyPermission();
	
	public Resident getMayor() {
		return mayor;
	}

	public void setMayor(Resident mayor) {
		this.mayor = mayor;
	}

	public Nation getNation() {
		return nation;
	}

	public void setNation(Nation nation) {
		this.nation = nation;
	}

	public List<Resident> getResidents() {
		return residents;
	}

	public List<Resident> getAssistants() {
		return assistants;
	}	
	
	public boolean hasResident(String name) {
		for (Resident resident : residents)
			if (resident.getName().equalsIgnoreCase(name))
				return true;
		return false;
	}
	
	public boolean hasResident(Resident resident) {
		return residents.contains(resident);
	}
	
	public boolean hasAssistant(Resident resident) {
		if (resident == null)
			return false;
		else
			return assistants.contains(resident);
	}
	
	public void addResident(Resident resident) throws AlreadyRegisteredException {
		if (hasResident(resident)) {
			throw new AlreadyRegisteredException();
		} else {
			residents.add(resident);
			resident.setTown(this);
		}
	}
	
	public void addAssistant(Resident resident) throws AlreadyRegisteredException {
		if (hasAssistant(resident)) {
			throw new AlreadyRegisteredException();
		} else {
			assistants.add(resident);
		}
	}
	
	public boolean isMayor(Resident resident) {
		return (resident == mayor);
	}
	
	public boolean hasNation() {
		return !(nation == null);
	}
	
	public int getNumResidents() {
		return residents.size();
	}
	
	public boolean isCapital() {
		return (hasNation() ? nation.isCapital(this) : false);
	}

	public void setHasMobs(boolean hasMobs) {
		this.hasMobs = hasMobs;
	}

	public boolean hasMobs() {
		return hasMobs;
	}

	public void setPVP(boolean isPVP) {
		this.isPVP = isPVP;
	}

	public boolean isPVP() {
		return isPVP;
	}

	public void setTownBoard(String townBoard) {
		this.townBoard = townBoard;
	}

	public String getTownBoard() {
		return townBoard;
	}
}
