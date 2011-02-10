package com.shade.bukkit.towny.war;

import com.shade.bukkit.towny.TownyTimerTask;
import com.shade.bukkit.towny.TownyUniverse;

public class StartWarTimerTask extends TownyTimerTask {

	public StartWarTimerTask(TownyUniverse universe) {
		super(universe);
	}

	@Override
	public void run() {
		universe.getWarEvent().start();
	}

}
