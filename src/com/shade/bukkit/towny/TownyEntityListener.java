package com.shade.bukkit.towny;

import org.bukkit.Entity;
import org.bukkit.Player;
import org.bukkit.event.entity.EntityDamagedByEntityEvent;
import org.bukkit.event.player.PlayerListener;

public class TownyEntityListener extends PlayerListener  {
	private final Towny plugin;

	public TownyEntityListener(Towny instance) {
		plugin = instance;
	}
	
	public void onEntityDamagedByEntity(EntityDamagedByEntityEvent event) {
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
		Coord key = Coord.parseCoord(
				settings,
				a.getLocation().getBlockX(),
				a.getLocation().getBlockZ());
		try {
			TownyWorld world = universe.getWorld(a.getWorld().getName());
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
