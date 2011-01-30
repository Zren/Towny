package com.shade.bukkit.towny;

import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

//TODO: Admin/Group Build Rights
//TODO: algorithm is updating coord twice when updating permissions 

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

	public TownyBlockListener(Towny instance) {
		plugin = instance;
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		if (event.getDamageLevel() == BlockDamageLevel.BROKEN) {
			long start = System.currentTimeMillis();

			onBlockBreakEvent(event, true);

			if (TownySettings.getDebug())
				System.out.println("[Towny] Debug: onBlockBreakEvent took " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	public void onBlockBreakEvent(BlockDamageEvent event, boolean firstCall) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Coord pos = Coord.parseCoord(block);

		// Check cached permissions first
		try {
			PlayerCache cache = getCache(player);
			cache.updateCoord(pos);
			if (!cache.getDestroyPermission())
				event.setCancelled(true);
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old destroy permission was null, update it
				updateDestroyCache(player, pos, true);
				onBlockBreakEvent(event, false);
			} else
				plugin.sendErrorMsg(player, "Error updating destroy permissions cache.");
		}

	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		long start = System.currentTimeMillis();

		onBlockPlaceEvent(event, true);

		if (TownySettings.getDebug())
			System.out.println("[Towny] Debug: onBlockPlacedEvent took " + (System.currentTimeMillis() - start) + "ms");
	}

	public void onBlockPlaceEvent(BlockPlaceEvent event, boolean firstCall) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Coord pos = Coord.parseCoord(block);

		// Check cached permissions first
		try {
			PlayerCache cache = getCache(player);
			cache.updateCoord(pos);
			if (!cache.getBuildPermission()) { // If build cache is empty, throws null pointer
				event.setBuild(false);
				event.setCancelled(true);
			}
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old build permission was null, update it
				updateBuildCache(player, pos, true);
				onBlockPlaceEvent(event, false);
			} else
				plugin.sendErrorMsg(player, "Error updating build permissions cache.");
		}
	}

	

	public void updateDestroyCache(Player player, Coord pos, boolean sendMsg) {
		if (plugin.isTownyAdmin(player)) {
			cacheDestroy(player, pos, true);
			return;
		}
		
		TownyUniverse universe = plugin.getTownyUniverse();
		TownBlock townBlock;
		Town town;
		try {
			townBlock = universe.getWorld(player.getWorld().getName()).getTownBlock(pos);
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
			// Unclaimed Zone destroy rights
			if (!TownySettings.getUnclaimedZoneDestroyRights()) {
				// TODO: Have permission to destroy here
				if (sendMsg)
					plugin.sendErrorMsg(player, "Not allowed to destroy in the wild.");
				cacheDestroy(player, pos, false);
			} else
				cacheDestroy(player, pos, true);

			return;
		}

		try {
			Resident resident = universe.getResident(player.getName());
			
			// War Time destroy rights
			if (universe.isWarTime())
				try {
					if (!resident.getTown().getNation().isNeutral() && !town.getNation().isNeutral()) {
						cacheDestroy(player, pos, true);
						return;
					}	
				} catch (NotRegisteredException e) {
				}
			
			// Resident Plot destroy rights
			try {
				Resident owner = townBlock.getResident();
				if (resident == owner)
					cacheDestroy(player, pos, true);
				else if (owner.hasFriend(resident)) {
					if (owner.permissions.residentDestroy)
						cacheDestroy(player, pos, true);
					else {
						if (sendMsg)
							plugin.sendErrorMsg(player, "Owner doesn't allow friends to destroy here.");
						cacheDestroy(player, pos, false);
					}
				} else if (owner.permissions.allyDestroy)
					// Exit out and use town permissions
					throw new TownyException();
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player, "Owner doesn't allow allies to destroy here.");
					cacheDestroy(player, pos, false);
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
					plugin.sendErrorMsg(player, "Residents aren't allowed to destroy.");
				cacheDestroy(player, pos, false);
			} else if (resident.getTown() != town) {
				// Allied destroy rights
				if (universe.isAlly(resident.getTown(), town) && town.getPermissions().allyDestroy)
					cacheDestroy(player, pos, true);
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player, "Not allowed to destroy here.");
					cacheDestroy(player, pos, false);
				}
			} else
				cacheDestroy(player, pos, true);
		} catch (TownyException e) {
			// Outsider destroy rights
			if (!town.getPermissions().outsiderDestroy) {
				if (sendMsg)
					plugin.sendErrorMsg(player, e.getError());
				cacheDestroy(player, pos, false);
			} else
				cacheDestroy(player, pos, true);
		}
	}

	public void updateBuildCache(Player player, Coord pos, boolean sendMsg) {
		if (plugin.isTownyAdmin(player)) {
			cacheBuild(player, pos, true);
			return;
		}
		
		TownyUniverse universe = plugin.getTownyUniverse();
		TownBlock townBlock;
		Town town;

		try {
			townBlock = universe.getWorld(player.getWorld().getName()).getTownBlock(pos);
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
			// Unclaimed Zone Build Rights
			if (!TownySettings.getUnclaimedZoneBuildRights()) {
				// TODO: Have permission to build here
				if (sendMsg)
					plugin.sendErrorMsg(player, "Not allowed to build in the wild.");
				cacheBuild(player, pos, false);
			} else
				cacheBuild(player, pos, true);

			return;
		}

		try {
			Resident resident = universe.getResident(player.getName());

			// War Time build rights
			if (universe.isWarTime())
				try {
					if (!resident.getTown().getNation().isNeutral() && !town.getNation().isNeutral()) {
						cacheBuild(player, pos, true);
						return;
					}	
				} catch (NotRegisteredException e) {
				}
			
			// Resident Plot rights
			try {
				Resident owner = townBlock.getResident();
				if (resident == owner)
					cacheBuild(player, pos, true);
				else if (owner.hasFriend(resident)) {
					if (owner.permissions.residentBuild)
						cacheBuild(player, pos, true);
					else {
						if (sendMsg)
							plugin.sendErrorMsg(player, "Owner doesn't allow friends to build here.");
						cacheBuild(player, pos, false);
					}
				} else if (owner.permissions.allyBuild)
					// Exit out and use town permissions
					throw new TownyException();
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player, "Owner doesn't allow allies to build here.");
					cacheBuild(player, pos, false);
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
					plugin.sendErrorMsg(player, "Residents aren't allowed to build.");
				cacheBuild(player, pos, false);
			} else if (resident.getTown() != town) {
				// Allied Build Rights
				if (universe.isAlly(resident.getTown(), town) && town.getPermissions().allyBuild)
					cacheBuild(player, pos, true);
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player, "Not allowed to build here.");
					cacheBuild(player, pos, false);
				}
			} else
				cacheBuild(player, pos, true);
		} catch (TownyException e) {
			// Outsider Build Rights
			if (!town.getPermissions().outsiderBuild) {
				if (sendMsg)
					plugin.sendErrorMsg(player, e.getError());
				cacheBuild(player, pos, false);
			} else
				cacheBuild(player, pos, true);
		}
	}
	
	public void cacheBuild(Player player, Coord coord, boolean buildRight) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(coord);
		cache.setBuildPermission(buildRight);

		if (TownySettings.getDebug())
			System.out.println("[Towny] Debug: " + player.getName() + " (" + coord.toString() + ") Cached Build: " + buildRight);
	}

	public void cacheDestroy(Player player, Coord coord, boolean destroyRight) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(coord);
		cache.setDestroyPermission(destroyRight);

		if (TownySettings.getDebug())
			System.out.println("[Towny] Debug: " + player.getName() + " (" + coord.toString() + ") Cached Destroy: " + destroyRight);
	}
	
	public PlayerCache getCache(Player player) {
		return plugin.getCache(player);
	}
}