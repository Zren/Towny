package com.shade.bukkit.towny;

public class DailyTimerTask extends TownyTimerTask {
	public DailyTimerTask(TownyUniverse universe) {
		super(universe);
	}

	@Override
	public void run() {
		universe.sendGlobalMessage(TownySettings.getNewDayMsg());
		
		// Collect taxes
		try {
			universe.collectTownTaxes();
			universe.collectNationTaxes();
		} catch (IConomyException e) {
		}

		// Automatically delete old residents
		if (TownySettings.isDeletingOldResidents())
			for (Resident resident : universe.getResidents())
				if (System.currentTimeMillis() - resident.getLastOnline() > TownySettings.getMaxInactivePeriod()) {
					// TODO: Delete resident
				}

	}

}
