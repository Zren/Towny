package ca.xshade.bukkit.towny.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.Coord;
import ca.xshade.bukkit.towny.object.TownBlock;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.object.TownyWorld;

public class TownyEntityListener extends EntityListener {
	private final Towny plugin;

	public TownyEntityListener(Towny instance) {
		plugin = instance;
	}
	
	
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled())
			return;
		
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent)event;
			Entity attacker = entityEvent.getDamager();
			Entity defender = entityEvent.getEntity();

			if (defender instanceof Player)
				if (preventDamageCall(attacker, defender))
					event.setCancelled(true);
				else if (attacker instanceof Player) {
					long start = System.currentTimeMillis();
					
					Player a = (Player) attacker;
					Player b = (Player) defender;
					if (preventFriendlyFire(a, b))
						event.setCancelled(true);
					
					plugin.sendDebugMsg("onEntityDamagedByEntity took " + (System.currentTimeMillis() - start) + "ms");
				}
		}
		
	}

	public boolean preventDamageCall(Entity a, Entity b) {
		TownyUniverse universe = plugin.getTownyUniverse();
		
		try {
			
			TownyWorld world = universe.getWorld(a.getWorld().getName());
			// World using Towny
			if (!world.isUsingTowny())
				return false;
			// World PvP
			if (!world.isPvP())
				return true;
			
			// Check Town PvP status
			Coord key = Coord.parseCoord(a);
			TownBlock townblock = world.getTownBlock(key);
			if (!townblock.getTown().isPVP() && !universe.isWarTime())
				return true;
		} catch (Exception e) {
		}
		return false;
	}
	
	public boolean preventFriendlyFire(Player a, Player b) {
		TownyUniverse universe = plugin.getTownyUniverse();
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
