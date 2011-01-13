package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class Town extends TownyObject {
	private List<Resident> residents = new ArrayList<Resident>();
	private List<Resident> assistants = new ArrayList<Resident>();
	private List<TownBlock> townblocks = new ArrayList<TownBlock>();
	private Resident mayor;
	private int bonusBlocks;
	private Nation nation;
	private boolean isPVP, hasMobs;
	private String townBoard;
	private TownBlock homeBlock;
	private TownyWorld world;
	private TownyPermission permissions = new TownyPermission();
	
	public Town(String name) {
		setName(name);
	}

	public Resident getMayor() {
		return mayor;
	}

	public void setMayor(Resident mayor) throws TownyException {
		if (!hasResident(mayor))
			throw new TownyException("Mayor doesn't belong to town.");
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
		return nation != null;
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

	public void setPermissions(String line) {
		permissions.reset();
		permissions.load(line);
	}
	
	public TownyPermission getPermissions() {
		return permissions;
	}

	public void setBonusBlocks(int bonusBlocks) {
		this.bonusBlocks = bonusBlocks;
	}

	public int getBonusBlocks() {
		return bonusBlocks;
	}

	public void setTownblocks(List<TownBlock> townblocks) {
		this.townblocks = townblocks;
	}

	public List<TownBlock> getTownblocks() {
		return townblocks;
	}

	public void setHomeBlock(TownBlock homeBlock) {
		this.homeBlock = homeBlock;
	}

	public TownBlock getHomeBlock() {
		return homeBlock;
	}

	public void setWorld(TownyWorld world) {
		this.world = world;
	}

	public TownyWorld getWorld() {
		return world;
	}

	public boolean hasMayor() {
		return mayor != null;
	}
}
