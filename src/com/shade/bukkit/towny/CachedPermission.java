package com.shade.bukkit.towny;

public class CachedPermission {
	private Coord lastTownBlock;
	private Boolean buildPermission, destroyPermission;

	public CachedPermission(Coord lastTownBlock) {
		this.setLastTownBlock(lastTownBlock);
	}

	public void setLastTownBlock(Coord lastTownBlock) {
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
		if (buildPermission == null)
			throw new NullPointerException();
		else
			return destroyPermission;
	}

	public void newCache(Coord lastTownBlock) {
		this.setLastTownBlock(lastTownBlock);
		buildPermission = null;
		destroyPermission = null;
	}
}
