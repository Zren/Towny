package com.shade.bukkit.towny;

public class Coord {
	int x, z;
	
	public Coord(int x, int z) {
		this.x = x;
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
}
