package ca.xshade.bukkit.towny;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.towny.object.TownyObject;
import ca.xshade.bukkit.towny.object.WorldCoord;
import ca.xshade.util.KeyValueFile;
import ca.xshade.util.StringMgmt;


//TODO: Make String[]/StrArr into lists.

public class TownySettings {
	// String[]
	public enum StrArr {
		DATABASE_SAVE,
		COMMANDS_RESIDENT,
		COMMANDS_TOWN,
		COMMANDS_NATION,
		COMMANDS_WORLD,
		COMMANDS_PLOT,
		COMMANDS_TOWNY,
		COMMANDS_TOWNY_ADMIN,
		COMMANDS_TOWN_CHAT,
		COMMANDS_NATION_CHAT,
		MOB_REMOVAL_ENTITIES
	};
	// Integer[]
	public enum IntArr {
		SWITCH_IDS,
		UNCLAIMED_ZONE_IGNORE,
		ITEM_USE_IDS
	};
	// String
	public enum Str {
		LAST_RUN_VERSION,
		DATABASE_LOAD,
		DEFAULT_TOWN_NAME,
		DEFAULT_MAYOR_PREFIX,
		DEFAULT_MAYOR_POSTFIX,
		DEFAULT_KING_PREFIX,
		DEFAULT_KING_POSTFIX,
		DEFAULT_CAPITAL_PREFIX,
		DEFAULT_CAPITAL_POSTFIX,
		DEFAULT_TOWN_PREFIX,
		DEFAULT_TOWN_POSTFIX,
		DEFAULT_NATION_PREFIX,
		DEFAULT_NATION_POSTFIX,
		UNCLAIMED_ZONE_NAME,
		MSG_REGISTRATION,
		MSG_NEW_TOWN,
		MSG_NEW_NATION,
		MSG_JOIN_TOWN,
		MSG_JOIN_NATION,
		MSG_DEL_RESIDENT,
		MSG_DEL_TOWN,
		MSG_DEL_NATION,
		MSG_NEW_MAYOR,
		MSG_NEW_KING,
		MSG_WAR_JOIN,
		MSG_WAR_ELIMINATED,
		MSG_WAR_FORFEITED,
		MSG_WAR_LOSE_BLOCK,
		MSG_WAR_SCORE,
		MSG_NEW_DAY,
		MSG_COULDNT_PAY_TAXES,
		MSG_BUY_RESIDENT_PLOT,
		MSG_PLOT_FOR_SALE,
		MSG_MAYOR_ABADON, //TODO
		LINE_NOT_PERM_TO_NEW_TOWN, //TODO
		LINE_NOT_PERM_TO_NEW_NATION, //TODO 
		UNCLAIMED_PLOT_NAME,
		NPC_PREFIX,
		FLATFILE_BACKUP //TODO
	};
	// Integer
	public enum Int {
		TOWN_BLOCK_SIZE,
		TOWN_BLOCK_RATIO,
		DEFAULT_MAX_TOWN_BLOCKS,
		PRICE_NATION_NEUTRALITY,
		WARTIME_WARNING_DELAY,
		WARTIME_TOWN_BLOCK_HP,
		WARTIME_HOME_BLOCK_HP,
		WARTIME_POINTS_TOWNBLOCK,
		WARTIME_POINTS_TOWN,
		WARTIME_POINTS_NATION,
		WARTIME_POINTS_KILL,
		WARTIME_MIN_HEIGHT,
		MOB_REMOVAL_SPEED,
		HEALTH_REGEN_SPEED,
		TOWN_LIMIT
	};
	// Long
	public enum KeyLong {
		INACTIVE_AFTER_TIME,
		DELETED_AFTER_TIME,
		DAY_INTERVAL
	};
	// Double
	public enum Doub {
		PRICE_CLAIM_TOWNBLOCK,
		PRICE_OUTPOST,
		PRICE_TOWN_SPAWN_TRAVEL,
		WARTIME_DEATH_PRICE,
		DEATH_PRICE,
		WARTIME_TOWN_BLOCK_LOSS_PRICE,
		PRICE_TOWN_UPKEEP,
		PRICE_NATION_UPKEEP,
		WARTIME_BASE_SPOILS,
		PRICE_NEW_TOWN,
		PRICE_NEW_NATION
	};
	// Boolean
	public enum Bool {
		FIRST_RUN,
		FRIENDLY_FIRE,
		TOWN_CREATION_ADMIN_ONLY,
		NATION_CREATION_ADMIN_ONLY,
		UNCLAIMED_ZONE_BUILD,
		UNCLAIMED_ZONE_DESTROY,
		UNCLAIMED_ZONE_SWITCH,
		UNCLAIMED_ZONE_ITEM_USE,
		SHOW_TOWN_NOTIFICATIONS,
		USING_ICONOMY,
		USING_ESSENTIALS,
		MODIFY_CHAT_NAME,
		DELETE_OLD_RESIDENTS,
		DEBUG_MODE,
		MOB_REMOVAL,
		HEALTH_REGEN,
		ALLOW_OUTPOSTS,
		ALLOW_TOWN_SPAWN_TRAVEL,
		DEV_MODE,
		WARTIME_REMOVE_ON_MONARCH_DEATH, //TODO: Add to Wiki
		ALLOW_TOWN_SPAWN, //TODO
		PVE_IN_NON_PVP_TOWNS, //TODO
		FORCE_PVP_ON, //TODO
		TOWN_RESPAWN, //TODO
		DAILY_TAXES, //TODO
		DAILY_BACKUPS //TODO
	};
	// Nation Level
	public enum NationLevel {
		NAME_PREFIX,
		NAME_POSTFIX,
		CAPITAL_PREFIX,
		CAPITAL_POSTFIX,
		KING_PREFIX,
		KING_POSTFIX
	};
	// Town Level
	public enum TownLevel {
		NAME_PREFIX,
		NAME_POSTFIX,
		MAYOR_PREFIX,
		MAYOR_POSTFIX,
		TOWN_BLOCK_LIMIT
	};
	
	
	
	private static final ConcurrentHashMap<TownySettings.StrArr,List<String>> configStrArr
		= new ConcurrentHashMap<TownySettings.StrArr,List<String>>();
	private static final ConcurrentHashMap<TownySettings.IntArr,List<Integer>> configIntArr
		= new ConcurrentHashMap<TownySettings.IntArr,List<Integer>>();
	private static final ConcurrentHashMap<TownySettings.Str,String> configStr
		= new ConcurrentHashMap<TownySettings.Str,String>();
	private static final ConcurrentHashMap<TownySettings.Int,Integer> configInt
		= new ConcurrentHashMap<TownySettings.Int,Integer>();
	private static final ConcurrentHashMap<TownySettings.KeyLong,Long> configLong
		= new ConcurrentHashMap<TownySettings.KeyLong,Long>();
	private static final ConcurrentHashMap<TownySettings.Doub,Double> configDoub
		= new ConcurrentHashMap<TownySettings.Doub,Double>();
	private static final ConcurrentHashMap<TownySettings.Bool,Boolean> configBool
		= new ConcurrentHashMap<TownySettings.Bool,Boolean>();
	/*
	private static final ConcurrentHashMap<Integer,ConcurrentHashMap<TownySettings.NationLevel,Object>> configNationLevel
		= new ConcurrentHashMap<Integer,ConcurrentHashMap<TownySettings.NationLevel,Object>>();
	private static final ConcurrentHashMap<Integer,ConcurrentHashMap<TownySettings.TownLevel,Object>> configTownLevel
		= new ConcurrentHashMap<Integer,ConcurrentHashMap<TownySettings.TownLevel,Object>>();
	*/
	
	private static final SortedMap<Integer,Map<TownySettings.TownLevel,Object>> configTownLevel = 
		Collections.synchronizedSortedMap(new TreeMap<Integer,Map<TownySettings.TownLevel,Object>>(Collections.reverseOrder()));
	private static final SortedMap<Integer,Map<TownySettings.NationLevel,Object>> configNationLevel = 
		Collections.synchronizedSortedMap(new TreeMap<Integer,Map<TownySettings.NationLevel,Object>>(Collections.reverseOrder()));
	
	
	static {
		// String[]
		configStrArr.put(TownySettings.StrArr.DATABASE_SAVE, new ArrayList<String>(Arrays.asList(new String[]{"flatfile"})));
		configStrArr.put(TownySettings.StrArr.COMMANDS_RESIDENT, new ArrayList<String>(Arrays.asList(new String[]{"/resident","/r","/res","/p","/player"})));
		configStrArr.put(TownySettings.StrArr.COMMANDS_TOWN, new ArrayList<String>(Arrays.asList(new String[]{"/town","/t"})));
		configStrArr.put(TownySettings.StrArr.COMMANDS_NATION, new ArrayList<String>(Arrays.asList(new String[]{"/nation","/n","/nat"})));
		configStrArr.put(TownySettings.StrArr.COMMANDS_WORLD, new ArrayList<String>(Arrays.asList(new String[]{"/townyworld","/tw"})));
		configStrArr.put(TownySettings.StrArr.COMMANDS_PLOT, new ArrayList<String>(Arrays.asList(new String[]{"/plot"})));
		configStrArr.put(TownySettings.StrArr.COMMANDS_TOWNY, new ArrayList<String>(Arrays.asList(new String[]{"/towny"})));
		configStrArr.put(TownySettings.StrArr.COMMANDS_TOWNY_ADMIN, new ArrayList<String>(Arrays.asList(new String[]{"/townyadmin","/ta"})));
		configStrArr.put(TownySettings.StrArr.COMMANDS_TOWN_CHAT, new ArrayList<String>(Arrays.asList(new String[]{"/tc"})));
		configStrArr.put(TownySettings.StrArr.COMMANDS_NATION_CHAT, new ArrayList<String>(Arrays.asList(new String[]{"/nc"})));
		configStrArr.put(TownySettings.StrArr.MOB_REMOVAL_ENTITIES, new ArrayList<String>(Arrays.asList(new String[]{
				"Monster", "WaterMob", "Flying", "Slime"
		})));
		// Integer[]
		configIntArr.put(TownySettings.IntArr.SWITCH_IDS,  new ArrayList<Integer>(Arrays.asList(new Integer[]{64,69,70,71,72,77})));
		configIntArr.put(TownySettings.IntArr.UNCLAIMED_ZONE_IGNORE, new ArrayList<Integer>(Arrays.asList(new Integer[]{14,15,16,21,56,65,66,73,74,89})));
		configIntArr.put(TownySettings.IntArr.ITEM_USE_IDS,  new ArrayList<Integer>(Arrays.asList(new Integer[]{259,325,326,327})));
		// String
		configStr.put(TownySettings.Str.LAST_RUN_VERSION, "2.0.0");
		configStr.put(TownySettings.Str.DATABASE_LOAD, "flatfile");
		configStr.put(TownySettings.Str.DEFAULT_TOWN_NAME, "");
		configStr.put(TownySettings.Str.DEFAULT_MAYOR_PREFIX, "Mayor ");
		configStr.put(TownySettings.Str.DEFAULT_MAYOR_POSTFIX, "");
		configStr.put(TownySettings.Str.DEFAULT_KING_PREFIX, "King ");
		configStr.put(TownySettings.Str.DEFAULT_KING_POSTFIX, "");
		configStr.put(TownySettings.Str.DEFAULT_CAPITAL_PREFIX, "Capital: ");
		configStr.put(TownySettings.Str.DEFAULT_CAPITAL_POSTFIX, " City");
		configStr.put(TownySettings.Str.DEFAULT_TOWN_PREFIX, "");
		configStr.put(TownySettings.Str.DEFAULT_TOWN_POSTFIX, " Town");
		configStr.put(TownySettings.Str.DEFAULT_NATION_PREFIX, "");
		configStr.put(TownySettings.Str.DEFAULT_NATION_POSTFIX, " Nation");
		configStr.put(TownySettings.Str.UNCLAIMED_ZONE_NAME, "Wilderness");
		configStr.put(TownySettings.Str.MSG_REGISTRATION, "&6[Towny] &bWelcome this is your first login.@&6[Towny] &bYou've successfully registered!@&6[Towny] &bTry the /towny command for more help.");
		configStr.put(TownySettings.Str.MSG_NEW_TOWN, "&6[Towny] &b%s created a new town called %s");
		configStr.put(TownySettings.Str.MSG_NEW_NATION, "&6[Towny] &b%s created a new nation called %s");
		configStr.put(TownySettings.Str.MSG_JOIN_TOWN, "&6[Towny] &b%s joined town!");
		configStr.put(TownySettings.Str.MSG_JOIN_NATION, "&6[Towny] &b%s joined the nation!");
		configStr.put(TownySettings.Str.MSG_DEL_RESIDENT, "&6[Towny] &b%s lost all his Towny data!");
		configStr.put(TownySettings.Str.MSG_DEL_TOWN, "&6[Towny] &bThe town of %s fell into ruin!");
		configStr.put(TownySettings.Str.MSG_DEL_NATION, "&6[Towny] &bThe nation %s was disbanded!");
		configStr.put(TownySettings.Str.MSG_NEW_MAYOR, "&6[Towny] &b%s is now the mayor!");
		configStr.put(TownySettings.Str.MSG_NEW_KING, "&6[Towny] &b%s is now the king!");
		configStr.put(TownySettings.Str.MSG_WAR_JOIN, "&6[Towny] &b%s joined the fight!");
		configStr.put(TownySettings.Str.MSG_WAR_ELIMINATED, "&6[Towny] &b%s was eliminated from the war.");
		configStr.put(TownySettings.Str.MSG_WAR_FORFEITED, "&6[Towny] &b%s forfeited.");
		configStr.put(TownySettings.Str.MSG_WAR_LOSE_BLOCK, "&6[Towny] &b(%s) belonging to %s has fallen.");
		configStr.put(TownySettings.Str.MSG_WAR_SCORE, "&6[War] &b%s scored %d points!");
		configStr.put(TownySettings.Str.MSG_NEW_DAY, "&6[Towny] &bA new day is here! Taxes and rent has been collected.");
		configStr.put(TownySettings.Str.MSG_COULDNT_PAY_TAXES, "&6[Towny] &b%s couldn't pay taxes%s");
		configStr.put(TownySettings.Str.MSG_BUY_RESIDENT_PLOT, "&6[Towny] &b%s bought %s's plot!");
		configStr.put(TownySettings.Str.MSG_PLOT_FOR_SALE, "&6[Towny] &b%s put the plot (%s) up for sale!");
		configStr.put(TownySettings.Str.MSG_MAYOR_ABADON, "&6[Towny] &bYou would abandon your people? Choose another mayor with '/t set mayor' if your sure.");
		configStr.put(TownySettings.Str.LINE_NOT_PERM_TO_NEW_TOWN, "&6[Towny] &bOnly admins are allowed to create towns.");
		configStr.put(TownySettings.Str.LINE_NOT_PERM_TO_NEW_NATION, "&6[Towny] &bOnly admins are allowed to create nations.");
		configStr.put(TownySettings.Str.UNCLAIMED_PLOT_NAME, "Unowned");
		configStr.put(TownySettings.Str.NPC_PREFIX, "[NPC]");
		configStr.put(TownySettings.Str.FLATFILE_BACKUP, "zip");
		// Integer
		configInt.put(TownySettings.Int.TOWN_BLOCK_SIZE, 16);
		configInt.put(TownySettings.Int.TOWN_BLOCK_RATIO, 16);
		configInt.put(TownySettings.Int.DEFAULT_MAX_TOWN_BLOCKS, 64);
		configInt.put(TownySettings.Int.PRICE_NATION_NEUTRALITY, 0);
		configInt.put(TownySettings.Int.WARTIME_WARNING_DELAY, 30); // 30 seconds 
		configInt.put(TownySettings.Int.WARTIME_TOWN_BLOCK_HP, 60); // 1 minute
		configInt.put(TownySettings.Int.WARTIME_HOME_BLOCK_HP, 120); // 2 minutes
		configInt.put(TownySettings.Int.WARTIME_POINTS_TOWNBLOCK, 1);
		configInt.put(TownySettings.Int.WARTIME_POINTS_TOWN, 10);
		configInt.put(TownySettings.Int.WARTIME_POINTS_NATION, 100);
		configInt.put(TownySettings.Int.WARTIME_POINTS_KILL, 1);
		configInt.put(TownySettings.Int.WARTIME_MIN_HEIGHT, 60);
		configInt.put(TownySettings.Int.MOB_REMOVAL_SPEED, 5000); // 5 Seconds
		configInt.put(TownySettings.Int.HEALTH_REGEN_SPEED, 3000); // 9 Seconds (20*3 = 3 minute)
		configInt.put(TownySettings.Int.TOWN_LIMIT, 3000);
		// Long
		configLong.put(TownySettings.KeyLong.INACTIVE_AFTER_TIME, 86400000L); // 24 Hours
		configLong.put(TownySettings.KeyLong.DELETED_AFTER_TIME, 5184000000L); // Two Months
		configLong.put(TownySettings.KeyLong.DAY_INTERVAL, 86400000L); // 24 Hours
		//Double
		configDoub.put(TownySettings.Doub.PRICE_NEW_TOWN, 250D);
		configDoub.put(TownySettings.Doub.PRICE_NEW_NATION, 1000D);
		configDoub.put(TownySettings.Doub.PRICE_CLAIM_TOWNBLOCK, 25D);
		configDoub.put(TownySettings.Doub.PRICE_OUTPOST, 500D);
		configDoub.put(TownySettings.Doub.PRICE_TOWN_SPAWN_TRAVEL, 10D);
		configDoub.put(TownySettings.Doub.WARTIME_BASE_SPOILS, 100D);
		configDoub.put(TownySettings.Doub.WARTIME_DEATH_PRICE, 200D);
		configDoub.put(TownySettings.Doub.DEATH_PRICE, 10D);
		configDoub.put(TownySettings.Doub.WARTIME_TOWN_BLOCK_LOSS_PRICE, 100D);
		configDoub.put(TownySettings.Doub.PRICE_TOWN_UPKEEP, 10D);
		configDoub.put(TownySettings.Doub.PRICE_NATION_UPKEEP, 100D);
		// Boolean
		configBool.put(TownySettings.Bool.FIRST_RUN, true);
		configBool.put(TownySettings.Bool.FRIENDLY_FIRE, false);
		configBool.put(TownySettings.Bool.TOWN_CREATION_ADMIN_ONLY, false);
		configBool.put(TownySettings.Bool.NATION_CREATION_ADMIN_ONLY, false);
		configBool.put(TownySettings.Bool.UNCLAIMED_ZONE_BUILD, false);
		configBool.put(TownySettings.Bool.UNCLAIMED_ZONE_DESTROY, false);
		configBool.put(TownySettings.Bool.UNCLAIMED_ZONE_SWITCH, true);
		configBool.put(TownySettings.Bool.UNCLAIMED_ZONE_ITEM_USE, false);
		configBool.put(TownySettings.Bool.SHOW_TOWN_NOTIFICATIONS, true);
		configBool.put(TownySettings.Bool.USING_ICONOMY, true);
		configBool.put(TownySettings.Bool.USING_ESSENTIALS, true);
		configBool.put(TownySettings.Bool.MODIFY_CHAT_NAME, true);
		configBool.put(TownySettings.Bool.DELETE_OLD_RESIDENTS, false);
		configBool.put(TownySettings.Bool.DEBUG_MODE, false);
		configBool.put(TownySettings.Bool.MOB_REMOVAL, true);
		configBool.put(TownySettings.Bool.HEALTH_REGEN, true);
		configBool.put(TownySettings.Bool.ALLOW_OUTPOSTS, true);
		configBool.put(TownySettings.Bool.ALLOW_TOWN_SPAWN_TRAVEL, true);
		configBool.put(TownySettings.Bool.ALLOW_TOWN_SPAWN, true);
		configBool.put(TownySettings.Bool.DEV_MODE, false);
		configBool.put(TownySettings.Bool.WARTIME_REMOVE_ON_MONARCH_DEATH, false);
		configBool.put(TownySettings.Bool.PVE_IN_NON_PVP_TOWNS, true);
		configBool.put(TownySettings.Bool.FORCE_PVP_ON, false);
		configBool.put(TownySettings.Bool.TOWN_RESPAWN, true);
		configBool.put(TownySettings.Bool.DAILY_TAXES, true);
		configBool.put(TownySettings.Bool.DAILY_BACKUPS, true);
		
		newTownLevel(0, "", " Town", "Mayor ", "", 16);
		newNationLevel(0, "", " Nation", "Capital: ", " City", "King ", "");
	}
	
	public static void newTownLevel(int numResidents,
			String namePrefix, String namePostfix,
			String mayorPrefix, String mayorPostfix, 
			int townBlockLimit) {
		ConcurrentHashMap<TownySettings.TownLevel,Object> m = new ConcurrentHashMap<TownySettings.TownLevel,Object>();
		m.put(TownySettings.TownLevel.NAME_PREFIX, namePrefix);
		m.put(TownySettings.TownLevel.NAME_POSTFIX, namePostfix);
		m.put(TownySettings.TownLevel.MAYOR_PREFIX, mayorPrefix);
		m.put(TownySettings.TownLevel.MAYOR_POSTFIX, mayorPostfix);
		m.put(TownySettings.TownLevel.TOWN_BLOCK_LIMIT, townBlockLimit);
		configTownLevel.put(numResidents, m);
	}
	
	public static void newNationLevel(int numResidents, 
			String namePrefix, String namePostfix, 
			String capitalPrefix, String capitalPostfix,
			String kingPrefix, String kingPostfix) {
		ConcurrentHashMap<TownySettings.NationLevel,Object> m = new ConcurrentHashMap<TownySettings.NationLevel,Object>();
		m.put(TownySettings.NationLevel.NAME_PREFIX, namePrefix);
		m.put(TownySettings.NationLevel.NAME_POSTFIX, namePostfix);
		m.put(TownySettings.NationLevel.CAPITAL_PREFIX, capitalPrefix);
		m.put(TownySettings.NationLevel.CAPITAL_POSTFIX, capitalPostfix);
		m.put(TownySettings.NationLevel.KING_PREFIX, kingPrefix);
		m.put(TownySettings.NationLevel.KING_POSTFIX, kingPostfix);
		configNationLevel.put(numResidents, m);
	}
	
	/**
	 * Loads town levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 * 
	 * numResidents:namePrefix:namePostfix:mayorPrefix:mayorPostfix:townBlockLimit
	 * 
	 * townBlockLimit is a required field even if using a calculated ratio.
	 * 
	 * @param filepath
	 * @throws IOException 
	 */
	
	public static void loadTownLevelConfig(String filepath) throws IOException {
		String line;
		String[] tokens;
		BufferedReader fin = new BufferedReader(new FileReader(filepath));
        while ((line = fin.readLine()) != null)
			if (!line.startsWith("#")) { //Ignore comment lines
                tokens = line.split(",", 6);
                if (tokens.length >= 6)
					try {
                        int numResidents = Integer.parseInt(tokens[0]);
                        int townBlockLimit = Integer.parseInt(tokens[5]);
                        newTownLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], townBlockLimit);
						if (getDebug())
							// Used to know the actual values registered
							 System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(getTownLevel(numResidents).values().toArray()));
							//System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(tokens));
                    } catch (Exception e) {
                    	System.out.println("[Towny] Input Error: Town level ignored: " + line);
                    }
            }
        fin.close();
	}
	
	/**
	 * Loads nation levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 * 
	 * numResidents:namePrefix:namePostfix:capitalPrefix:capitalPostfix:kingPrefix:kingPostfix
	 * 
	 * @param filepath
	 * @throws IOException 
	 */
	
	public static void loadNationLevelConfig(String filepath) throws IOException {
		String line;
		String[] tokens;
		BufferedReader fin = new BufferedReader(new FileReader(filepath));
        while ((line = fin.readLine()) != null)
			if (!line.startsWith("#")) { //Ignore comment lines
                tokens = line.split(",", 7);
                if (tokens.length >= 7)
					try {
                        int numResidents = Integer.parseInt(tokens[0]);
                        newNationLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6]);
						if (getDebug())
							// Used to know the actual values registered
							// System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(getNationLevel(numResidents).values().toArray()));
							System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(tokens));
                    } catch (Exception e) {
                    	System.out.println("[Towny] Input Error: Nation level ignored: " + line);
                    }
            }
        fin.close();
	}
	
	public static Map<TownySettings.TownLevel,Object> getTownLevel(int numResidents) {
		return configTownLevel.get(numResidents);
	}
	
	public static Map<TownySettings.NationLevel,Object> getNationLevel(int numResidents) {
		return configNationLevel.get(numResidents);
	}
	
	public static Map<TownySettings.TownLevel,Object> getTownLevel(Town town) {
		return getTownLevel(calcTownLevel(town));
	}
	
	public static Map<TownySettings.NationLevel,Object> getNationLevel(Nation nation) {
		return getNationLevel(calcNationLevel(nation));
	}
	
	//TODO: more efficient way
	public static int calcTownLevel(Town town) {
		int n = town.getNumResidents();
		for (Integer level : configTownLevel.keySet())
			if (n >= level)
				return level;
        return 0;
    }
	
	//TODO: more efficient way
	public static int calcNationLevel(Nation nation) {
		int n = nation.getNumResidents();
		for (Integer level : configNationLevel.keySet())
			if (n >= level)
				return level;
        return 0;
    }
	
	public static HashMap<Object,Object> getMap() {
		HashMap<Object,Object> out = new HashMap<Object,Object>();
		out.putAll(configStrArr);
		out.putAll(configStr);
		out.putAll(configInt);
		out.putAll(configBool);
		return out;
	}
	
	public static void loadConfig(String filepath) throws IOException {
		KeyValueFile kvFile = new KeyValueFile(filepath);
		for (TownySettings.StrArr key : TownySettings.StrArr.values()) {
			String line = kvFile.getString(key.toString().toLowerCase(), StringMgmt.join(getStrArr(key), ","));
			configStrArr.put(key, Arrays.asList(line.split(",")));
		}
		for (TownySettings.IntArr key : TownySettings.IntArr.values()) {
			String line = kvFile.getString(key.toString().toLowerCase(), StringMgmt.join(getIntArr(key), ","));
			String[] tokens = line.split(",");
			List<Integer> nums = new ArrayList<Integer>();
			for (String token : tokens)
				try {
					nums.add(Integer.parseInt(token));
				} catch (NumberFormatException e) {
				}
			configIntArr.put(key, nums);
		}
		for (TownySettings.Str key : TownySettings.Str.values())
			configStr.put(key, kvFile.getString(key.toString().toLowerCase(), getString(key)));
		for (TownySettings.Int key : TownySettings.Int.values())
			configInt.put(key, kvFile.getInt(key.toString().toLowerCase(), getInt(key)));
		for (TownySettings.KeyLong key : TownySettings.KeyLong.values())
			configLong.put(key, kvFile.getLong(key.toString().toLowerCase(), getLong(key)));
		for (TownySettings.Doub key : TownySettings.Doub.values())
			configDoub.put(key, kvFile.getDouble(key.toString().toLowerCase(), getDouble(key)));
		for (TownySettings.Bool key : TownySettings.Bool.values())
			configBool.put(key, kvFile.getBoolean(key.toString().toLowerCase(), getBoolean(key)));
		
		kvFile.save();
	}
	
	public static Integer getInt(TownySettings.Int key) {
		return configInt.get(key);
	}
	
	public static Long getLong(TownySettings.KeyLong key) {
		return configLong.get(key);
	}
	
	public static Double getDouble(TownySettings.Doub key) {
		return configDoub.get(key);
	}
	
	public static Boolean getBoolean(TownySettings.Bool key) {
		return configBool.get(key);
	}
	
	public static String getString(TownySettings.Str key) {
		return configStr.get(key);
	}
	
	public static List<Integer> getIntArr(TownySettings.IntArr key) {
		return configIntArr.get(key);
	}
	
	public static List<String> getStrArr(TownySettings.StrArr key) {
		return configStrArr.get(key);
	}
	

	public static String[] parseString(String str) {
		return parseSingleLineString(str).split("@");
	}
	
	public static String parseSingleLineString(String str) {
		return str.replaceAll("&", "\u00A7");
	}

	public static String[] getRegistrationMsg() {
		return parseString(getString(TownySettings.Str.MSG_REGISTRATION));
	}

	public static String[] getNewTownMsg(String who, String town) {
		return parseString(String.format(getString(TownySettings.Str.MSG_NEW_TOWN), who, town));
	}

	public static String[] getNewNationMsg(String who, String nation) {
		return parseString(String.format(getString(TownySettings.Str.MSG_NEW_NATION), who, nation));
	}

	public static String[] getJoinTownMsg(String who) {
		return parseString(String.format(getString(TownySettings.Str.MSG_JOIN_TOWN), who));
	}

	public static String[] getJoinNationMsg(String who) {
		return parseString(String.format(getString(TownySettings.Str.MSG_JOIN_NATION), who));
	}

	public static String[] getNewMayorMsg(String who) {
		return parseString(String.format(getString(TownySettings.Str.MSG_NEW_MAYOR), who));
	}

	public static String[] getNewKingMsg(String who) {
		return parseString(String.format(getString(TownySettings.Str.MSG_NEW_KING), who));
	}
	
	public static List<String> getResidentCommands() {
		return getStrArr(TownySettings.StrArr.COMMANDS_RESIDENT);
	}
	
	public static List<String> getTownCommands() {
		return getStrArr(TownySettings.StrArr.COMMANDS_TOWN);
	}
	
	public static List<String> getNationCommands() {
		return getStrArr(TownySettings.StrArr.COMMANDS_NATION);
	}
	
	public static List<String> getWorldCommands() {
		return getStrArr(TownySettings.StrArr.COMMANDS_WORLD);
	}
	
	public static List<String> getPlotCommands() {
		return getStrArr(TownySettings.StrArr.COMMANDS_PLOT);
	}
	
	public static List<String> getTownyCommands() {
		return getStrArr(TownySettings.StrArr.COMMANDS_TOWNY);
	}
	
	public static List<String> getTownyAdminCommands() {
		return getStrArr(TownySettings.StrArr.COMMANDS_TOWNY_ADMIN);
	}
	
	public static List<String> getTownChatCommands() {
		return getStrArr(TownySettings.StrArr.COMMANDS_TOWN_CHAT);
	}
	
	public static List<String> getNationChatCommands() {
		return getStrArr(TownySettings.StrArr.COMMANDS_NATION_CHAT);
	}
	
	public static String getFirstCommand(List<String> commands) {
		if (commands.size() > 0)
			return commands.get(0);
		else
			return "/<unknown>";
	}

	public static long getInactiveAfter() {
		return getLong(TownySettings.KeyLong.INACTIVE_AFTER_TIME);
	}

	public static String getKingPrefix(Resident resident) {
		try {
			return (String)getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_PREFIX);
		} catch (NotRegisteredException e) {
			return getString(TownySettings.Str.DEFAULT_KING_PREFIX);
		}
	}

	public static String getMayorPrefix(Resident resident) {
		try {
			return (String)getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_PREFIX);
		} catch (NotRegisteredException e) {
			return getString(TownySettings.Str.DEFAULT_MAYOR_PREFIX);
		}
	}

	public static String getCapitalPostfix(Town town) {
		try {
			return (String)getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_POSTFIX);
		} catch (NotRegisteredException e) {
			return getString(TownySettings.Str.DEFAULT_CAPITAL_POSTFIX);
		}
	}

	public static String getTownPostfix(Town town) {
		try {
			return (String)getTownLevel(town).get(TownySettings.TownLevel.NAME_POSTFIX);
		} catch (Exception e) {
			// Should not reach here
			return getString(TownySettings.Str.DEFAULT_TOWN_POSTFIX);
		}
	}

	public static String getLoadDatabase() {
		return getString(TownySettings.Str.DATABASE_LOAD);
	}

	public static List<String> getSaveDatabases() {
		return getStrArr(TownySettings.StrArr.DATABASE_SAVE);
	}
	
	//TODO: Remove workaround
	public static String getSaveDatabase() {
		return getSaveDatabases().get(0);
	}

	public static boolean isFirstRun() {
		return getBoolean(TownySettings.Bool.FIRST_RUN);
	}

	public static String getNationPostfix() {
		return getString(TownySettings.Str.DEFAULT_NATION_POSTFIX);
	}

	public static int getMaxTownBlocks(Town town) {
		int ratio = getInt(TownySettings.Int.TOWN_BLOCK_RATIO);
		if (ratio == 0)
			return town.getBonusBlocks() + (Integer)getTownLevel(town).get(TownySettings.TownLevel.TOWN_BLOCK_LIMIT);
		else
			return town.getBonusBlocks() + town.getNumResidents()*ratio;
	}

	public static int getTownBlockSize() {
		return getInt(TownySettings.Int.TOWN_BLOCK_SIZE);
	}

	public static boolean getFriendlyFire() {
		return getBoolean(TownySettings.Bool.FRIENDLY_FIRE);
	}

	public static boolean isTownCreationAdminOnly() {
		return getBoolean(TownySettings.Bool.TOWN_CREATION_ADMIN_ONLY);
	}
	
	public static boolean isNationCreationAdminOnly() {
		return getBoolean(TownySettings.Bool.NATION_CREATION_ADMIN_ONLY);
	}

	public static boolean isUsingIConomy() {
		return getBoolean(TownySettings.Bool.USING_ICONOMY);
	}
	
	public static boolean isUsingEssentials() {
		return getBoolean(TownySettings.Bool.USING_ESSENTIALS);
	}

	public static double getNewTownPrice() {
		return getDouble(TownySettings.Doub.PRICE_NEW_TOWN);
	}
	
	public static double getNewNationPrice() {
		return getDouble(TownySettings.Doub.PRICE_NEW_NATION);
	}

	public static boolean getUnclaimedZoneBuildRights() {
		return getBoolean(TownySettings.Bool.UNCLAIMED_ZONE_BUILD);
	}
	
	public static boolean getUnclaimedZoneDestroyRights() {
		return getBoolean(TownySettings.Bool.UNCLAIMED_ZONE_DESTROY);
	}
	
	public static boolean getUnclaimedZoneItemUseRights() {
		return getBoolean(TownySettings.Bool.UNCLAIMED_ZONE_ITEM_USE);
	}

	public static boolean getDebug() {
		return getBoolean(TownySettings.Bool.DEBUG_MODE);
	}

	public static boolean getShowTownNotifications() {
		return getBoolean(TownySettings.Bool.SHOW_TOWN_NOTIFICATIONS);
	}

	public static String getUnclaimedZoneName() {
		return getString(TownySettings.Str.UNCLAIMED_ZONE_NAME);
	}

	public static boolean isUsingChatPrefix() {
		return getBoolean(TownySettings.Bool.MODIFY_CHAT_NAME);
	}

	public static long getMaxInactivePeriod() {
		return getLong(TownySettings.KeyLong.DELETED_AFTER_TIME);
	}

	public static boolean isDeletingOldResidents() {
		return getBoolean(TownySettings.Bool.DELETE_OLD_RESIDENTS);
	}

	public static int getWarTimeWarningDelay() {
		return getInt(TownySettings.Int.WARTIME_WARNING_DELAY);
	}

	public static int getWarzoneTownBlockHealth() {
		return getInt(TownySettings.Int.WARTIME_TOWN_BLOCK_HP);
	}
	
	public static int getWarzoneHomeBlockHealth() {
		return getInt(TownySettings.Int.WARTIME_HOME_BLOCK_HP);
	}

	public static String[] getJoinWarMsg(TownyObject obj) {
		return parseString(String.format(getString(TownySettings.Str.MSG_WAR_JOIN), obj.getName()));
	}

	public static String[] getWarTimeEliminatedMsg(String who) {
		return parseString(String.format(getString(TownySettings.Str.MSG_WAR_ELIMINATED), who));
	}
	
	public static String[] getWarTimeForfeitMsg(String who) {
		return parseString(String.format(getString(TownySettings.Str.MSG_WAR_FORFEITED), who));
	}

	public static String[] getWarTimeLoseTownBlockMsg(WorldCoord worldCoord) {
		return getWarTimeLoseTownBlockMsg(worldCoord, "");
	}
	
	public static String[] getWarTimeLoseTownBlockMsg(WorldCoord worldCoord, String town) {
		return parseString(String.format(getString(TownySettings.Str.MSG_WAR_LOSE_BLOCK), worldCoord.toString(), town));
	}
	
	public static String[] getNewDayMsg() {
		return parseString(getString(TownySettings.Str.MSG_NEW_DAY));
	}

	public static String getDefaultTownName() {
		return getString(TownySettings.Str.DEFAULT_TOWN_NAME);
	}

	public static String getNationPrefix() {
		return getString(TownySettings.Str.DEFAULT_NATION_PREFIX);
	}
	
	public static String getTownPrefix(Town town) {
		try {
			return (String)getTownLevel(town).get(TownySettings.TownLevel.NAME_PREFIX);
		} catch (Exception e) {
			// Should not reach here
			return getString(TownySettings.Str.DEFAULT_TOWN_PREFIX);
		}
	}
	
	public static String getCapitalPrefix(Town town) {
		try {
			return (String)getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_PREFIX);
		} catch (NotRegisteredException e) {
			return getString(TownySettings.Str.DEFAULT_CAPITAL_PREFIX);
		}
	}
	
	public static String getKingPostfix(Resident resident) {
		try {
			return (String)getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_POSTFIX);
		} catch (NotRegisteredException e) {
			return getString(TownySettings.Str.DEFAULT_KING_POSTFIX);
		}
	}
	
	public static String getMayorPostfix(Resident resident) {
		try {
			return (String)getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_POSTFIX);
		} catch (NotRegisteredException e) {
			return getString(TownySettings.Str.DEFAULT_MAYOR_POSTFIX);
		}
	}
	
	public static int getWarPointsForTownBlock() {
		return getInt(TownySettings.Int.WARTIME_POINTS_TOWNBLOCK);
	}
	
	public static int getWarPointsForTown() {
		return getInt(TownySettings.Int.WARTIME_POINTS_TOWN);
	}
	
	public static int getWarPointsForNation() {
		return getInt(TownySettings.Int.WARTIME_POINTS_NATION);
	}
	
	public static int getWarPointsForKill() {
		return getInt(TownySettings.Int.WARTIME_POINTS_KILL);
	}

	public static String[] getWarTimeScoreMsg(Town town, int n) {
		return parseString(String.format(getString(TownySettings.Str.MSG_WAR_SCORE), town.getName(), n));
	}
	
	public static int getMinWarHeight() {
		return getInt(TownySettings.Int.WARTIME_MIN_HEIGHT);
	}
	
	public static String[] getCouldntPayTaxesMsg(TownyObject obj, String reaction) {
		return parseString(String.format(getString(TownySettings.Str.MSG_COULDNT_PAY_TAXES), obj.getName(), reaction));
	}
	
	public static String[] getDelResidentMsg(Resident resident) {
		return parseString(String.format(getString(TownySettings.Str.MSG_DEL_RESIDENT), resident.getName()));
	}
	
	public static String[] getDelTownMsg(Town town) {
		return parseString(String.format(getString(TownySettings.Str.MSG_DEL_TOWN), town.getName()));
	}
	
	public static String[] getDelNationMsg(Nation nation) {
		return parseString(String.format(getString(TownySettings.Str.MSG_DEL_NATION), nation.getName()));
	}
	
	public static List<String> getMobRemovalEntities() {
		return getStrArr(TownySettings.StrArr.MOB_REMOVAL_ENTITIES);
	}
	
	public static int getMobRemovalSpeed() {
		return getInt(TownySettings.Int.MOB_REMOVAL_SPEED);
	}
	
	public static boolean isRemovingMobs() {
		return getBoolean(TownySettings.Bool.MOB_REMOVAL);
	}
	
	public static int getHealthRegenSpeed() {
		return getInt(TownySettings.Int.HEALTH_REGEN_SPEED);
	}
	
	public static boolean hasHealthRegen() {
		return getBoolean(TownySettings.Bool.HEALTH_REGEN);
	}
	
	public static String[] getBuyResidentPlotMsg(String who, String owner) {
		return parseString(String.format(getString(TownySettings.Str.MSG_BUY_RESIDENT_PLOT), who, owner));
	}
	
	public static String[] getPlotForSaleMsg(String who, WorldCoord worldCoord) {
		return parseString(String.format(getString(TownySettings.Str.MSG_PLOT_FOR_SALE), who, worldCoord.toString()));
	}
	
	public static String[] getMayorAbondonMsg() {
		return parseString(getString(TownySettings.Str.MSG_MAYOR_ABADON));
	}
	
	public static String getNotPermToNewTownLine() {
		return parseSingleLineString(getString(TownySettings.Str.LINE_NOT_PERM_TO_NEW_TOWN));
	}
	
	public static String getNotPermToNewNationLine() {
		return parseSingleLineString(getString(TownySettings.Str.LINE_NOT_PERM_TO_NEW_NATION));
	}
	
	public static boolean hasTownLimit() {
		return getInt(TownySettings.Int.TOWN_LIMIT) == 0;
	}
	
	public static int getTownLimit() {
		return getInt(TownySettings.Int.TOWN_LIMIT);
	}
	
	public static int getNationNeutralityCost() {
		return getInt(TownySettings.Int.PRICE_NATION_NEUTRALITY);
	}
	
	public static boolean isAllowingOutposts() {
		return getBoolean(TownySettings.Bool.ALLOW_OUTPOSTS);
	}
	
	public static double getOutpostCost() {
		return getDouble(TownySettings.Doub.PRICE_OUTPOST);
	} 
	
	public static List<Integer> getSwitchIds() {
		return getIntArr(TownySettings.IntArr.SWITCH_IDS);
	}
	
	public static List<Integer> getUnclaimedZoneIgnoreIds() {
		return getIntArr(TownySettings.IntArr.UNCLAIMED_ZONE_IGNORE);
	}
	
	public static List<Integer> getItemUseIds() {
		return getIntArr(TownySettings.IntArr.ITEM_USE_IDS);
	}
	
	public static boolean isUnclaimedZoneIgnoreId(int id) {
		return getIntArr(TownySettings.IntArr.UNCLAIMED_ZONE_IGNORE).contains(id);
	}
	
	public static boolean isSwitchId(int id) {
		return getIntArr(TownySettings.IntArr.SWITCH_IDS).contains(id);
	}
	
	public static boolean isItemUseId(int id) {
		return getIntArr(TownySettings.IntArr.ITEM_USE_IDS).contains(id);
	}
	
	public static void setBoolean(String filepath, TownySettings.Bool key, Boolean value) {
		KeyValueFile kvFile = new KeyValueFile(filepath);
		configBool.put(key, value);
		kvFile.setBoolean(key.toString().toLowerCase(), getBoolean(key));
	}
	
	public static void setString(String filepath, TownySettings.Str key, String value) {
		KeyValueFile kvFile = new KeyValueFile(filepath);
		configStr.put(key, value);
		kvFile.setString(key.toString().toLowerCase(), getString(key));
	}

	public static String getNPCPrefix() {
		return getString(TownySettings.Str.NPC_PREFIX);
	}

	public static double getClaimPrice() {
		return getDouble(TownySettings.Doub.PRICE_CLAIM_TOWNBLOCK);
	}

	public static boolean getUnclaimedZoneSwitchRights() {
		return getBoolean(TownySettings.Bool.UNCLAIMED_ZONE_SWITCH);
	}

	public static String getUnclaimedPlotName() {
		return getString(TownySettings.Str.UNCLAIMED_PLOT_NAME);
	}

	public static long getDayInterval() {
		return getLong(TownySettings.KeyLong.DAY_INTERVAL);
	}
	
	public static boolean isAllowingTownSpawn() {
		return getBoolean(TownySettings.Bool.ALLOW_TOWN_SPAWN);
	}
	
	public static boolean isAllowingTownSpawnTravel() {
		return getBoolean(TownySettings.Bool.ALLOW_TOWN_SPAWN_TRAVEL);
	}
	
	public static boolean isTaxingDaily() {
		return getBoolean(TownySettings.Bool.DAILY_TAXES);
	}
	
	public static boolean isBackingUpDaily() {
		return getBoolean(TownySettings.Bool.DAILY_BACKUPS);
	}
	
	public static double getTownSpawnTravelPrice() {
		return getDouble(TownySettings.Doub.PRICE_TOWN_SPAWN_TRAVEL);
	}
	
	public static double getBaseSpoilsOfWar() {
		return getDouble(TownySettings.Doub.WARTIME_BASE_SPOILS);
	}
	
	public static double getWartimeDeathPrice() {
		return getDouble(TownySettings.Doub.WARTIME_DEATH_PRICE);
	}
	
	public static double getDeathPrice() {
		return getDouble(TownySettings.Doub.DEATH_PRICE);
	}
	
	public static double getWartimeTownBlockLossPrice() {
		return getDouble(TownySettings.Doub.WARTIME_TOWN_BLOCK_LOSS_PRICE);
	}
	
	public static boolean isDevMode() {
		return getBoolean(TownySettings.Bool.DEV_MODE);
	}
	
	public static boolean isPvEWithinNonPvPZones() {
		return getBoolean(TownySettings.Bool.PVE_IN_NON_PVP_TOWNS);
	}

	public static boolean isRemovingOnMonarchDeath() {
		return getBoolean(TownySettings.Bool.WARTIME_REMOVE_ON_MONARCH_DEATH);
	}

	public static double getTownUpkeepCost() {
		return getDouble(TownySettings.Doub.PRICE_TOWN_UPKEEP);
	}
	
	public static double getNationUpkeepCost() {
		return getDouble(TownySettings.Doub.PRICE_NATION_UPKEEP);
	}
	
	public static String getFlatFileBackupType() {
		return getString(TownySettings.Str.FLATFILE_BACKUP);
	}
	
	public static boolean isForcingPvP() {
		return getBoolean(TownySettings.Bool.FORCE_PVP_ON);
	}

	public static boolean isTownRespawning() {
		return getBoolean(TownySettings.Bool.TOWN_RESPAWN);
	}
	
	public static boolean isTownyUpdating(String currentVersion) {
		if (isTownyUpToDate(currentVersion))
			return false;
		else
			return true; //Assume
	}
	
	public static boolean isTownyUpToDate(String currentVersion) {
		return currentVersion.equals(getLastRunVersion());
	}

	public static String getLastRunVersion() {
		return getString(TownySettings.Str.LAST_RUN_VERSION);
	}
}
