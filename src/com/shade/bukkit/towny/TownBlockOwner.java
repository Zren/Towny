package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

public class TownBlockOwner extends TownyIConomyObject {
	protected List<TownBlock> townBlocks = new ArrayList<TownBlock>();
	
	public void setTownblocks(List<TownBlock> townblocks) {
		this.townBlocks = townblocks;
	}

	public List<TownBlock> getTownBlocks() {
		return townBlocks;
	}

	public boolean hasTownBlock(TownBlock townBlock) {
		return townBlocks.contains(townBlock);
	}
	
	public void addTownBlock(TownBlock townBlock) throws AlreadyRegisteredException {
		if (hasTownBlock(townBlock)) {
			throw new AlreadyRegisteredException();
		} else {
			townBlocks.add(townBlock);
		}
	}
	
	public void removeTownBlock(TownBlock townBlock) throws NotRegisteredException {
		if (!hasTownBlock(townBlock)) {
			throw new NotRegisteredException();
		} else {
			townBlocks.remove(townBlock);
		}
	}
}
