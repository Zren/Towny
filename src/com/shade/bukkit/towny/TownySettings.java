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
	
	public String[] getNewNationMsg(String who, String nation) {
		return parseString(String.format("%s created a new nation called %s", who, nation));
	}
	
	public String[] getJoinTownMsg(String who) {
		return parseString(String.format("%s joined town!", who));
	}
	
	public String[] getJoinNationMsg(String who) {
		return parseString(String.format("%s joined the nation!", who));
	}
	
	public String[] getNewMayorMsg(String who) {
		return parseString(String.format("%s is now the mayor!", who));
	}
	
	public String[] getNewKingMsg(String who) {
		return parseString(String.format("%s is now the king!", who));
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
		return 64;
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

	public boolean isUsingIConomy() {
		return false;
	}

	public int getNewTownPrice() {
		return 100;
	}

	public boolean getUnclaimedZoneBuildRights() {
		return false;
	}
	
	public boolean getDebug() {
		return true;
	}

	public boolean getShowTownNotifications() {
		return true;
	}

	public String getUnclaimedZoneName() {
		return "Wilderness";
	}

	public boolean getUnclaimedZoneDestroyRights() {
		return false;
	}

	public boolean isUsingChatPrefix() {
		return true;
	}
}
