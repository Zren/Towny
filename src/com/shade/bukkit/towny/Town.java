package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class Town extends TownyObject {
	List<Resident> residents, assistants;
	Resident mayor;
	Nation nation;
	boolean isPVP, hasMobs;
	String townBoard;
	
	public List<String> getStatus() {
        List<String> out = new ArrayList<String>();
        
     // ___[ Racoon City (PvP) ]___
        out.add(ChatTools.formatTitle(toString() + (isPVP ? Colors.Red+" (PvP)" : "")));
        
        // Lord: Mayor Quimby
        // Board: Get your fried chicken
        if (townBoard != null)
            out.add(Colors.Green + "Board: " + Colors.LightGreen + townBoard);
        
		// Town Size: 0 / 16 [Bonus: 0]
		//TODO:out.add(Colors.Green + "Town Size: " + Colors.LightGreen + TownyWorld.getInstance().countTownBlocks(this) + " / " + getMaxTownBlocks() + Colors.LightBlue + " [Bonus: "+bonusBlocks+"]");
		
		//if (mayor != null)
            out.add(Colors.Green + "Lord: " + Colors.LightGreen + mayor);
        // Assistants:
		// Sammy, Ginger
        if (assistants.size() > 0) {
			out.add(Colors.Green + "Assistants:");
			out.addAll(ChatTools.list(assistants.toArray()));
		}
        // Nation: Azur Empire
        if (nation != null)
            out.add(Colors.Green + "Nation: " + Colors.LightGreen + nation);
        
        // Residents [12]:
        // James, Carry, Mason
        out.add(Colors.Green + "Residents " + Colors.LightGreen + "[" + getNumResidents() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(residents.toArray()));
        
        return out;
    }
	
	public boolean hasResident(String name) {
		for (Resident resident : residents)
			if (resident.getName().equalsIgnoreCase(name))
				return true;
		return false;
	}
	
	public boolean hasResident(Resident resident) {
		return residents.contains(resident);
	}
	
	public boolean hasAssistant(Resident resident) {
		if (resident == null)
			return false;
		else
			return assistants.contains(resident);
	}
	
	public void addResident(Resident resident) throws AlreadyRegisteredException {
		if (hasResident(resident))
			throw new AlreadyRegisteredException();
		else
			residents.add(resident);
	}
	
	public boolean isMayor(Resident resident) {
		return (resident == mayor);
	}
	
	public boolean hasNation() {
		return !(nation == null);
	}
	
	public int getNumResidents() {
		return residents.size();
	}
}
