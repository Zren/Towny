package com.shade.bukkit.towny.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

import com.shade.bukkit.towny.Towny;
import com.shade.bukkit.towny.TownySettings;
import com.shade.bukkit.towny.object.Coord;
import com.shade.bukkit.towny.object.TownBlock;
import com.shade.bukkit.towny.object.TownyUniverse;
import com.shade.bukkit.towny.object.TownyWorld;

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
			
			plugin.sendDebugMsg("onEntityDamagedByEntity took " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	public boolean preventDamageCall(Player a, Player b) {
		TownyUniverse universe = plugin.getTownyUniverse();
		
		try {
			// World PvP
			TownyWorld world = universe.getWorld(a.getWorld().getName());
			if (!world.isPvP())
				return true;
			
			// Check Town PvP status
			Coord key = Coord.parseCoord(a);
			TownBlock townblock = world.getTownBlock(key);
			if (!townblock.getTown().isPVP() && !universe.isWarTime())
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
		
		if (entity instanceof Player) {
			Player player = (Player)entity;
			plugin.sendDebugMsg("onPlayerDeath: " + player.getName() + "[ID: " + entity.getEntityId() + "]");
		}
    }
}
