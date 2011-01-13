package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class TownyFormatter {
	private TownySettings settings = new TownySettings();
	
	public TownyFormatter(TownySettings settings) {
		this.settings = settings;
	}
	
	
	public List<String> getStatus(Resident resident) {
        List<String> out = new ArrayList<String>();
        
        // ___[ King Harlus ]___
        out.add(ChatTools.formatTitle(getFormattedName(resident)));
        
        // Last Online: March 7
        out.add(Colors.Green + "Last Online: " + Colors.LightGreen + resident.getLastOnline());
        
        // Town: Camelot
        String line = Colors.Green + "Town: " + Colors.LightGreen;
        if (!resident.hasTown()) {
            line += "None";
        } else {
            try {
				line += getFormattedName(resident.getTown());
			} catch (TownyException e) {
				line += "Error: " + e.getError();
			}
        }
        out.add(line);
		
		// Friends [12]:
        // James, Carry, Mason
        List<Resident> friends = resident.getFriends();
        out.add(Colors.Green + "Friends " + Colors.LightGreen + "[" + friends.size() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(friends.toArray()));
        
        return out;
    }
	
	public List<String> getStatus(Town town) {
        List<String> out = new ArrayList<String>();
        
     // ___[ Racoon City (PvP) ]___
        out.add(ChatTools.formatTitle(getFormattedName(town) + (town.isPVP() ? Colors.Red+" (PvP)" : "")));
        
        // Lord: Mayor Quimby
        // Board: Get your fried chicken
        try {
            out.add(Colors.Green + "Board: " + Colors.LightGreen + town.getTownBoard());
        } catch (NullPointerException e) {}
        
		// Town Size: 0 / 16 [Bonus: 0]
        // TODO: Maximum
		out.add(Colors.Green + "Town Size: " + Colors.LightGreen + town.getTownblocks().size() + " / " + settings.getMaxTownBlocks(town) + Colors.LightBlue + " [Bonus: "+town.getBonusBlocks()+"]");
		
		//if (mayor != null)
            out.add(Colors.Green + "Lord: " + Colors.LightGreen + town.getMayor());
        // Assistants:
		// Sammy, Ginger
        if (town.getAssistants().size() > 0) {
			out.add(Colors.Green + "Assistants:");
			out.addAll(ChatTools.list(town.getAssistants().toArray()));
		}
        // Nation: Azur Empire
        if (town.hasNation())
            out.add(Colors.Green + "Nation: " + Colors.LightGreen + town.getNation());
        
        // Residents [12]:
        // James, Carry, Mason
        out.add(Colors.Green + "Residents " + Colors.LightGreen + "[" + town.getNumResidents() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(town.getResidents().toArray()));
        
        return out;
    }
	
	public List<String> getStatus(Nation nation) {
        List<String> out = new ArrayList<String>();
        
        // ___[ Azur Empire ]___
        out.add(ChatTools.formatTitle(getFormattedName(nation)));
        
        // King: King Harlus
        if (nation.getNumTowns() > 0 && nation.hasCapital() && nation.getCapital().hasMayor())
            out.add(Colors.Green + "King: " + Colors.LightGreen + getFormattedName(nation.getCapital().getMayor()));
		// Assistants:
		// Mayor Rockefel, Sammy, Ginger
        if (nation.getAssistants().size() > 0) {
			out.add(Colors.Green + "Assistants:");
			out.addAll(ChatTools.list(nation.getAssistants().toArray()));
		}
        // Towns [44]:
        // James City, Carry Grove, Mason Town
        out.add(Colors.Green + "Towns " + Colors.LightGreen + "[" + nation.getNumTowns() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(nation.getTowns().toArray()));
		// Allies [4]:
        // James Nation, Carry Territory, Mason Country
        out.add(Colors.Green + "Allies " + Colors.LightGreen + "[" + nation.getAllies().size() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(nation.getAllies().toArray()));
		// Enemies [4]:
        // James Nation, Carry Territory, Mason Country
        out.add(Colors.Green + "Enemies " + Colors.LightGreen + "[" + nation.getEnemies().size() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(nation.getEnemies().toArray()));
		
        
        return out;
    }
	
	public String getFormattedName(Resident resident) {
		if (resident.isKing())
			return settings.getKingPrefix() + resident.getName();
		else if (resident.isMayor())
			return settings.getMayorPrefix() + resident.getName();
		return resident.getName();
	}
	
	public String getFormattedName(Town town) {
		if (town.isCapital())
			return town.getName() + settings.getCapitalPostfix();
		return town.getName() + settings.getTownPostfix();
	}
	
	public String getFormattedName(Nation nation) {
		return nation.getName() + settings.getNationPostfix();
	}
}
