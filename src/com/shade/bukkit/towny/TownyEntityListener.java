package com.shade.bukkit.towny;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityListener;

public class TownyEntityListener extends EntityListener  {
	private final Towny plugin;

	public TownyEntityListener(Towny instance) {
		plugin = instance;
	}
	
	public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		Entity defender = event.getEntity();
		
		
		if (attacker instanceof Player && defender instanceof Player) {
			Player a = (Player)attacker;
			Player b = (Player)defender;
			if (preventDamageCall(a, b)) {
				event.setCancelled(true);
			}
		}
	}
	
	public boolean preventDamageCall(Player a, Player b) {
		TownyUniverse universe = plugin.getTownyUniverse();
		TownySettings settings = universe.getSettings(); 
		
		// Check Town PvP status
		try {
			TownyWorld world = universe.getWorld(a.getWorld().getName());
			Coord key = Coord.parseCoord(a);
			TownBlock townblock = world.getTownBlock(key);
			
			if (!townblock.getTown().isPVP())
				return true;
		} catch (Exception e) {}
		
		// Check Allies
		if (!settings.getFriendlyFire() && universe.isAlly(a.getName(), b.getName()))
			return true;
		
		return false;
	}
}
