package com.shade.bukkit.towny;

import org.bukkit.entity.Player;

import com.shade.bukkit.towny.object.Coord;

public class PlayerCache {
	private Coord lastTownBlock;
	private Boolean buildPermission, destroyPermission, switchPermission;
	//TODO: cache last entity attacked

	public PlayerCache(Player player) {
		this(Coord.parseCoord(player));
	}
	
	public PlayerCache(Coord lastTownBlock) {
		this.setLastTownBlock(lastTownBlock);
	}

	/**
	 * Update the cache with new coordinates. Reset the other cached permissions.
	 * @param lastTownBlock
	 */
	
	public void setLastTownBlock(Coord lastTownBlock) {
		reset();
		this.lastTownBlock = lastTownBlock;
	}

	public Coord getLastTownBlock() {
		return lastTownBlock;
	}

	public void setBuildPermission(boolean buildPermission) {
		this.buildPermission = buildPermission;
	}

	public boolean getBuildPermission() throws NullPointerException {
		if (buildPermission == null)
			throw new NullPointerException();
		else
			return buildPermission;
	}

	public void setDestroyPermission(boolean destroyPermission) {
		this.destroyPermission = destroyPermission;
	}

	public boolean getDestroyPermission() throws NullPointerException {
		if (destroyPermission == null)
			throw new NullPointerException();
		else
			return destroyPermission;
	}
	
	public void setSwitchPermission(boolean switchPermission) {
		this.switchPermission = switchPermission;
	}
	
	public boolean getSwitchPermission() throws NullPointerException {
		if (switchPermission == null)
			throw new NullPointerException();
		else
			return switchPermission;
	}
	
	public boolean updateCoord(Coord pos) {
		if (!getLastTownBlock().equals(pos)) {
			setLastTownBlock(pos);
			return true;
		} else
			return false;
	}
	
	private void reset() {
		lastTownBlock = null;
		buildPermission = null;
		destroyPermission = null;
		switchPermission = null;
	}
	
	public enum TownBlockStatus {
		UNKOWN,
		ADMIN,
		UNCLAIMED_ZONE,
		WARTIME,
		OUTSIDER,
		PLOT_OWNER,
		PLOT_FRIEND,
		PLOT_ALLY,
		TOWN_OWNER,
		TOWN_RESIDENT,
		TOWN_ALLY
	};
	
	private TownBlockStatus townBlockStatus = TownBlockStatus.UNKOWN;

	public void setStatus(TownBlockStatus townBlockStatus) {
		this.townBlockStatus = townBlockStatus;
	}
	
	public TownBlockStatus getStatus() {
		return townBlockStatus;
	}
}
