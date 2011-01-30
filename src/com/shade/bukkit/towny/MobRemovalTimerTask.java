package com.shade.bukkit.towny;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;

public class MobRemovalTimerTask extends TownyTimerTask {
	private Server server;
	
	public MobRemovalTimerTask(TownyUniverse universe, Server server) {
		super(universe);
		this.server = server;
	}

	@Override
	public void run() {
		int numRemoved = 0;
		
		for (World world : server.getWorlds())
			for (Object e : getWorldEntities(world))
				if (TownySettings.getMobRemovalEntities().contains(e.getClass().getName())) {
					Entity entity = (Entity)e;
					Location loc = entity.getLocation();
					Coord coord = Coord.parseCoord(loc);
					try {
						TownyWorld townyWorld = universe.getWorld(world.getName());
						TownBlock townBlock = townyWorld.getTownBlock(coord);
						if (!townBlock.getTown().hasMobs()) {
							entity.teleportTo(new Location(world, loc.getX(), -50, loc.getZ()));
							numRemoved++;
						}
					} catch (TownyException x) {
					}
				}
		if (TownySettings.getDebug())
			System.out.println("[Towny] Debug: MobRemoval (Removed: "+numRemoved+")");
	}
	
	@SuppressWarnings("rawtypes")
	public final List getWorldEntities(World world) {
		CraftWorld w = (CraftWorld)world;
        return w.getHandle().b;
    }
}
