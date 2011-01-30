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
		for (World world : server.getWorlds())
			for (Object e : getWorldEntities(world)) {
				Entity entity = (Entity)e;
				Location loc = entity.getLocation();
				Coord coord = Coord.parseCoord(loc);
				if (TownySettings.getMobRemovalEntities().contains(e.getClass().getName()))
					try {
						TownyWorld townyWorld = universe.getWorld(world.getName());
						TownBlock townBlock = townyWorld.getTownBlock(coord);
						if (!townBlock.getTown().hasMobs())
							entity.teleportTo(new Location(world, loc.getX(), -50, loc.getZ()));
					} catch (TownyException x) {
					}
			}
	}
	
	@SuppressWarnings("rawtypes")
	public final List getWorldEntities(World world) {
		CraftWorld w = (CraftWorld)server.getWorlds()[(int)world.getId()];
        return w.getHandle().b;
    }
}
