package ca.xshade.bukkit.towny;

import org.bukkit.entity.Player;

import ca.xshade.bukkit.towny.object.Coord;
import ca.xshade.bukkit.towny.object.TownyWorld;
import ca.xshade.bukkit.towny.object.WorldCoord;

public class PlayerCache {
	private WorldCoord lastTownBlock;
	private Boolean buildPermission, destroyPermission, switchPermission;
	private String blockErrMsg;
	//TODO: cache last entity attacked

	public PlayerCache(TownyWorld world, Player player) {
		this(new WorldCoord(world, Coord.parseCoord(player)));
	}
	
	public PlayerCache(WorldCoord lastTownBlock) {
		this.setLastTownBlock(lastTownBlock);
	}

	/**
	 * Update the cache with new coordinates. Reset the other cached permissions.
	 * @param lastTownBlock
	 */
	
	public void setLastTownBlock(WorldCoord lastTownBlock) {
		reset();
		this.lastTownBlock = lastTownBlock;
	}

	public WorldCoord getLastTownBlock() {
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
	
	public boolean updateCoord(WorldCoord pos) {
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
		blockErrMsg = null;
	}
	
	public enum TownBlockStatus {
		UNKOWN,
		ADMIN,
		UNCLAIMED_ZONE,
		WARZONE,
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
	
	public TownBlockStatus getStatus() throws NullPointerException {
		if (townBlockStatus == null)
			throw new NullPointerException();
		else
			return townBlockStatus;
	}

	public void setBlockErrMsg(String blockErrMsg) {
		this.blockErrMsg = blockErrMsg;
	}

	public String getBlockErrMsg() {
		String temp = blockErrMsg;
		setBlockErrMsg(null); // Delete error msg after reading it.
		return temp;
	}
	
	public boolean hasBlockErrMsg() {
		return blockErrMsg != null;
	}
}
