package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

public class Town extends TownBlockOwner {
	private List<Resident> residents = new ArrayList<Resident>();
	private List<Resident> assistants = new ArrayList<Resident>();
	private Resident mayor;
	private int bonusBlocks, taxes;
	private Nation nation;
	private boolean isPVP, hasMobs;
	private String townBoard;
	private TownBlock homeBlock;
	private TownyWorld world;
	private TownyPermission permissions = new TownyPermission();
	private Location spawn;
	
	public Town(String name) {
		setName(name);
		permissions.allies = true;
		permissions.residentBuild = true;
		permissions.residentDestroy = true;
	}

	public Resident getMayor() {
		return mayor;
	}

	public void setMayor(Resident mayor) throws TownyException {
		if (!hasResident(mayor))
			throw new TownyException("Mayor doesn't belong to town.");
		this.mayor = mayor;
	}

	public Nation getNation() throws TownyException {
		if (hasNation())
			return nation;
		else
			throw new TownyException("Town doesn't belong to any nation.");
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
		return resident == mayor;
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
	public void setHomeBlock(TownBlock homeBlock) throws TownyException {
		if (!hasTownBlock(homeBlock))
			throw new TownyException("Town has no claim over this town block.");
		this.homeBlock = homeBlock;
		try {
			setSpawn(spawn);
		} catch (TownyException e) {
			spawn = null;
		} catch(NullPointerException e) { 
			// In the event that spawn is already null
		}
	}

	public TownBlock getHomeBlock() throws TownyException {
		if (hasHomeBlock())
			return homeBlock;
		else
			throw new TownyException("Town has not set a home block."); 
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

	public void setTaxes(int taxes) {
		this.taxes = taxes;
	}

	public int getTaxes() {
		return taxes;
	}
	
	public void removeResident(Resident resident) throws NotRegisteredException {
		if (!hasResident(resident)) {
			throw new NotRegisteredException();
		} else {
			//TODO: Remove all plots of land owned in town.
			residents.remove(resident);
		}
	}
	
	public void collectTaxes() throws IConomyException {
		for (Resident resident : residents) {
			// Mayor and his assistants don't have to pay.
			if (!hasAssistant(resident) || !isMayor(resident)) {
				if (resident.pay(getTaxes(), this)) {
					
				} else {
					//TODO: Make a message that tells the player they ran out of money to live in the town.
					try {
						removeResident(resident);
					} catch (NotRegisteredException e) {
						//TODO: Possibly format a list of residents who refused to leave?
					}
				}
			}
		}
	}

	public void setSpawn(Location spawn) throws TownyException {
		if (!hasHomeBlock())
			throw new TownyException("Home Block has not been set");
		Coord spawnBlock = Coord.parseCoord(spawn);
		if (homeBlock.getX() == spawnBlock.getX() && homeBlock.getZ() == spawnBlock.getZ())
			this.spawn = spawn;
		else
			throw new TownyException("Spawn is not within the homeBlock."); 
	}

	public Location getSpawn() throws TownyException {
		if (hasSpawn())
			return spawn;
		else
			throw new TownyException("Town has not set a spawn location."); 
	}

	private boolean hasSpawn() {
		return spawn != null;
	}

	public boolean hasHomeBlock() {
		return homeBlock != null;
	}
}
