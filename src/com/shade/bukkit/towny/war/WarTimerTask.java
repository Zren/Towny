package com.shade.bukkit.towny.war;

import org.bukkit.entity.Player;

import com.shade.bukkit.towny.NotRegisteredException;
import com.shade.bukkit.towny.TownySettings;
import com.shade.bukkit.towny.TownyTimerTask;
import com.shade.bukkit.towny.object.Coord;
import com.shade.bukkit.towny.object.Nation;
import com.shade.bukkit.towny.object.Resident;
import com.shade.bukkit.towny.object.TownBlock;
import com.shade.bukkit.towny.object.WorldCoord;

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
			warEvent.end();
			warEvent.getWarTimer().cancel();
			universe.setWarEvent(null);
			universe.getPlugin().updateCache();
			if (TownySettings.getDebug())
				System.out.println("[Towny] [War] Debug: End");
			return;
		}
		
		int numPlayers = 0;
		for (Player player : universe.getOnlinePlayers()) {
			numPlayers += 1;
			System.out.println("[Towny] [War] Debug: "+player.getName()+": ");
			try {
				Resident resident = universe.getResident(player.getName());
				if (resident.hasNation()) {
					Nation nation = resident.getTown().getNation();
					System.out.println("[Towny] [War] Debug: hasNation");
					if (nation.isNeutral())
						continue;
					System.out.println("[Towny] [War] Debug: notNeutral");
					if (!warEvent.isWarringNation(nation))
						continue;
					System.out.println("[Towny] [War] Debug: warringNation");
					//TODO: Cache player coord & townblock
					
					WorldCoord worldCoord = new WorldCoord(universe.getWorld(player.getWorld().getName()), Coord.parseCoord(player));
					if (!warEvent.isWarZone(worldCoord))
						continue;
					System.out.println("[Towny] [War] Debug: warZone");
					if (player.getLocation().getBlockY() < TownySettings.getMinWarHeight())
						continue;
					System.out.println("[Towny] [War] Debug: aboveMinHeight");
					TownBlock townBlock = worldCoord.getTownBlock(); //universe.getWorld(player.getWorld().getName()).getTownBlock(worldCoord);
					if (townBlock.getTown().getNation().hasAlly(nation))
						continue;
					System.out.println("[Towny] [War] Debug: notAlly");
					//Enemy nation
					warEvent.damage(resident.getTown(), townBlock);
					System.out.println("[Towny] [War] Debug: damaged");
				}
			} catch(NotRegisteredException e) {
				continue;
			}
		}
		
		if (TownySettings.getDebug())
			System.out.println("[Towny] [War] Debug: # Players: " + numPlayers);
	}
}
