package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class Resident extends TownyObject {
	private List<Resident> friends = new ArrayList<Resident>();
	private Town town;
	private long lastOnline;
	
	public List<String> getStatus() {
        List<String> out = new ArrayList<String>();
        
        // ___[ King Harlus ]___
        out.add(ChatTools.formatTitle(toString()));
        
        // Last Online: March 7
        out.add(Colors.Green + "Last Online: " + Colors.LightGreen + getLastOnline());
        
        // Town: Camelot
        String line = Colors.Green + "Town: " + Colors.LightGreen;
        if (town == null) {
            line += "None";
        } else {
            line += town;
        }
        out.add(line);
		
		// Friends [12]:
        // James, Carry, Mason
        out.add(Colors.Green + "Friends " + Colors.LightGreen + "[" + friends.size() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(friends.toArray()));
        
        return out;
    }

	public void setLastOnline(long lastOnline) {
		this.lastOnline = lastOnline;
	}

	public long getLastOnline() {
		return lastOnline;
	}

	public boolean isKing() {
		return (hasNation() ? town.nation.isKing(this) : false);
	}

	public boolean isMayor() {
		if (town == null)
			return false;
		else
			return town.isMayor(this);
	}
	
	public boolean hasTown() {
		return !(town == null);
	}
	
	public boolean hasNation() {
		return (hasTown() ? town.hasNation() : false);
	}
	
	public Town getTown() throws TownyException {
		if (hasTown())
			return town;
		else
			throw new TownyException("Resident doesn't belong to any town");
	}
}
