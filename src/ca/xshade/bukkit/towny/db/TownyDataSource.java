package ca.xshade.bukkit.towny.db;

import java.io.IOException;

import org.bukkit.World;

import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.object.TownyWorld;

/*
 * --- : Loading process : ---
 * 
 * Load all the names/keys for each world, nation, town, and resident.
 * Load each world, which loads it's town blocks.
 * Load nations, towns, and residents.
 */

/*
 * Loading Towns:
 * Make sure to load TownBlocks, then HomeBlock, then Spawn.
 */

public abstract class TownyDataSource {
	protected TownyUniverse universe;
	protected TownySettings settings;
	protected Towny plugin;
	protected boolean firstRun = false;

	public void initialize(Towny plugin, TownyUniverse universe) {
		this.universe = universe;
		this.plugin = plugin;
	}
	
	public void backup() throws IOException {
	}

	public boolean loadAll() {
		return loadWorldList() && loadNationList() && loadTownList()
				&& loadResidentList() && loadWorlds() && loadNations()
				&& loadTowns() && loadResidents();
	}

	public boolean saveAll() {
		return saveWorldList() && saveNationList() && saveTownList()
				&& saveResidentList() && saveWorlds() && saveNations()
				&& saveTowns() && saveResidents();
	}

	abstract public boolean loadResidentList();

	abstract public boolean loadTownList();

	abstract public boolean loadNationList();

	abstract public boolean loadResident(Resident resident);

	abstract public boolean loadTown(Town town);

	abstract public boolean loadNation(Nation nation);

	abstract public boolean loadWorld(TownyWorld world);

	abstract public boolean saveResidentList();

	abstract public boolean saveTownList();

	abstract public boolean saveNationList();

	abstract public boolean saveWorldList();

	abstract public boolean saveResident(Resident resident);

	abstract public boolean saveTown(Town town);

	abstract public boolean saveNation(Nation nation);

	abstract public boolean saveWorld(TownyWorld world);

	public boolean loadWorldList() {
		return loadServerWorldsList();
	}
	
	public boolean loadServerWorldsList() {
		for (World world : plugin.getServer().getWorlds())
			try {
				String[] split = world.getName().split("/");
				String worldName = split[split.length-1];
				universe.newWorld(worldName);
			} catch (AlreadyRegisteredException e) {
			}
		return true;
	}

	/*
	 * Load all of category
	 */

	public boolean loadResidents() {
		for (Resident resident : universe.getResidents())
			if (!loadResident(resident))
				System.out.println("[Towny] Loading Error: Could not read resident data '" + resident.getName() + "'.");
		return true;
	}

	public boolean loadTowns() {
		for (Town town : universe.getTowns())
			if (!loadTown(town))
				System.out.println("[Towny] Loading Error: Could not read town data " + town.getName() + "'.");
		return true;
	}

	public boolean loadNations() {
		for (Nation nation : universe.getNations())
			if (!loadNation(nation))
				System.out.println("[Towny] Loading Error: Could not read nation data '" + nation.getName() + "'.");
		return true;
	}

	public boolean loadWorlds() {
		for (TownyWorld world : universe.getWorlds())
			if (!loadWorld(world))
				if (!TownySettings.isFirstRun())
					System.out.println("[Towny] Loading Error: Could not read world data '" + world.getName() + "'.");
		return true;
	}

	/*
	 * Save all of category
	 */

	public boolean saveResidents() {
		for (Resident resident : universe.getResidents())
			saveResident(resident);
		return true;
	}

	public boolean saveTowns() {
		for (Town town : universe.getTowns())
			saveTown(town);
		return true;
	}

	public boolean saveNations() {
		for (Nation nation : universe.getNations())
			saveNation(nation);
		return true;
	}

	public boolean saveWorlds() {
		for (TownyWorld world : universe.getWorlds())
			saveWorld(world);
		return true;
	}
}