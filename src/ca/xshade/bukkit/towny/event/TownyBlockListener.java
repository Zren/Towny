package ca.xshade.bukkit.towny.event;

import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockInteractEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.PlayerCache;
import ca.xshade.bukkit.towny.PlayerCache.TownBlockStatus;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.Coord;
import ca.xshade.bukkit.towny.object.TownyPermission;
import ca.xshade.bukkit.towny.object.TownyWorld;
import ca.xshade.bukkit.towny.object.WorldCoord;

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
		if (event.isCancelled())
			return;
		
		if (event.getDamageLevel() == BlockDamageLevel.STOPPED) {
			long start = System.currentTimeMillis();

			onBlockBreakEvent(event, true);

			plugin.sendDebugMsg("onBlockBreakEvent took " + (System.currentTimeMillis() - start) + "ms");
		}
	}
	
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
			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);
			TownBlockStatus status = cache.getStatus();
			if (status == TownBlockStatus.UNCLAIMED_ZONE && hasWildOverride(worldCoord.getWorld(), player, event.getBlock().getTypeId()))
				return;
			if (!cache.getDestroyPermission())
				event.setCancelled(true);
			if (cache.hasBlockErrMsg())
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old destroy permission was null, update it
				TownBlockStatus status = plugin.cacheStatus(player, worldCoord, plugin.getStatusCache(player, worldCoord));
				plugin.cacheDestroy(player, worldCoord, getDestroyPermission(player, status, worldCoord));
				onBlockBreakEvent(event, false);
			} else
				plugin.sendErrorMsg(player, "Error updating destroy permissions cache.");
		}
	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled())
			return;
		
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
			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);
			TownBlockStatus status = cache.getStatus();
			if (status == TownBlockStatus.UNCLAIMED_ZONE && hasWildOverride(worldCoord.getWorld(), player, event.getBlock().getTypeId()))
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
				TownBlockStatus status = plugin.cacheStatus(player, worldCoord, plugin.getStatusCache(player, worldCoord));
				plugin.cacheBuild(player, worldCoord, getBuildPermission(player, status, worldCoord));
				onBlockPlaceEvent(event, false, errMsg);
			} else
				plugin.sendErrorMsg(player, "Error updating build permissions cache.");
		}
	}
	
	@Override
	public void onBlockInteract(BlockInteractEvent event) {
		if (event.isCancelled())
			return;
		
		long start = System.currentTimeMillis();

		//onBlockInteractEvent(event, true, null);

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
				PlayerCache cache = plugin.getCache(player);
				cache.updateCoord(worldCoord);
				TownBlockStatus status = cache.getStatus();
				if (status == TownBlockStatus.UNCLAIMED_ZONE && hasWildOverride(worldCoord.getWorld(), player, event.getBlock().getTypeId()))
					return;
				if (!cache.getSwitchPermission())
					event.setCancelled(true);
				if (cache.hasBlockErrMsg())
					plugin.sendErrorMsg(player, cache.getBlockErrMsg());
				return;
			} catch (NullPointerException e) {
				if (firstCall) {
					// New or old build permission was null, update it
					TownBlockStatus status = plugin.cacheStatus(player, worldCoord, plugin.getStatusCache(player, worldCoord));
					plugin.cacheSwitch(player, worldCoord, getSwitchPermission(player, status, worldCoord));
					onBlockInteractEvent(event, false, errMsg);
				} else
					plugin.sendErrorMsg(player, "Error updating switch permissions cache.");
			}
		}
	}

	public boolean getBuildPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.BUILD);
	}
	
	public boolean getDestroyPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.DESTROY);
	}
	
	public boolean getSwitchPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.SWITCH);
	}
	
	public boolean hasWildOverride(TownyWorld world, Player player, int blockId) {
		return world.isUnclaimedZoneIgnoreId(blockId) || plugin.hasPermission(player, "towny.wild.block." + blockId);
	}
}