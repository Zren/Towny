package com.shade.bukkit.towny;

import java.util.HashMap;

import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

//TODO: Admin/Group Build Rights

/*
 * Logic:
 * 
 * Check Cache -> if need to update
 * Update Cache by
 * check townblock with
 * wild permissions
 * is there a plot owner? otherwise skip to town permissions
 * is plot owner
 * is plot friend
 * plot owner allow allies? if so check town permissions
 * is the townblock owned by a town?
 * is town resident
 * has town part of town's nation
 * is nation's ally
 * 
 * TODO: wartime allows enemy nations to edit
 * 
 */

public class TownyBlockListener extends BlockListener {
	private final Towny plugin;

	private HashMap<String, CachedPermission> cachedPermissions = new HashMap<String, CachedPermission>();

	public TownyBlockListener(Towny instance) {
		plugin = instance;
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		if (event.getDamageLevel() == BlockDamageLevel.BROKEN) {
			long start = System.currentTimeMillis();

			onBlockBreakEvent(event, true);

			if (plugin.getTownyUniverse().getSettings().getDebug())
				System.out.println("[Towny] Debug: onBlockBreakEvent took " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	public void onBlockBreakEvent(BlockDamageEvent event, boolean firstCall) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Coord pos = Coord.parseCoord(block);

		// Check cached permissions first
		try {
			if (cachedPermissions.containsKey(player.getName())) {
				if (!getCache(player.getName(), pos).getDestroyPermission())
					event.setCancelled(true);
				return;
			}
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old destroy permission was null, update it
				updateBuildCache(player, pos, true);
				onBlockBreakEvent(event, false);
			} else
				plugin.sendErrorMsg(player, "Error updating build permissions cache.");
		}

	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		long start = System.currentTimeMillis();

		onBlockPlaceEvent(event, true);

		if (plugin.getTownyUniverse().getSettings().getDebug())
			System.out.println("[Towny] Debug: onBlockPlacedEvent took " + (System.currentTimeMillis() - start) + "ms");
	}

	public void onBlockPlaceEvent(BlockPlaceEvent event, boolean firstCall) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Coord pos = Coord.parseCoord(block);

		// Check cached permissions first
		try {
			if (cachedPermissions.containsKey(player.getName())) {
				if (!getCache(player.getName(), pos).getBuildPermission()) {
					event.setBuild(false);
					event.setCancelled(true);
				}
				return;
			}
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old build permission was null, update it
				updateBuildCache(player, pos, true);
				onBlockPlaceEvent(event, false);
			} else
				plugin.sendErrorMsg(player, "Error updating build permissions cache.");
		}
	}

	public void updatePlayerCache(Player player) {
		Coord pos = Coord.parseCoord(player);
		updateBuildCache(player, pos, false);
		updateDestroyCache(player, pos, false);
	}

	public void updateDestroyCache(Player player, Coord pos, boolean sendMsg) {
		TownyUniverse universe = plugin.getTownyUniverse();
		TownySettings settings = universe.getSettings();
		TownBlock townBlock;
		Town town;

		try {
			townBlock = universe.getWorld(player.getWorld().getName()).getTownBlock(pos);
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
			// Unclaimed Zone destroy rights
			if (!settings.getUnclaimedZoneDestroyRights()) {
				// TODO: Have permission to destroy here
				plugin.sendErrorMsg(player, "Not allowed to destroy in the wild.");
				cacheDestroy(player.getName(), pos, false);
			}

			return;
		}

		try {
			Resident resident = universe.getResident(player.getName());
			
			// War Time destroy rights
			if (universe.isWarTime())
				try {
					if (!resident.getTown().getNation().isNeutral() && !town.getNation().isNeutral()) {
						cacheDestroy(player.getName(), pos, true);
						return;
					}	
				} catch (NotRegisteredException e) {
				}
			
			// Resident Plot destroy rights
			try {
				Resident owner = townBlock.getResident();
				if (resident == owner)
					cacheDestroy(player.getName(), pos, true);
				else if (owner.hasFriend(resident)) {
					if (owner.permissions.residentDestroy)
						cacheDestroy(player.getName(), pos, true);
					else {
						if (sendMsg)
							plugin.sendErrorMsg(player, "Owner doesn't allow friends to destroy here.");
						cacheDestroy(player.getName(), pos, false);
					}
				} else if (owner.permissions.allyDestroy)
					// Exit out and use town permissions
					throw new TownyException();
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player, "Owner doesn't allow allies to destroy here.");
					cacheDestroy(player.getName(), pos, false);
				}

				return;
			} catch (NotRegisteredException x) {
			} catch (TownyException x) {
			}

			// Town resident destroy rights
			if (!resident.hasTown())
				throw new TownyException("You don't belong to this town.");

			if (!town.getPermissions().residentDestroy) {
				if (sendMsg)
					plugin.sendErrorMsg(player, "Residents aren't allowed to build.");
				cacheDestroy(player.getName(), pos, false);
			} else if (resident.getTown() != town) {
				// Allied destroy rights
				if (universe.isAlly(resident.getTown(), town) && town.getPermissions().allyDestroy)
					cacheDestroy(player.getName(), pos, true);
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player, "Not allowed to build here.");
					cacheDestroy(player.getName(), pos, false);
				}
			} else
				cacheDestroy(player.getName(), pos, true);
		} catch (TownyException e) {
			// Outsider destroy rights
			if (!town.getPermissions().outsiderDestroy) {
				if (sendMsg)
					plugin.sendErrorMsg(player, e.getError());
				cacheDestroy(player.getName(), pos, false);
			} else
				cacheDestroy(player.getName(), pos, true);
		}
	}

	public void updateBuildCache(Player player, Coord pos, boolean sendMsg) {
		TownyUniverse universe = plugin.getTownyUniverse();
		TownySettings settings = universe.getSettings();
		TownBlock townBlock;
		Town town;

		try {
			townBlock = universe.getWorld(player.getWorld().getName())
					.getTownBlock(pos);
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
			// Unclaimed Zone Build Rights
			if (!settings.getUnclaimedZoneBuildRights()) {
				// TODO: Have permission to build here
				if (sendMsg)
					plugin.sendErrorMsg(player,
							"Not allowed to build in the wild.");
				cacheBuild(player.getName(), pos, false);
			}

			return;
		}

		try {
			Resident resident = universe.getResident(player.getName());

			// War Time build rights
			if (universe.isWarTime())
				try {
					if (!resident.getTown().getNation().isNeutral() && !town.getNation().isNeutral()) {
						cacheBuild(player.getName(), pos, true);
						return;
					}	
				} catch (NotRegisteredException e) {
				}
			
			// Resident Plot rights
			try {
				Resident owner = townBlock.getResident();
				if (resident == owner)
					cacheBuild(player.getName(), pos, true);
				else if (owner.hasFriend(resident)) {
					if (owner.permissions.residentBuild)
						cacheBuild(player.getName(), pos, true);
					else {
						if (sendMsg)
							plugin.sendErrorMsg(player,
									"Owner doesn't allow friends to destroy here.");
						cacheBuild(player.getName(), pos, false);
					}
				} else if (owner.permissions.allyBuild)
					// Exit out and use town permissions
					throw new TownyException();
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player,
								"Owner doesn't allow allies to build here.");
					cacheBuild(player.getName(), pos, false);
				}

				return;
			} catch (NotRegisteredException x) {
			} catch (TownyException x) {
			}

			// Town Resident Build Rights
			if (!resident.hasTown())
				throw new TownyException("You don't belong to this town.");

			if (!town.getPermissions().residentBuild) {
				if (sendMsg)
					plugin.sendErrorMsg(player,
							"Residents aren't allowed to build.");
				cacheBuild(player.getName(), pos, false);
			} else if (resident.getTown() != town) {
				// Allied Build Rights
				if (universe.isAlly(resident.getTown(), town) && town
						.getPermissions().allyBuild)
					cacheBuild(player.getName(), pos, true);
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player,
								"Not allowed to build here.");
					cacheBuild(player.getName(), pos, false);
				}
			} else
				cacheBuild(player.getName(), pos, true);
		} catch (TownyException e) {
			// Outsider Build Rights
			if (!town.getPermissions().outsiderBuild) {
				if (sendMsg)
					plugin.sendErrorMsg(player, e.getError());
				cacheBuild(player.getName(), pos, false);
			} else
				cacheBuild(player.getName(), pos, true);
		}
	}

	public void cacheBuild(String name, Coord coord, boolean buildRight) {
		CachedPermission cache = getCache(name, coord);
		cache.setBuildPermission(buildRight);

		if (plugin.getTownyUniverse().getSettings().getDebug())
			System.out.println("[Towny] Debug: " + name + " ("
					+ coord.toString() + ") Cached Build: " + buildRight);
	}

	public void cacheDestroy(String name, Coord coord, boolean destroyRight) {
		CachedPermission cache = getCache(name, coord);
		cache.setDestroyPermission(destroyRight);

		if (plugin.getTownyUniverse().getSettings().getDebug())
			System.out.println("[Towny] Debug: " + name + " ("
					+ coord.toString() + ") Cached Destroy: " + destroyRight);
	}

	public void clearCache() {
		cachedPermissions.clear();

		if (plugin.getTownyUniverse().getSettings().getDebug())
			System.out.println("[Towny] Debug: Build/Destroy Cache Cleared");
	}

	public void updateCache(Player player) {
		Coord coord = Coord.parseCoord(player);
		cacheBuild(player.getName(), coord, false);
		cacheDestroy(player.getName(), coord, false);
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