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
	
	public String getKingPrefix() {
		return "King ";
	}
	
	public String getMayorPrefix() {
		return "Mayor ";
	}
	
	public String getCapitalPostfix() {
		return " Capital City";
	}
	
	public String getTownPostfix() {
		return " Town";
	}

	public String getFlatFileFolder() {
		return "towny";
	}
	
	public String getLoadDatabase() {
		return "flatfile";
	}
	
	public String getSaveDatabase() {
		return "flatfile";
	}

	public boolean isFirstRun() {
		return false;
	}

	public String getNationPostfix() {
		return " Nation";
	}

	public int getMaxTownBlocks(Town town) {
		return 16;
	}

	public int getTownBlockSize() {
		return 16;
	}
	
	public boolean getFriendlyFire() {
		return false;
	}
}
