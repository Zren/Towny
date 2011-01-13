package com.shade.bukkit.towny;

public class TownySettings {
	private Town defaultTown;
	
	public String[] parseString(String str) {
		return str.replaceAll("&", "\u00A7").split("@");
	}
	
	public String[] getRegistrationMsg() {
		return parseString("Welcome this is your first login.@You've successfully registered!");
	}
	
	public String[] getNewTownMsg(String who, String town) {
		return parseString(String.format("%s created a new town called %s", who, town));
	}
	
	public String[] getJoinTownMsg(String who) {
		return parseString(String.format("%s joined town!", who));
	}
	
	public String[] getNewMayorMsg(String who) {
		return parseString(String.format("%s is now the mayor!", who));
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

	public boolean isTownCreationAdminOnly() {
		return false;
	}
}
