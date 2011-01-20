package com.shade.bukkit.towny.db;

import org.bukkit.World;

import com.shade.bukkit.towny.AlreadyRegisteredException;
import com.shade.bukkit.towny.Nation;
import com.shade.bukkit.towny.Resident;
import com.shade.bukkit.towny.Town;
import com.shade.bukkit.towny.Towny;
import com.shade.bukkit.towny.TownySettings;
import com.shade.bukkit.towny.TownyUniverse;
import com.shade.bukkit.towny.TownyWorld;

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

	public void initialize(Towny plugin, TownyUniverse universe,
			TownySettings settings) {
		this.universe = universe;
		this.plugin = plugin;
		this.settings = settings;

		if (settings.isFirstRun())
			firstRun = true;

		if (firstRun)
			firstRun();
	}

	private void firstRun() {
		System.out.println("------------------------------");
		System.out.println("[Towny] Detected first run");
		System.out.println("[Towny] Registering default");
		System.out.println("------------------------------");
	}

	public boolean loadAll() {
		return (loadWorldList() && loadNationList() && loadTownList()
				&& loadResidentList() && loadWorlds() && loadNations()
				&& loadTowns() && loadResidents());
	}

	public boolean saveAll() {
		return (saveWorldList() && saveNationList() && saveTownList()
				&& saveResidentList() && saveWorlds() && saveNations()
				&& saveTowns() && saveResidents());
	}

	abstract public boolean loadResidentList();

	abstract public boolean loadTownList();

	abstract public boolean loadNationList();

	abstract public boolean loadResident(Resident resdient);

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
		for (World world : plugin.getServer().getWorlds()) {
			try {
				universe.newWorld(world.getName());
			} catch (AlreadyRegisteredException e) {
			}
		}
		return true;
	}

	/*
	 * Load all of category
	 */

	public boolean loadResidents() {
		for (Resident resident : universe.getResidents()) {
			loadResident(resident);
		}
		return true;
	}

	public boolean loadTowns() {
		for (Town town : universe.getTowns()) {
			loadTown(town);
		}
		return true;
	}

	public boolean loadNations() {
		for (Nation nation : universe.getNations()) {
			loadNation(nation);
		}
		return true;
	}

	public boolean loadWorlds() {
		for (TownyWorld world : universe.getWorlds()) {
			loadWorld(world);
		}
		return true;
	}

	/*
	 * Save all of category
	 */

	public boolean saveResidents() {
		for (Resident resident : universe.getResidents()) {
			saveResident(resident);
		}
		return true;
	}

	public boolean saveTowns() {
		for (Town town : universe.getTowns()) {
			saveTown(town);
		}
		return true;
	}

	public boolean saveNations() {
		for (Nation nation : universe.getNations()) {
			saveNation(nation);
		}
		return true;
	}

	public boolean saveWorlds() {
		for (TownyWorld world : universe.getWorlds()) {
			saveWorld(world);
		}
		return true;
	}
}