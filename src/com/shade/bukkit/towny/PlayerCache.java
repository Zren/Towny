package com.shade.bukkit.towny;

import org.bukkit.entity.Player;

public class PlayerCache {
	private Coord lastTownBlock;
	private Boolean buildPermission, destroyPermission;
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
	}
}
