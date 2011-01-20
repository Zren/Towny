package com.shade.bukkit.util;

import org.bukkit.World;

class Job {
	private String boss;
	private boolean notify;
	private World world;

	public Job(String boss, World world) {
		this(boss, world, true);
	}

	public Job(String boss, World world, boolean notify) {
		this.setBoss(boss);
		this.setWorld(world);
		this.setNotify(notify);
	}

	public void setBoss(String boss) {
		this.boss = boss;
	}

	public String getBoss() {
		return boss;
	}

	public void setNotify(boolean notify) {
		this.notify = notify;
	}

	public boolean isNotify() {
		return notify;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public World getWorld() {
		return world;
	}
}
