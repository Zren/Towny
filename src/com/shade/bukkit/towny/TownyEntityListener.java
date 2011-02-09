package com.shade.bukkit.towny;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

public class TownyEntityListener extends EntityListener {
	private final Towny plugin;

	public TownyEntityListener(Towny instance) {
		plugin = instance;
	}

	@Override
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		Entity defender = event.getEntity();

		if (attacker instanceof Player && defender instanceof Player) {
			long start = System.currentTimeMillis();
			
			Player a = (Player) attacker;
			Player b = (Player) defender;
			if (preventDamageCall(a, b))
				event.setCancelled(true);
			
			if (TownySettings.getDebug())
				System.out.println("[Towny] Debug: onEntityDamagedByEntity took " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	public boolean preventDamageCall(Player a, Player b) {
		TownyUniverse universe = plugin.getTownyUniverse();

		// Check Town PvP status
		try {
			if (universe.isWarTime())
				throw new Exception();
			
			TownyWorld world = universe.getWorld(a.getWorld().getName());
			Coord key = Coord.parseCoord(a);
			TownBlock townblock = world.getTownBlock(key);

			if (!townblock.getTown().isPVP())
				return true;
		} catch (Exception e) {
		}

		// Check Allies
		if (!TownySettings.getFriendlyFire() && universe.isAlly(a.getName(), b.getName()))
			return true;

		return false;
	}
	
	
	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		Entity entity =  event.getEntity();
		if (TownySettings.getDebug())
			System.out.println("[Towny] Debug: onDeath: " + entity.getEntityId());
		
		if (entity instanceof Player) {
			Player player = (Player)entity;
			plugin.getTownyUniverse().townSpawn(player, true);
		}
    }
}
