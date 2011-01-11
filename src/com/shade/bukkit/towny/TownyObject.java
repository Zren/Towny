package com.shade.bukkit.towny;

public abstract class TownyObject {
	private String name;
	
	//abstract boolean load();
	//abstract boolean save();
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
}
