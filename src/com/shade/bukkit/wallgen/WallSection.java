package com.shade.bukkit.wallgen;

import org.bukkit.Location;

public class WallSection {
	int r, t;
	Location p;

	public WallSection(Location p, int r, int t) {
		this.p = p;
		this.r = r;
		this.t = t;
	}
}