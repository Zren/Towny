package com.shade.bukkit.towny.event;

import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockInteractEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.shade.bukkit.towny.NotRegisteredException;
import com.shade.bukkit.towny.PlayerCache;
import com.shade.bukkit.towny.PlayerCache.TownBlockStatus;
import com.shade.bukkit.towny.Towny;
import com.shade.bukkit.towny.TownyException;
import com.shade.bukkit.towny.TownySettings;
import com.shade.bukkit.towny.object.Coord;
import com.shade.bukkit.towny.object.Resident;
import com.shade.bukkit.towny.object.Town;
import com.shade.bukkit.towny.object.TownBlock;
import com.shade.bukkit.towny.object.TownyPermission;
import com.shade.bukkit.towny.object.TownyUniverse;
import com.shade.bukkit.towny.object.WorldCoord;

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

	public TownyBlockListener(Towny instance) {
		plugin = instance;
	}

	
	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		if (event.getDamageLevel() == BlockDamageLevel.BROKEN) {
			long start = System.currentTimeMillis();

			onBlockBreakEvent(event, true);

			plugin.sendDebugMsg("onBlockBreakEvent took " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	/*
	 * Old method
	 * 
	
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
				updateStatusCache(player, pos);
				updateDestroyCache(player, pos, true);
				onBlockBreakEvent(event, false);
			} else
				plugin.sendErrorMsg(player, "Error updating destroy permissions cache.");
		}
	}
	*/
	
	public void onBlockBreakEvent(BlockDamageEvent event, boolean firstCall) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		WorldCoord worldCoord;
		try {
			worldCoord = new WorldCoord(plugin.getTownyUniverse().getWorld(block.getWorld().getName()), Coord.parseCoord(block));
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, "This world has not been configured by Towny.");
			event.setCancelled(true);
			return;
		}

		// Check cached permissions first
		try {
			PlayerCache cache = getCache(player);
			cache.updateCoord(worldCoord);
			TownBlockStatus status = cache.getStatus();
			if (status == TownBlockStatus.UNCLAIMED_ZONE && TownySettings.isUnclaimedZoneIgnoreId(event.getBlock().getTypeId()))
				return;
			if (!cache.getDestroyPermission())
				event.setCancelled(true);
			if (cache.hasBlockErrMsg())
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old destroy permission was null, update it
				TownBlockStatus status = cacheStatus(player, worldCoord, getStatusCache(player, worldCoord));
				cacheDestroy(player, worldCoord, getDestroyPermission(player, status, worldCoord));
				onBlockBreakEvent(event, false);
			} else
				plugin.sendErrorMsg(player, "Error updating destroy permissions cache.");
		}
	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		long start = System.currentTimeMillis();

		onBlockPlaceEvent(event, true, null);

		plugin.sendDebugMsg("onBlockPlacedEvent took " + (System.currentTimeMillis() - start) + "ms");
	}

	public void onBlockPlaceEvent(BlockPlaceEvent event, boolean firstCall, String errMsg) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		WorldCoord worldCoord;
		try {
			worldCoord = new WorldCoord(plugin.getTownyUniverse().getWorld(block.getWorld().getName()), Coord.parseCoord(block));
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, "This world has not been configured by Towny.");
			event.setCancelled(true);
			return;
		}

		// Check cached permissions first
		try {
			PlayerCache cache = getCache(player);
			cache.updateCoord(worldCoord);
			TownBlockStatus status = cache.getStatus();
			if (status == TownBlockStatus.UNCLAIMED_ZONE && TownySettings.isUnclaimedZoneIgnoreId(event.getBlock().getTypeId()))
				return;
			if (!cache.getBuildPermission()) { // If build cache is empty, throws null pointer
				event.setBuild(false);
				event.setCancelled(true);
			}
			if (cache.hasBlockErrMsg())
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old build permission was null, update it
				TownBlockStatus status = cacheStatus(player, worldCoord, getStatusCache(player, worldCoord));
				cacheBuild(player, worldCoord, getBuildPermission(player, status, worldCoord));
				onBlockPlaceEvent(event, false, errMsg);
			} else
				plugin.sendErrorMsg(player, "Error updating build permissions cache.");
		}
	}
	
	@Override
	public void onBlockInteract(BlockInteractEvent event) {
		long start = System.currentTimeMillis();

		onBlockInteractEvent(event, true, null);

		plugin.sendDebugMsg("onBlockInteractEvent took " + (System.currentTimeMillis() - start) + "ms");
	}
	
	public void onBlockInteractEvent(BlockInteractEvent event, boolean firstCall, String errMsg) {
		if (event.getEntity() != null && event.getEntity() instanceof Player) {
			Player player = (Player)event.getEntity();
			Block block = event.getBlock();
			
			if (!TownySettings.isSwitchId(block.getTypeId()))
				return;

			WorldCoord worldCoord;
			try {
				worldCoord = new WorldCoord(plugin.getTownyUniverse().getWorld(block.getWorld().getName()), Coord.parseCoord(block));
			} catch (NotRegisteredException e1) {
				plugin.sendErrorMsg(player, "This world has not been configured by Towny.");
				event.setCancelled(true);
				return;
			}
	
			// Check cached permissions first
			try {
				PlayerCache cache = getCache(player);
				cache.updateCoord(worldCoord);
				TownBlockStatus status = cache.getStatus();
				if (status == TownBlockStatus.UNCLAIMED_ZONE && TownySettings.isUnclaimedZoneIgnoreId(event.getBlock().getTypeId()))
					return;
				if (!cache.getSwitchPermission())
					event.setCancelled(true);
				if (cache.hasBlockErrMsg())
					plugin.sendErrorMsg(player, cache.getBlockErrMsg());
				return;
			} catch (NullPointerException e) {
				if (firstCall) {
					// New or old build permission was null, update it
					TownBlockStatus status = cacheStatus(player, worldCoord, getStatusCache(player, worldCoord));
					cacheSwitch(player, worldCoord, getSwitchPermission(player, status, worldCoord));
					onBlockInteractEvent(event, false, errMsg);
				} else
					plugin.sendErrorMsg(player, "Error updating switch permissions cache.");
			}
		}
	}

	public boolean getBuildPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return getPermission(player, status, pos, TownyPermission.ActionType.BUILD);
	}
	
	public boolean getDestroyPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return getPermission(player, status, pos, TownyPermission.ActionType.DESTROY);
	}
	
	public boolean getSwitchPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return getPermission(player, status, pos, TownyPermission.ActionType.SWITCH);
	}

	public boolean getPermission(Player player, TownBlockStatus status, WorldCoord pos, TownyPermission.ActionType actionType) {
		if (status == TownBlockStatus.ADMIN ||
			status == TownBlockStatus.WARZONE ||
			status == TownBlockStatus.PLOT_OWNER ||
			status == TownBlockStatus.TOWN_OWNER)
				return true;
		
		TownBlock townBlock;
		Town town;
		try {
			townBlock = pos.getTownBlock();
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
			// Wilderness Permissions
			if (status == TownBlockStatus.UNCLAIMED_ZONE)
				if (plugin.hasPermission(player, "towny.wild." + actionType.toString()))
					return true;
				else if (!TownyPermission.getUnclaimedZone(actionType, pos.getWorld())) {
					// TODO: Have permission to destroy here
					cacheBlockErrMsg(player, "Not allowed to " + actionType.toString() + " in the wild.");
					return false;
				} else
					return true;
			else {
				plugin.sendErrorMsg(player, "Error updating destroy permission.");
				return false;
			}
		}
		
		// Plot Permissions
		try {
			Resident owner = townBlock.getResident();
			
			if (status == TownBlockStatus.PLOT_FRIEND) {
				if (owner.getPermissions().getResident(actionType))
					return true;
				else {
					cacheBlockErrMsg(player, "Owner doesn't allow friends to " + actionType.toString() + " here.");
					return false;
				}
			} else if (status == TownBlockStatus.PLOT_ALLY)
				if (owner.getPermissions().getAlly(actionType))
					return true;
				else {
					cacheBlockErrMsg(player, "Owner doesn't allow allies to " + actionType.toString() + " here.");
					return false;
				}
			else if (status == TownBlockStatus.OUTSIDER)
				if (owner.getPermissions().getOutsider(actionType))
					return true;
				else {
					cacheBlockErrMsg(player, "Owner doesn't allow outsiders to " + actionType.toString() + " here.");
					return false;
				}
		} catch (NotRegisteredException x) {
		}
	
		// Town Permissions
		if (status == TownBlockStatus.TOWN_RESIDENT) {
			if (town.getPermissions().getResident(actionType))
				return true;
			else {
				cacheBlockErrMsg(player, "Residents aren't allowed to " + actionType.toString() + ".");
				return false;
			}
		} else if (status == TownBlockStatus.TOWN_ALLY)
			if (town.getPermissions().getAlly(actionType))
				return true;
			else {
				cacheBlockErrMsg(player, "Allies aren't allowed to " + actionType.toString() + " here.");
				return false;
			}
		else if (status == TownBlockStatus.OUTSIDER)
			if (town.getPermissions().getOutsider(actionType))
				return true;
			else {
				cacheBlockErrMsg(player, "Outsiders aren't allowed to " + actionType.toString() + " here.");
				return false;
			}
		
		plugin.sendErrorMsg(player, "Error updating " + actionType.toString() + " permission.");
		return false;
	}	


	/*
	 * Old block algorithm
	 * 
	
	public void updateSwitchCache(Player player, WorldCoord pos, boolean sendMsg) {
		if (plugin.isTownyAdmin(player)) {
			cacheSwitch(player, pos, true);
			return;
		}
		
		TownyUniverse universe = plugin.getTownyUniverse();
		TownBlock townBlock;
		Town town;
		try {
			townBlock = universe.getWorld(player.getWorld().getName()).getTownBlock(pos);
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
			// Unclaimed Zone switch rights
			if (plugin.hasPermission(player, "towny.wild.switch"))
				cacheSwitch(player, pos, true);
			else if (!TownySettings.getUnclaimedZoneSwitchRights()) {
				// TODO: Have permission to switch here
				if (sendMsg)
					plugin.sendErrorMsg(player, "Not allowed to toggle switches in the wild.");
				cacheSwitch(player, pos, false);
			} else
				cacheSwitch(player, pos, true);

			return;
		}

		try {
			Resident resident = universe.getResident(player.getName());
			
			// War Time switch rights
			if (universe.isWarTime())
				try {
					if (!resident.getTown().getNation().isNeutral() && !town.getNation().isNeutral()) {
						cacheSwitch(player, pos, true);
						return;
					}	
				} catch (NotRegisteredException e) {
				}
			
			// Resident Plot switch rights
			try {
				Resident owner = townBlock.getResident();
				if (resident == owner)
					cacheSwitch(player, pos, true);
				else if (owner.hasFriend(resident)) {
					if (owner.getPermissions().residentSwitch)
						cacheSwitch(player, pos, true);
					else {
						if (sendMsg)
							plugin.sendErrorMsg(player, "Owner doesn't allow friends to toggle switches.");
						cacheSwitch(player, pos, false);
					}
				} else if (owner.getPermissions().allySwitch)
					// Exit out and use town permissions
					throw new TownyException();
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player, "Owner doesn't allow allies to toggle switches.");
					cacheSwitch(player, pos, false);
				}

				return;
			} catch (NotRegisteredException x) {
			} catch (TownyException x) {
			}

			// Town resident destroy rights
			if (!resident.hasTown())
				throw new TownyException("You don't belong to this town.");

			if (!town.getPermissions().residentSwitch) {
				if (sendMsg)
					plugin.sendErrorMsg(player, "Residents aren't allowed to toggle switches.");
				cacheSwitch(player, pos, false);
			} else if (resident.getTown() != town) {
				// Allied destroy rights
				if (universe.isAlly(resident.getTown(), town) && town.getPermissions().allyDestroy)
					cacheSwitch(player, pos, true);
				else {
					if (sendMsg)
						plugin.sendErrorMsg(player, "Not allowed to toggle switches here.");
					cacheSwitch(player, pos, false);
				}
			} else
				cacheSwitch(player, pos, true);
		} catch (TownyException e) {
			// Outsider destroy rights
			if (!town.getPermissions().outsiderSwitch) {
				if (sendMsg)
					plugin.sendErrorMsg(player, e.getError());
				cacheSwitch(player, pos, false);
			} else
				cacheSwitch(player, pos, true);
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
			if (plugin.hasPermission(player, "towny.wild.destroy"))
				cacheDestroy(player, pos, true);
			else if (!TownySettings.getUnclaimedZoneDestroyRights()) {
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
					if (owner.getPermissions().residentDestroy)
						cacheDestroy(player, pos, true);
					else {
						if (sendMsg)
							plugin.sendErrorMsg(player, "Owner doesn't allow friends to destroy here.");
						cacheDestroy(player, pos, false);
					}
				} else if (owner.getPermissions().allyDestroy)
					// Exit out and use town getPermissions()
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
	
	public void updateBuildCache(Player player, WorldCoord pos, boolean sendMsg) {
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
			if (plugin.hasPermission(player, "towny.wild.build"))
				cacheBuild(player, pos, true);
			else if (!TownySettings.getUnclaimedZoneBuildRights()) {
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
					if (owner.getPermissions().residentBuild)
						cacheBuild(player, pos, true);
					else {
						if (sendMsg)
							plugin.sendErrorMsg(player, "Owner doesn't allow friends to build here.");
						cacheBuild(player, pos, false);
					}
				} else if (owner.getPermissions().allyBuild)
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
	
	*/

	
	
	public TownBlockStatus getStatusCache(Player player, WorldCoord worldCoord) {
		if (plugin.isTownyAdmin(player))
			return TownBlockStatus.ADMIN;
		
		TownyUniverse universe = plugin.getTownyUniverse();
		TownBlock townBlock;
		Town town;
		try {
			townBlock = worldCoord.getTownBlock();
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
			// Unclaimed Zone switch rights
			return TownBlockStatus.UNCLAIMED_ZONE;
		}

		try {
			Resident resident = universe.getResident(player.getName());
			
			// War Time switch rights
			if (universe.isWarTime())
				try {
					if (!resident.getTown().getNation().isNeutral() && !town.getNation().isNeutral())
						return TownBlockStatus.WARZONE;	
				} catch (NotRegisteredException e) {
				}
			
			// Resident Plot switch rights
			try {
				Resident owner = townBlock.getResident();
				if (resident == owner)
					return TownBlockStatus.PLOT_OWNER;
				else if (owner.hasFriend(resident))
					return TownBlockStatus.PLOT_FRIEND;
				else
					// Exit out and use town permissions
					throw new TownyException();
			} catch (NotRegisteredException x) {
			} catch (TownyException x) {
			}

			// Town resident destroy rights
			if (!resident.hasTown())
				throw new TownyException();

			if (resident.getTown() != town) {
				// Allied destroy rights
				if (universe.isAlly(resident.getTown(), town))
					return TownBlockStatus.TOWN_ALLY;
				else
					return TownBlockStatus.OUTSIDER;
			} else if (resident.isMayor() || resident.getTown().hasAssistant(resident))
				return TownBlockStatus.TOWN_OWNER;
			else
				return TownBlockStatus.TOWN_RESIDENT;
		} catch (TownyException e) {
			// Outsider destroy rights
			return TownBlockStatus.OUTSIDER;
		}
	}
	
	private TownBlockStatus cacheStatus(Player player, WorldCoord worldCoord, TownBlockStatus townBlockStatus) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(worldCoord);
		cache.setStatus(townBlockStatus);

		plugin.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Status: " + townBlockStatus);
		return townBlockStatus;
	}


	public void cacheBuild(Player player, WorldCoord worldCoord, boolean buildRight) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(worldCoord);
		cache.setBuildPermission(buildRight);

		plugin.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Build: " + buildRight);
	}

	public void cacheDestroy(Player player, WorldCoord worldCoord, boolean destroyRight) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(worldCoord);
		cache.setDestroyPermission(destroyRight);

		plugin.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Destroy: " + destroyRight);
	}
	
	public void cacheSwitch(Player player, WorldCoord worldCoord, boolean switchRight) {
		PlayerCache cache = getCache(player);
		cache.updateCoord(worldCoord);
		cache.setSwitchPermission(switchRight);

		plugin.sendDebugMsg(player.getName() + " (" + worldCoord.toString() + ") Cached Switch: " + switchRight);
	}
	
	private void cacheBlockErrMsg(Player player, String msg) {
		PlayerCache cache = getCache(player);
		cache.setBlockErrMsg(msg);
	}
	
	public PlayerCache getCache(Player player) {
		return plugin.getCache(player);
	}
}