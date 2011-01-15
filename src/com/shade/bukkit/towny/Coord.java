package com.shade.bukkit.towny;

import org.bukkit.Player;

public class Coord {
	private int x, z;
	
	public Coord(int x, int z) {
		this.x = x;
		this.z = z;
	}
	
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public int hashCode() {
		int result = 17;
		result = 31 * result + x;
		result = 31 * result + z;
		return result;
	}
	
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Coord))
			return false;
		
		Coord o = (Coord)obj;
		return this.x == o.x
			&& this.z == o.z;
	}
	
	public static Coord parseCoord(TownySettings settings, int x, int z) {
		return new Coord(
				x / settings.getTownBlockSize() - (x < 0 ? 1 : 0),
				z / settings.getTownBlockSize() - (z < 0 ? 1 : 0));
	}
	
	public static Coord parseCoord(TownySettings settings, Player player) {
		return parseCoord(settings, player.getLocation().getBlockX(), player.getLocation().getBlockZ());
	}
	
	public String toString() {
		return getX() + "," + getZ();
	}
}
