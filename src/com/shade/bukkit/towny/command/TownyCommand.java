package com.shade.bukkit.towny.command;

import org.bukkit.command.Command;

import com.shade.bukkit.towny.TownyUniverse;

public abstract class TownyCommand extends Command {
	
	public TownyCommand(String name) {
		super(name);
	}

	protected static TownyUniverse universe;
	
	public static void setUniverse(TownyUniverse u) {
		universe = u;
	}
}
