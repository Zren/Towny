package ca.xshade.bukkit.towny.questioner;

import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;

public class JoinTownTask extends ResidentTownQuestionTask {
	
	public JoinTownTask(Resident resident, Town town) {
		super(resident, town);
	}

	@Override
	public void run() {
		try {
			town.addResident(resident);
			towny.deleteCache(resident.getName());
			universe.getDataSource().saveResident(resident);
			
			getUniverse().sendTownMessage(town, resident.getName() + " joined town.");
		} catch (AlreadyRegisteredException e) {
			try {
				getUniverse().sendResidentMessage(resident, e.getError());
			} catch (TownyException e1) {
			}
		}
	}
}
