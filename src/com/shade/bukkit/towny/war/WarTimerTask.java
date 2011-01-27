package com.shade.bukkit.towny.war;

import org.bukkit.entity.Player;

import com.shade.bukkit.towny.Coord;
import com.shade.bukkit.towny.Nation;
import com.shade.bukkit.towny.NotRegisteredException;
import com.shade.bukkit.towny.Resident;
import com.shade.bukkit.towny.TownBlock;
import com.shade.bukkit.towny.TownySettings;
import com.shade.bukkit.towny.TownyTimerTask;
import com.shade.bukkit.towny.WorldCoord;

public class WarTimerTask extends TownyTimerTask {
	War warEvent;
	
	public WarTimerTask(War warEvent) {
		super(warEvent.getTownyUniverse());
		this.warEvent = warEvent;
	}

	@Override
	public void run() {
		//TODO: check if war has ended and end gracefully
		if (!warEvent.isWarTime()) {
			for (Player player : universe.getOnlinePlayers())
				warEvent.sendStats(player);
			warEvent.getWarTimer().cancel();
			universe.setWarEvent(null);
			universe.getPlugin().updateCache();
			if (TownySettings.getDebug())
				System.out.println("[Towny] [War] Debug: End");
			return;
		}
		
		int numPlayers = 0;
		for (Player player : universe.getOnlinePlayers()) {
			numPlayers++;
			try {
				Resident resident = universe.getResident(player.getName());
				if (resident.hasNation()) {
					Nation nation = resident.getTown().getNation();
					if (nation.isNeutral())
						continue;
					
					//TODO: Cache player coord & townblock
					
					WorldCoord worldCoord = new WorldCoord(universe.getWorld(player.getWorld().getName()), Coord.parseCoord(player));
					if (!warEvent.isWarZone(worldCoord))
						continue;
					
					TownBlock townBlock = universe.getWorld(player.getWorld().getName()).getTownBlock(worldCoord);
					if (townBlock.getTown().getNation().hasAlly(nation))
						continue;
					
					//Enemy nation
					warEvent.damage(townBlock);
				}
			} catch(NotRegisteredException e) {
				continue;
			}
		}
		
		if (TownySettings.getDebug()) {
			System.out.println("[Towny] [War] Debug: # Players: " + numPlayers);
			System.out.println("[Towny] [War] Debug: # Towns: " + numPlayers);
		}
	}
}
