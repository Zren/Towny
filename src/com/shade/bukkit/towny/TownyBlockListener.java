package com.shade.bukkit.towny;

import java.util.HashMap;

import org.bukkit.Block;
import org.bukkit.Player;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlacedEvent;

//TODO: Admin/Group Build Rights

public class TownyBlockListener extends BlockListener  {
	private final Towny plugin;
	
	private HashMap<String,CachedPermission> cachedPermissions = new HashMap<String,CachedPermission>(); 

    public TownyBlockListener(Towny instance) {
        plugin = instance;
    }
    
    //TODO: Updated API uses present tense
    public void onBlockPlaced(BlockPlacedEvent event) {
    	long start = System.currentTimeMillis();
    	
    	onBlockPlacedEvent(event);
    	
    	if (plugin.getTownyUniverse().getSettings().getDebug())
    		System.out.println("[Towny] Debug: onBlockPlacedEvent took " + (System.currentTimeMillis()-start) + "ms");
    }
    public void onBlockPlacedEvent(BlockPlacedEvent event) {
    	Player player = event.getPlayer();
    	Block block = event.getBlock();
    	TownyUniverse universe = plugin.getTownyUniverse();
    	TownySettings settings = universe.getSettings();
    	Coord pos = Coord.parseCoord(block);
    	
    	// Check cached permissions first
    	try {
	    	if (cachedPermissions.containsKey(player.getName())) {
	    		if (!(getCache(player.getName(), pos).getBuildPermission())) {
	    			event.setBuild(false);
					event.setCancelled(true);
	    		}
	    		return;
	    	}
    	} catch (NullPointerException e) { 
    		// New or old build permission was null, update it
    	}
    	
    	TownBlock townBlock;
    	Town town;
    	
    	try {
			townBlock = universe.getWorld(player.getWorld().getName()).getTownBlock(pos);
			town = townBlock.getTown();
    	} catch(NotRegisteredException e) {
    		//Unclaimed Zone Build Rights
			if (!settings.getUnclaimedZoneBuildRights()) {
				//TODO: Have permission to build here
				event.setBuild(false);
				cacheBuild(player.getName(), pos, false);
				event.setCancelled(true);
			}
			
			return;
		}
    	
		try {
			Resident resident = universe.getResident(player.getName());
			if (!resident.hasTown())
				throw new TownyException("You don't belong to this town.");
			
			// Resident Build Rights
			if (!town.getPermissions().residentBuild) {
				plugin.sendErrorMsg(player, "Residents aren't allowed to build.");
				event.setBuild(false);
				cacheBuild(player.getName(), pos, false);
				event.setCancelled(true);
			} else {
				if (resident.getTown() != town) {
					// Allied Build Rights
					if ((universe.isAlly(resident.getTown(), town) && town.getPermissions().allies)) {
						plugin.sendErrorMsg(player, "Not allowed to build here.");
						event.setBuild(false);
						cacheBuild(player.getName(), pos, false);
						event.setCancelled(true);
					} else {
						cacheBuild(player.getName(), pos, true);
					}
				} else {
					cacheBuild(player.getName(), pos, true);
				}
			}
		} catch(TownyException e) {
			// Outsider Build Rights
			if (!town.getPermissions().outsiderBuild) {
				plugin.sendErrorMsg(player, e.getError());
				event.setBuild(false);
				cacheBuild(player.getName(), pos, false);
				event.setCancelled(true);
			} else {
				cacheBuild(player.getName(), pos, true);
			}
		}
    }
    
    public void cacheBuild(String name, Coord coord, boolean buildRight) {
    	CachedPermission cache = getCache(name, coord);
    	cache.setBuildPermission(buildRight);
    	
    	if (plugin.getTownyUniverse().getSettings().getDebug())
    		System.out.println("[Towny] "+name+" ("+coord.toString()+") Cached Build: "+buildRight);
    }
    
    public void cacheDestroy(String name, Coord coord, boolean destroyRight) {
    	CachedPermission cache = getCache(name, coord);
    	cache.setDestroyPermission(destroyRight);
    	
    	if (plugin.getTownyUniverse().getSettings().getDebug())
    		System.out.println("[Towny] "+name+" ("+coord.toString()+") Cached Destroy: "+destroyRight);
    }
    
    public CachedPermission getCache(String name, Coord coord) {
    	if (!cachedPermissions.containsKey(name))
    		cachedPermissions.put(name, new CachedPermission(coord));
    	CachedPermission cache = cachedPermissions.get(name);
    	if (!cache.getLastTownBlock().equals(coord))
    		cache.newCache(coord);
    	return cache;
    }
}