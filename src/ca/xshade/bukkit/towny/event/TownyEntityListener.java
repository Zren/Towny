package ca.xshade.bukkit.towny.event;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

import ca.xshade.bukkit.towny.MobRemovalTimerTask;
import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownyException;
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
			//long start = System.currentTimeMillis();
			
			EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent)event;
			Entity attacker = entityEvent.getDamager();
			Entity defender = entityEvent.getEntity();

			TownyUniverse universe = plugin.getTownyUniverse();
			try {
				TownyWorld world = universe.getWorld(defender.getWorld().getName());
				
				// Wartime
				if (universe.isWarTime()) {
					event.setCancelled(false);
					throw new Exception();
				}
				
				Player a = null;
				Player b = null;
				
				if (attacker instanceof Player)
					a = (Player) attacker;
				if (defender instanceof Player)
					b = (Player) defender;
				
				if (preventDamageCall(world, attacker, defender, a, b))
					event.setCancelled(true);
			} catch (Exception e) {
			}
			
			
			//plugin.sendDebugMsg("onEntityDamagedByEntity took " + (System.currentTimeMillis() - start) + "ms");
		}
		
	}

	
	
	public boolean preventDamageCall(TownyWorld world, Entity a, Entity b, Player ap, Player bp) {
		// World using Towny
		if (!world.isUsingTowny())
			return false;
		
		if (ap != null && bp != null)
			if (preventDamagePvP(world, ap, bp) || preventFriendlyFire(ap, bp))
				return true;
		
		
		try {
			// Check Town PvP status
			Coord key = Coord.parseCoord(a);
			TownBlock townblock = world.getTownBlock(key);
			if (!townblock.getTown().isPVP())
				if (bp != null && (ap != null || a instanceof Arrow))
					return true;
				else if (!TownySettings.isPvEWithinNonPvPZones()) // TODO: Allow EvE >.>
					return true;
		} catch (NotRegisteredException e) {
		}
		
		return false;
	}
	
	public boolean preventDamagePvP(TownyWorld world, Player a, Player b) {
		// Universe is only PvP
		if (TownySettings.isForcingPvP())
			return false;
		
		// World PvP
		if (!world.isPvP())
			return true;
		
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
	
	@Override
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (TownySettings.isRemovingMobs() && event.getEntity() instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity)event.getEntity();
			Location loc = event.getLocation();
			if (MobRemovalTimerTask.isRemovingEntity(livingEntity)) {
				Coord coord = Coord.parseCoord(loc);
				try {
					TownyWorld townyWorld = plugin.getTownyUniverse().getWorld(loc.getWorld().getName());
					TownBlock townBlock = townyWorld.getTownBlock(coord);
					if (!townBlock.getTown().hasMobs())
						//plugin.sendDebugMsg("onCreatureSpawn: Canceled " + event.getCreatureType() + " from spawning within "+coord.toString()+".");
						event.setCancelled(true);
				} catch (TownyException x) {
				}
			}
		}
	}
}
