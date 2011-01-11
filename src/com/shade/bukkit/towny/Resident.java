package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class Resident extends TownyObject {
	private List<Resident> friends = new ArrayList<Resident>();
	private Town town;
	private long lastOnline;
	
	public Resident(String name) {
		setName(name);
	}
	
	

	public void setLastOnline(long lastOnline) {
		this.lastOnline = lastOnline;
	}

	public long getLastOnline() {
		return lastOnline;
	}

	public boolean isKing() {
		return (hasNation() ? town.getNation().isKing(this) : false);
	}

	public boolean isMayor() {
		return (hasTown() ? town.isMayor(this) : false);
	}
	
	public boolean hasTown() {
		return !(town == null);
	}
	
	public boolean hasNation() {
		return (hasTown() ? town.hasNation() : false);
	}
	
	public Town getTown() throws TownyException {
		if (hasTown())
			return town;
		else
			throw new TownyException("Resident doesn't belong to any town");
	}
	
	public void setTown(Town town) {
		this.town = town;
	}



	public void setFriends(List<Resident> friends) {
		this.friends = friends;
	}



	public List<Resident> getFriends() {
		return friends;
	}
}
