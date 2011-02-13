package com.shade.bukkit.towny;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import com.shade.bukkit.towny.object.Coord;
import com.shade.bukkit.towny.object.TownBlock;
import com.shade.bukkit.towny.object.TownyUniverse;
import com.shade.bukkit.towny.object.TownyWorld;

public class MobRemovalTimerTask extends TownyTimerTask {
	private Server server;
	
	public MobRemovalTimerTask(TownyUniverse universe, Server server) {
		super(universe);
		this.server = server;
	}
	
	
	@Override
	public void run() {
		int numRemoved = 0;
		int livingEntities = 0;
		
		for (World world : server.getWorlds()) {
			livingEntities += world.getLivingEntities().size();
			for (LivingEntity livingEntity : world.getLivingEntities())
				if (TownySettings.getMobRemovalEntities().contains(livingEntity.toString())) {
					Location loc = livingEntity.getLocation();
					Coord coord = Coord.parseCoord(loc);
					try {
						TownyWorld townyWorld = universe.getWorld(world.getName());
						TownBlock townBlock = townyWorld.getTownBlock(coord);
						if (!townBlock.getTown().hasMobs()) {
							livingEntity.teleportTo(new Location(world, loc.getX(), -50, loc.getZ()));
							numRemoved++;
						}
					} catch (TownyException x) {
					}
				}
		}
		universe.getPlugin().sendDebugMsg("MobRemoval (Removed: "+numRemoved+") (Total Living: "+livingEntities+")");
	}
}
