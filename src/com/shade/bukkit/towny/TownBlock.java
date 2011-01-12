package com.shade.bukkit.towny;

public class TownBlock {
	//private List<Group> groups;
	private Town town;
	private Resident resident;
	private int x, z;
	private boolean isForSale = false;
	
	public TownBlock(int x, int z) {
		this.x = x;
		this.z = z;
	}
	
	public void setTown(Town town) {
		this.town = town;
	}
	public Town getTown() {
		return town;
	}
	public boolean hasTown() {
		return town != null;
	}
	public void setResident(Resident resident) {
		this.resident = resident;
	}
	public Resident getResident() {
		return resident;
	}
	public boolean hasResident() {
		return resident != null;
	}
	public void setForSale(boolean isForSale) {
		this.isForSale = isForSale;
	}
	public boolean isForSale() {
		return isForSale;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getX() {
		return x;
	}
	public void setZ(int z) {
		this.z = z;
	}
	public int getZ() {
		return z;
	}
}
