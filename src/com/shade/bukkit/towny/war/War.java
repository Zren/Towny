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
import com.shade.bukkit.towny.TownyException;
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
	private TownySettings settings;
	private boolean warTime = false;
	private Timer warTimer = new Timer();
	
	public War(Towny plugin, long startDelay) {
		this.plugin = plugin;
		this.universe = plugin.getTownyUniverse();
		this.settings = universe.getSettings();
		
		getWarTimer().scheduleAtFixedRate(new WarTimerTask(this), settings.getWarTimeWarningDelay(), 1000);
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
	
	public void setupDelay(int delay) throws TownyException {
		if (delay <= 0)
			start();
		else {
			for (Long t : TimeMgmt.getCountdownDelays(delay, TimeMgmt.defaultCountdownDelays))
				//Schedule the warnings leading up to the start of the war event
				warTimer.schedule(
						new ServerBroadCastTimerTask(plugin,
								String.format("War starts in %s", TimeMgmt.formatCountdownTime(t))),
								delay);
			warTimer.schedule(new StartWarTimerTask(universe), delay);
		}
	}
	
	public void endWar() {
		this.warTime = false;
		warTimer.cancel();
	}

	public boolean isWarTime() {
		return warTime;
	}
	
	public TownyUniverse getTownyUniverse() {
		return universe;
	}

	public void start() {
		warTimer.cancel();
		
		//Announce
		
		//Gather all nations at war
		for (Nation nation : universe.getNations())
			if (!nation.isNeutral())
				add(nation);
		warTimer.scheduleAtFixedRate(new WarTimerTask(this), 0, 1000);
	}
	
	public void add(Nation nation) {
		for (Town town : nation.getTowns())
			add(town);
	}
	
	public void add(Town town) {
		universe.sendTownMessage(town, settings.getJoinWarMsg(town));
		townScores.put(town, 0);
		for (TownBlock townBlock : town.getTownBlocks())
			if (town.isHomeBlock(townBlock))
				warZone.put(townBlock.getWorldCoord(), settings.getWarzoneHomeBlockHealth());
			else
				warZone.put(townBlock.getWorldCoord(), settings.getWarzoneTownBlockHealth());
	}

	public boolean isWarZone(WorldCoord worldCoord) {
		return warZone.containsKey(worldCoord);
	}

	public void damage(TownBlock townBlock) throws NotRegisteredException {
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int hp = warZone.get(worldCoord) - 1;
		if (hp > 0)
			warZone.put(worldCoord, hp);
		else
			remove(townBlock);
	}

	public void remove(TownBlock townBlock) throws NotRegisteredException {
		if (townBlock.getTown().isHomeBlock(townBlock))
			remove(townBlock.getTown());
		else
			remove(townBlock.getWorldCoord());
	}
	
	public void eliminate(Town town) {
		remove(town);
		universe.sendGlobalMessage(settings.getWarTimeEliminatedMsg(town.getName()));
		checkEnd();
	}
	
	public void nationLeave(Nation nation) {
		remove(nation);
		for (Town town : nation.getTowns())
			remove(town);
		universe.sendGlobalMessage(settings.getWarTimeForfeitMsg(nation.getName()));
		checkEnd();
	}
	
	public void townLeave(Town town) {
		remove(town);
		universe.sendGlobalMessage(settings.getWarTimeForfeitMsg(town.getName()));
		checkEnd();
	}
	
	public void remove(Nation nation) {
		warringNations.remove(nation);
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
		warZone.remove(worldCoord);
	}
	
	public void checkEnd() {
		if (warringNations.size() <= 1)
			end();
	}
	
	public void end() {
		warTimer.cancel();
		universe.setWarEvent(null);
		for (Player player : plugin.getServer().getOnlinePlayers())
			sendStats(player);
	}
	
	public void sendStats(Player player) {
		player.sendMessage(ChatTools.formatTitle("War Stats"));
		KeyValueTable kvTable = new KeyValueTable(townScores);
		kvTable.sortByValue();
		kvTable.revese();
		for (KeyValue kv : kvTable.getKeyValues()) {
			Town town = (Town)kv.key;
			player.sendMessage(String.format(
					Colors.Blue + "%24s "+Colors.Gold+"|"+Colors.LightGray+" %4d",
					universe.getFormatter().getFormattedName(town),
					(Integer)kv.value));
		}
	}
}
