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
			
			universe.collectNationCosts();
		} catch (IConomyException e) {
		}

		// Automatically delete old residents
		if (TownySettings.isDeletingOldResidents())
			for (Resident resident : universe.getResidents())
				if (System.currentTimeMillis() - resident.getLastOnline() > TownySettings.getMaxInactivePeriod())
					universe.removeResident(resident);
		if (TownySettings.getDebug())
			System.out.println("[Towny] Debug: New Day");
	}

}
