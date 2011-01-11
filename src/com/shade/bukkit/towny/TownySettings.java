package com.shade.bukkit.towny;

public class TownySettings {
	private Town defaultTown;
	
	public String[] getRegistrationMsg() {
		return new String[]{"Welcome this is your first login.", "You've successfully registered!"};
	}

	public void setDefaultTown(Town defaultTown) {
		this.defaultTown = defaultTown;
	}

	public Town getDefaultTown() {
		return defaultTown;
	}
	
	public int getInactiveAfter() {
		return 24*60*60*1000;
	}
}
