package com.shade.bukkit.towny;

import java.util.logging.*;
import java.io.*;

import org.bukkit.plugin.java.JavaPlugin;

// TODO: Make sure the lake of a particular value doesn't error out the entire file

public class TownyFlatFileSource extends TownyDataSource {
	private final String newLine = System.getProperty("line.separator");
	protected static final Logger log = Logger.getLogger("Minecraft");

	public void initialize(JavaPlugin plugin, TownyUniverse universe, TownySettings settings) {
		this.universe = universe;
		this.plugin = plugin;
		this.settings = settings;
		
		if (settings.isFirstRun())
			firstRun = true;
		
		//Create files and folders if non-existent
        try {
        	// Check if root folder for flatfile database is created, otherwise assume first run.
        	File rootFolder = new File(settings.getFlatFileFolder());
            if (!(rootFolder.exists() && rootFolder.isDirectory())) {
            	rootFolder.mkdir();
                firstRun = true;
            }
        	
            String[] foldersToCreate = {"/settings","/data","/data/residents","/data/towns","/data/nations","/data/worlds","/data/townblocks"};
            String[] filesToCreate = {"/data/residents.txt","/data/towns.txt","/data/nations.txt","/data/worlds.txt"};
            for (String folder : foldersToCreate) {
                File f = new File(settings.getFlatFileFolder() + folder);
                if (!(f.exists() && f.isDirectory()))
                    f.mkdir();
            }
            for (String file : filesToCreate) {
                File f = new File(settings.getFlatFileFolder() + file);
                if (!(f.exists() && f.isFile()))
                    f.createNewFile();
            }
        } catch (IOException e) {
            log.info("[Towny] Error creating flatfile default files and folders.");
        }
	}
	
	/* 
	 * Load keys
	 */
	
	public boolean loadResidentList() {
		String line;
		try {
			BufferedReader fin = new BufferedReader(new FileReader(settings.getFlatFileFolder() + "/data/residents.txt"));
			while ( (line = fin.readLine()) != null) {
				if (!line.equals("")) {
					universe.newResident(line);  
				}
			}
			fin.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean loadTownList() {
		String line;
		try {
			BufferedReader fin = new BufferedReader(new FileReader(settings.getFlatFileFolder() + "/data/towns.txt"));
			while ( (line = fin.readLine()) != null) {
				if (!line.equals("")) {
					universe.newTown(line);  
				}
			}
			fin.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean loadNationList() {
		String line;
		try {
			BufferedReader fin = new BufferedReader(new FileReader(settings.getFlatFileFolder() + "/data/nations.txt"));
			while ( (line = fin.readLine()) != null) {
				if (!line.equals("")) {
					universe.newNation(line);  
				}
			}
			fin.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	
	
	/*
	 * Load individual towny object
	 */
	
	public boolean loadResident(Resident resident) {
		String line;
		String path = settings.getFlatFileFolder()+"/data/residents/"+resident.getName()+".txt";
		File fileResident = new File(path);
		if ((fileResident.exists() && fileResident.isFile())) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				resident.setLastOnline(Long.parseLong(kvFile.get("lastOnline")));
				
				line = kvFile.get("town");
				if (line != null) {
					resident.setTown(universe.getTown(line));
				}

				line = kvFile.get("friends");
				if (line != null) {
					String[] tokens = line.split(",");
					for (String token : tokens) {
						Resident friend = universe.getResident(token);
						if (friend != null) {
							resident.addFriend(friend);
						}
					}
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while reading resident file "+resident.getName(), e);
				return false;
			}
			
			return true;
		} else {
			return false;
		} 
	}
	
	public boolean loadTown(Town town) {
		String line;
		String[] tokens;
		String path = settings.getFlatFileFolder()+"/data/towns/"+town.getName()+".txt";
		File fileResident = new File(path);
		if ((fileResident.exists() && fileResident.isFile())) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				
				line = kvFile.get("residents");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident resident = universe.getResident(token);
						if (resident != null) {
							town.addResident(resident);
						}
					}
				}
				
				line = kvFile.get("mayor");
				if (line != null) {
					town.setMayor(universe.getResident(line));
				}
				
				line = kvFile.get("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident assistant = universe.getResident(token);
						if (assistant != null) {
							town.addAssistant(assistant);
						}
					}
				}
				
				town.setTownBoard(kvFile.get("townBoard"));
				
				line = kvFile.get("protectionStatus");
				if (line != null) {
					town.setPermissions(line);
				}
				
				line = kvFile.get("bonusBlocks");
				if (line != null) {
					try {
						town.setBonusBlocks(Integer.parseInt(line));
					} catch (Exception e) {
						town.setBonusBlocks(0);
					}
				}
				
				line = kvFile.get("pvp");
				if (line != null) {
					try {
						town.setPVP(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {} catch (Exception e) {}
				}
				
				line = kvFile.get("mobs");
				if (line != null) {
					try {
						town.setHasMobs(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {} catch (Exception e) {}
				}
				
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while reading town file "+town.getName(), e);
				return false;
			}
			
			return true;
		} else {
			return false;
		} 
	}
	
	public boolean loadNation(Nation nation) {
		String line = "";
		String[] tokens;
		String path = settings.getFlatFileFolder()+"/data/nations/"+nation.getName()+".txt";
		File fileResident = new File(path);
		if ((fileResident.exists() && fileResident.isFile())) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				
				line = kvFile.get("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Town town = universe.getTown(token);
						if (town != null)
							nation.addTown(town);
					}
				}
				
				line = kvFile.get("capital");
				nation.setCapital(universe.getTown(line));
				
				line = kvFile.get("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident assistant = universe.getResident(token);
						if (assistant != null) {
							nation.addAssistant(assistant);
						}
					}
				}
				
				line = kvFile.get("allies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Nation friend = universe.getNation(token);
						if (friend != null) {
							nation.setAliegeance("ally", friend);
						}
					}
				}
				
				line = kvFile.get("enemies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Nation enemy = universe.getNation(token);
						if (enemy != null) {
							nation.setAliegeance("enemy", enemy);
						}
					}
				}
				
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while reading nation file "+nation.getName(), e);
				return false;
			}
			
			return true;
		} else {
			return false;
		} 
	}
	
	public boolean loadWorld(TownyWorld world) {
		String line = "";
		String[] tokens;
		String path = settings.getFlatFileFolder()+"/data/nations/"+world.getName()+".txt";
		File fileResident = new File(path);
		if ((fileResident.exists() && fileResident.isFile())) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				
				line = kvFile.get("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Town town = universe.getTown(token);
						if (town != null)
							world.addTown(town);
					}
				}
				
				loadTownBlocks(world);
				
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while reading world file "+world.getName(), e);
				return false;
			}
			
			return true;
		} else {
			return false;
		} 
	}
	
	public boolean loadTownBlocks(TownyWorld world) {
		String line;
		String[] tokens;
		
		try {
			BufferedReader fin = new BufferedReader(new FileReader(settings.getFlatFileFolder() + "/data/townblocks/"+world.getName()+".csv"));
			while ( (line = fin.readLine()) != null) {
				tokens = line.split(",");
				if (tokens.length >= 3) {
					Town town = universe.getTown(tokens[2]);
					
					// Towns can't control blocks in more than one world.
					if (town.getWorld() != world)
						continue;
					
					int x = Integer.parseInt(tokens[0]);
					int z = Integer.parseInt(tokens[1]);
					
					world.newTownBlock(x, z);
					TownBlock townblock = world.getTownBlock(x, z);
					townblock.setTown(town);
					
					if (tokens.length >= 4) {
						Resident resident = universe.getResident(tokens[3]);
						townblock.setResident(resident);
					}
					if (tokens.length >= 5) {
						try {
							townblock.setForSale(Boolean.parseBoolean(tokens[4]));
						} catch (Exception e) {}
					}
				}
			}
			fin.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/*
	 * Save keys
	 */
	
    public boolean saveResidentList() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(settings.getFlatFileFolder() + "/data/residents.txt"));
            for (Resident resident : universe.getResidents()) {
                fout.write(resident.getName() + newLine);
            }    
            fout.close();
			return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while saving residents list file", e);
			return false;
        }
    }
	
	public boolean saveTownList() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(settings.getFlatFileFolder() + "/data/towns.txt"));
            for (Town town : universe.getTowns()) {
                fout.write(town.getName() + newLine);
            }    
            fout.close();
			return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while saving town list file", e);
			return false;
        }
    }
	
	public boolean saveNationList() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(settings.getFlatFileFolder() + "/data/nations.txt"));
            for (Nation nation : universe.getNations()) {
                fout.write(nation.getName() + newLine);
            }    
            fout.close();
			return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while saving nation list file", e);
			return false;
        }
    }
	
	public boolean saveWorldList() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(settings.getFlatFileFolder() + "/data/worlds.txt"));
            for (TownyWorld world : universe.getWorlds()) {
                fout.write(world.getName() + newLine);
            }    
            fout.close();
			return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while saving world list file", e);
			return false;
        }
    }
	
	/*
	 * Save individual towny objects
	 */
	
	public boolean saveResident(Resident resdient) {
		try {
			String path = settings.getFlatFileFolder()+"/data/residents/"+resdient.getName()+".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			fout.write("lastOnline=" + Long.toString(resdient.getLastOnline()) + newLine);
			if (resdient.hasTown())
				fout.write("town=" + resdient.getTown().getName() + newLine);
			fout.write("friends=");
			for(Resident friend : resdient.getFriends())
				fout.write(friend.getName() + ",");
			fout.write(newLine);
			fout.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean saveTown(Town town) {
		try {
			String path = settings.getFlatFileFolder()+"/data/towns/"+town.getName()+".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			// Residents
			fout.write("residents=");
			for (Resident resident : town.getResidents())
				fout.write(resident.getName() + ",");
			fout.write(newLine);
			// Mayor
			if (town.hasMayor())
				fout.write("mayor=" + town.getMayor().getName() + newLine);
			// Nation
			if (town.hasNation())
				fout.write("nation=" + town.getNation().getName() + newLine);
			// Assistants
			fout.write("assistants=");
			for(Resident assistant : town.getAssistants())
				fout.write(assistant.getName() + ",");
			fout.write(newLine);
			// Town Board
			fout.write("townBoard=" + town.getTownBoard() + newLine);
			// Town Protection
			fout.write("protectionStatus=" + town.getPermissions().toString() + newLine);
			// Bonus Blocks
			fout.write("bonusBlocks=" + Integer.toString(town.getBonusBlocks()) + newLine);
			// Home Block
			fout.write("homeBlock=" + Long.toString(town.getHomeBlock().getX()) + ":" + Long.toString(town.getHomeBlock().getZ()) + newLine);
			// PVP
			fout.write("pvp=" + Boolean.toString(town.isPVP()) + newLine);
			// Mobs
			fout.write("mobs=" + Boolean.toString(town.hasMobs()) + newLine);
			
			fout.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean saveNation(Nation nation) {
		try {
			String path = settings.getFlatFileFolder()+"/data/nations/"+nation.getName()+".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			fout.write("towns=");
			for (Town town : nation.getTowns())
				fout.write(town.getName() + ",");
			fout.write(newLine);
			if (nation.hasCapital())
				fout.write("capital=" + nation.getCapital().getName());
			fout.write(newLine);
			fout.write("assistants=");
			for(Resident assistant : nation.getAssistants())
				fout.write(assistant.getName() + ",");
			fout.write(newLine);
			fout.write("friends=");
			for(Nation allyNation : nation.getAllies())
				fout.write(allyNation.getName() + ",");
			fout.write(newLine);
			fout.write("enemies=");
			for(Nation enemyNation : nation.getEnemies())
				fout.write(enemyNation.getName() + ",");
			fout.write(newLine);
			
			fout.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean saveWorld(TownyWorld world) {
		try {
			String path = settings.getFlatFileFolder()+"/data/worlds/"+world.getName()+".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			fout.write("towns=");
			for (Town town : world.getTowns())
				fout.write(town.getName() + ",");
			fout.write(newLine);
			
			fout.close();
			
			saveTownBlocks(world);
			
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public boolean saveTownBlocks(TownyWorld world) {
		try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(settings.getFlatFileFolder() + "/data/townblocks/"+world.getName()+".csv"));
            for (TownBlock townblock : world.getTownBlocks()) {
                String line = townblock.getX() + "," + Long.toString(townblock.getZ());
                line += ",";
                if (townblock.hasTown())
                	line += townblock.getTown().getName();
                line += ",";
                if (townblock.hasResident())
                	line += townblock.getResident().getName();
                line += ",";
                if (townblock.hasResident())
                	line += townblock.getResident().getName();
                line += "," + Boolean.toString(townblock.isForSale());
                fout.write(line + newLine);
            }    
            fout.close();
			return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while saving town blocks list file", e);
			return false;
        }
    }
}