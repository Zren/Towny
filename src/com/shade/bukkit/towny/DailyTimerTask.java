package com.shade.bukkit.towny;

import java.io.IOException;

import com.shade.bukkit.towny.object.Resident;
import com.shade.bukkit.towny.object.TownyUniverse;

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
			universe.collectTownCosts();
			universe.collectNationCosts();
		} catch (IConomyException e) {
		}

		// Automatically delete old residents
		if (TownySettings.isDeletingOldResidents())
			for (Resident resident : universe.getResidents())
				if (System.currentTimeMillis() - resident.getLastOnline() > TownySettings.getMaxInactivePeriod())
					universe.removeResident(resident);
		
		try {
			universe.getDataSource().backup();
		} catch (IOException e) {
			System.out.println("[Towny] Error: Could not create backup.");
			System.out.print(e.getStackTrace());
		}
		universe.getPlugin().sendDebugMsg("New Day");
	}

}
