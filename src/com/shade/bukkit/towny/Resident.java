package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class Resident extends TownBlockOwner {
	private List<Resident> friends = new ArrayList<Resident>();
	private Town town;
	private long lastOnline, registered;

	public Resident(String name) {
		setName(name);
		permissions.allyBuild = true;
		permissions.allyDestroy = true;
		permissions.residentBuild = true;
		permissions.residentDestroy = true;
	}

	public void setLastOnline(long lastOnline) {
		this.lastOnline = lastOnline;
	}

	public long getLastOnline() {
		return lastOnline;
	}

	public boolean isKing() {
		try {
			return getTown().getNation().isKing(this);
		} catch (TownyException e) {
			return false;
		}
	}

	public boolean isMayor() {
		return hasTown() ? town.isMayor(this) : false;
	}

	public boolean hasTown() {
		return !(town == null);
	}

	public boolean hasNation() {
		return hasTown() ? town.hasNation() : false;
	}

	public Town getTown() throws NotRegisteredException {
		if (hasTown())
			return town;
		else
			throw new NotRegisteredException(
					"Resident doesn't belong to any town");
	}

	public void setTown(Town town) throws AlreadyRegisteredException {
		if (town == null) {
			this.town = null;
			return;
		}
		if (this.town == town)
			return;
		if (hasTown())
			throw new AlreadyRegisteredException();
		this.town = town;
	}

	public void setFriends(List<Resident> friends) {
		this.friends = friends;
	}

	public List<Resident> getFriends() {
		return friends;
	}

	public boolean removeFriend(Resident resident) throws NotRegisteredException {
		if (hasFriend(resident))
			return friends.remove(resident);
		else
			throw new NotRegisteredException();
	}

	public boolean hasFriend(Resident resident) {
		return friends.contains(resident);
	}

	public void addFriend(Resident resident) throws AlreadyRegisteredException {
		if (hasFriend(resident))
			throw new AlreadyRegisteredException();
		else
			friends.add(resident);
	}
	
	public void removeAllFriends() {
		for (Resident resident : new ArrayList<Resident>(friends))
			try {
				removeFriend(resident);
			} catch (NotRegisteredException e) {
			}
	}

	public void clear() throws EmptyTownException {
		removeAllFriends();
		setLastOnline(0);
		
		if (hasTown())
			try {
				town.removeResident(this);
			} catch (NotRegisteredException e) {
			}
	}

	public void setRegistered(long registered) {
		this.registered = registered;
	}

	public long getRegistered() {
		return registered;
	}
}
