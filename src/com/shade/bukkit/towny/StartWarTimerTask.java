package com.shade.bukkit.towny;

public class StartWarTimerTask extends TownyTimerTask {

	public StartWarTimerTask(TownyUniverse universe) {
		super(universe);
	}

	@Override
	public void run() {
		universe.getWarTimer().scheduleAtFixedRate(new WarTimerTask(universe), universe.getSettings().getWarTimeWarningDelay(), 1000);
	}

}
