package com.shade.bukkit.towny.war;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;

import org.bukkit.entity.Player;

import com.shade.bukkit.towny.Nation;
import com.shade.bukkit.towny.NotRegisteredException;
import com.shade.bukkit.towny.Town;
import com.shade.bukkit.towny.TownBlock;
import com.shade.bukkit.towny.Towny;
import com.shade.bukkit.towny.TownySettings;
import com.shade.bukkit.towny.TownyUniverse;
import com.shade.bukkit.towny.WorldCoord;
import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;
import com.shade.bukkit.util.ServerBroadCastTimerTask;
import com.shade.util.KeyValue;
import com.shade.util.KeyValueTable;
import com.shade.util.TimeMgmt;

//TODO: Extend a new class called TownyEvent
public class War {
	private Hashtable<WorldCoord,Integer> warZone = new Hashtable<WorldCoord,Integer>(); 
	private Hashtable<Town,Integer> townScores = new Hashtable<Town,Integer>();
	private List<Town> warringTowns = new ArrayList<Town>();
	private List<Nation> warringNations = new ArrayList<Nation>();
	private Towny plugin;
	private TownyUniverse universe;
	private boolean warTime = false;
	private Timer warTimer = new Timer();
	
	public War(Towny plugin, int startDelay) {
		this.plugin = plugin;
		this.universe = plugin.getTownyUniverse();
		
		setupDelay(startDelay);
	}

	public void setWarTimer(Timer warTimer) {
		this.warTimer = warTimer;
	}

	public Timer getWarTimer() {
		return warTimer;
	}
	
	public Towny getPlugin() {
		return plugin;
	}

	public void setPlugin(Towny plugin) {
		this.plugin = plugin;
	}
	
	public void setupDelay(int delay) {
		if (delay <= 0)
			start();
		else {
			for (Long t : TimeMgmt.getCountdownDelays(delay, TimeMgmt.defaultCountdownDelays))
				//Schedule the warnings leading up to the start of the war event
				warTimer.schedule(
						new ServerBroadCastTimerTask(plugin,
								String.format("War starts in %s", TimeMgmt.formatCountdownTime(t))),
								(delay-t)*1000);
			warTimer.schedule(new StartWarTimerTask(universe), delay*1000);
		}
	}

	public boolean isWarTime() {
		return warTime;
	}
	
	public TownyUniverse getTownyUniverse() {
		return universe;
	}

	public void start() {
		warTime = true;
		
		//Announce
		
		//Gather all nations at war
		for (Nation nation : universe.getNations())
			if (!nation.isNeutral())
				add(nation);
		warTimer.scheduleAtFixedRate(new WarTimerTask(this), 0, 1000);
		checkEnd();
	}
	
	public void add(Nation nation) {
		for (Town town : nation.getTowns())
			add(town);
		warringNations.add(nation);
	}
	
	public void add(Town town) {
		universe.sendTownMessage(town, TownySettings.getJoinWarMsg(town));
		townScores.put(town, 0);
		warringTowns.add(town);
		for (TownBlock townBlock : town.getTownBlocks())
			if (town.isHomeBlock(townBlock))
				warZone.put(townBlock.getWorldCoord(), TownySettings.getWarzoneHomeBlockHealth());
			else
				warZone.put(townBlock.getWorldCoord(), TownySettings.getWarzoneTownBlockHealth());
	}

	public boolean isWarZone(WorldCoord worldCoord) {
		return warZone.containsKey(worldCoord);
	}

	public void townScored(Town town, int n) {
		townScores.put(town, townScores.get(town) + n);
		universe.sendGlobalMessage(TownySettings.getWarTimeScoreMsg(town, n));
	}
	
	public void damage(Town attacker, TownBlock townBlock) throws NotRegisteredException {
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int hp = warZone.get(worldCoord) - 1;
		if (hp > 0) {
			warZone.put(worldCoord, hp);
			universe.sendTownMessage(townBlock.getTown(), Colors.Red + "["+townBlock.getTown().getName()+"]("+townBlock.getCoord().toString()+") HP: "+hp);
		} else
			remove(attacker, townBlock);
	}
	
	public void remove(Town attacker, TownBlock townBlock) throws NotRegisteredException {
		townScored(attacker, TownySettings.getWarPointsForTownBlock());
		if (townBlock.getTown().isHomeBlock(townBlock))
			remove(townBlock.getTown());
		else
			remove(townBlock.getWorldCoord());
	}
	
	public void remove(TownBlock townBlock) throws NotRegisteredException {
		if (townBlock.getTown().isHomeBlock(townBlock))
			remove(townBlock.getTown());
		else
			remove(townBlock.getWorldCoord());
	}
	
	public void eliminate(Town town) {
		remove(town);
		try {
			checkNation(town.getNation());
		} catch (NotRegisteredException e) {
			plugin.sendErrorMsg("[War] Error checking "+town.getName()+"'s nation.");
		}
		universe.sendGlobalMessage(TownySettings.getWarTimeEliminatedMsg(town.getName()));
		checkEnd();
	}
	
	public void eliminate(Nation nation) {
		remove(nation);
		universe.sendGlobalMessage(TownySettings.getWarTimeEliminatedMsg(nation.getName()));
		checkEnd();
	}
	
	public void nationLeave(Nation nation) {
		remove(nation);
		for (Town town : nation.getTowns())
			remove(town);
		universe.sendGlobalMessage(TownySettings.getWarTimeForfeitMsg(nation.getName()));
		checkEnd();
	}
	
	public void townLeave(Town town) {
		remove(town);
		universe.sendGlobalMessage(TownySettings.getWarTimeForfeitMsg(town.getName()));
		checkEnd();
	}
	
	public void remove(Town attacker, Nation nation) {
		townScored(attacker, TownySettings.getWarPointsForNation());
		warringNations.remove(nation);
	}
	
	public void remove(Nation nation) {
		warringNations.remove(nation);
	}
	
	
	public void remove(Town attacker, Town town) throws NotRegisteredException {
		townScored(attacker, TownySettings.getWarPointsForTown());
		
		for (TownBlock townBlock : town.getTownBlocks())
			remove(townBlock.getWorldCoord());
		warringTowns.remove(town);
		try {
			if (!townsLeft(town.getNation()))
				remove(town.getNation());
		} catch (NotRegisteredException e) {
		}
	}
	
	public void remove(Town town) {
		for (TownBlock townBlock : town.getTownBlocks())
			remove(townBlock.getWorldCoord());
		warringTowns.remove(town);
		try {
		if (!townsLeft(town.getNation()))
			remove(town.getNation());
		} catch (NotRegisteredException e) {
		}
	}
	
	public boolean townsLeft(Nation nation) {
		return warringTowns.containsAll(nation.getTowns());
	}
	
	public void remove(WorldCoord worldCoord) {
		try {
			Town town = worldCoord.getTownBlock().getTown();
			universe.sendGlobalMessage(TownySettings.getWarTimeLoseTownBlockMsg(worldCoord, town.getName()));
			warZone.remove(worldCoord);
		} catch (NotRegisteredException e) {
			universe.sendGlobalMessage(TownySettings.getWarTimeLoseTownBlockMsg(worldCoord));
			warZone.remove(worldCoord);
		}
		
	}
	
	public void checkEnd() {
		if (warringNations.size() <= 1)
			end();
	}
	
	public void checkTown(Town town) {
		if (countActiveWarBlocks(town) == 0)
			eliminate(town);
	}
	
	public void checkNation(Nation nation) {
		if (countActiveTowns(nation) == 0)
			eliminate(nation);
	}
	
	public int countActiveWarBlocks(Town town) {
		int n = 0;
		for (TownBlock townBlock : town.getTownBlocks())
			if (warZone.containsKey(townBlock.getWorldCoord()))
				n++;
		return n;
	}
	
	public int countActiveTowns(Nation nation) {
		int n = 0;
		for (Town town : nation.getTowns())
			if (warringTowns.contains(town))
				n++;
		return n;
	}
	
	public void end() {
		warTime = false;
	}
	
	public void sendStats(Player player) {
		player.sendMessage(ChatTools.formatTitle("War Stats"));
		player.sendMessage(Colors.Green + "  Nations: " + Colors.LightGreen + warringNations.size());
		player.sendMessage(Colors.Green + "  Towns: " + Colors.LightGreen + warringTowns.size() +" / " + townScores.size());
		player.sendMessage(Colors.Green + "  WarZone: " + Colors.LightGreen + warZone.size() + " Town blocks");
	}
	
	public void sendScores(Player player) {
		sendScores(player, 10);
	}
	
	public void sendScores(Player player, int maxListing) {
		player.sendMessage(ChatTools.formatTitle("War - Top Scores"));
		KeyValueTable kvTable = new KeyValueTable(townScores);
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue kv : kvTable.getKeyValues()) {
			n++;
			if (n > maxListing)
				break;
			Town town = (Town)kv.key;
			player.sendMessage(String.format(
					Colors.Blue + "%40s "+Colors.Gold+"|"+Colors.LightGray+" %4d",
					universe.getFormatter().getFormattedName(town),
					(Integer)kv.value));
		}
	}
	
	public boolean isWarringNation(Nation nation) {
		return warringNations.contains(nation);
	}
}
