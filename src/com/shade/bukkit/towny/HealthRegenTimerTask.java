package com.shade.bukkit.towny;

import org.bukkit.Server;
import org.bukkit.entity.Player;

public class HealthRegenTimerTask extends TownyTimerTask {

private Server server;
	
	public HealthRegenTimerTask(TownyUniverse universe, Server server) {
		super(universe);
		this.server = server;
	}
	
	@Override
	public void run() {
		for (Player player : server.getOnlinePlayers()) {
			Coord coord = Coord.parseCoord(player);
			try {
				TownyWorld world = universe.getWorld(player.getWorld().getName());
				TownBlock townBlock = world.getTownBlock(coord);
					
				if (universe.isAlly(townBlock.getTown(), universe.getResident(player.getName()).getTown()))
					incHealth(player);
			} catch (TownyException x) {
			}
		}
	}
	
	public void incHealth(Player player) {
		int currentHP = player.getHealth();
		if (currentHP < 20)
			player.setHealth(currentHP++);
	}
	
}
