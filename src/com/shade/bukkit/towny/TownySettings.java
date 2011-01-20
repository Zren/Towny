package com.shade.bukkit.towny;

import java.util.Arrays;
import java.util.List;



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
	
	public List<String> getResidentCommands() {
		return Arrays.asList(new String[]{"/resident","/player"});
	}
	
	public List<String> getTownCommands() {
		return Arrays.asList(new String[]{"/town"});
	}
	
	public List<String> getNationCommands() {
		return Arrays.asList(new String[]{"/nation"});
	}
	
	public List<String> getPlotCommands() {
		return Arrays.asList(new String[]{"/plot"});
	}
	
	public List<String> getTownyCommands() {
		return Arrays.asList(new String[]{"/towny"});
	}
	
	public List<String> getTownyAdminCommands() {
		return Arrays.asList(new String[]{"/townyadmin"});
	}
	
	public List<String> getTownChatCommands() {
		return Arrays.asList(new String[]{"/tc"});
	}
	
	public List<String> getNationChatCommands() {
		return Arrays.asList(new String[]{"/nc"});
	}

	public Town getDefaultTown() {
		return defaultTown;
	}

	public int getInactiveAfter() {
		return 24 * 60 * 60 * 1000;
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

	public int getMaxInactivePeriod() {
		return 0;
	}

	public boolean isDeletingOldResidents() {
		return false;
	}

	public int getWarTimeWarningDelay() {
		return 30;
	}

	public int getWarzoneTownBlockHealth() {
		return 60;
	}
	
	public int getWarzoneHomeBlockHealth() {
		return 120;
	}

	public String[] getJoinWarMsg(TownyObject obj) {
		return parseString(String.format("%s joined the fight!", obj.getName()));
	}

	public String[] getWarTimeEliminatedMsg(String who) {
		return parseString(String.format("%s was eliminated from the war.", who));
	}
	
	public String[] getWarTimeForfeitMsg(String who) {
		return parseString(String.format("%s forfeited.", who));
	}
}
