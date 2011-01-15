package com.shade.bukkit.towny;

import java.util.ArrayList;

import org.bukkit.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handle events for all Player related events
 * @author Shade
 */
public class TownyPlayerListener extends PlayerListener {
    private final Towny plugin;

    public TownyPlayerListener(Towny instance) {
        plugin = instance;
    }

    @Override
    public void onPlayerJoin(PlayerEvent event) {
    	Player player = event.getPlayer();
    	try {
    		plugin.getTownyUniverse().onLogin(player);
    	} catch (TownyException x) {
    		plugin.sendErrorMsg(player, x.getError());
    	}
    }

    @Override
    public void onPlayerQuit(PlayerEvent event) {
    	plugin.getTownyUniverse().onLogout(event.getPlayer());
    }

    @Override
    public void onPlayerCommand(PlayerChatEvent event) {
        if (event.isCancelled())
        	return;
        
        String[] split = event.getMessage().split(" ");
        Player player = event.getPlayer();
        
        String[] newSplit = new String[split.length-1];
    	System.arraycopy(split, 1, newSplit, 0, split.length-1);
    	
        
        if (split[0].equalsIgnoreCase("/resident") || split[0].equalsIgnoreCase("/player")) {
        	parseResidentCommand(player, newSplit);
        	event.setCancelled(true);
        } else if (split[0].equalsIgnoreCase("/town")) {
        	parseTownCommand(player, newSplit);
        	event.setCancelled(true);
        } else if (split[0].equalsIgnoreCase("/nation")) {
        	parseNationCommand(player, newSplit);
        	event.setCancelled(true);
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
    	
    }
    
    public void parseResidentCommand(Player player, String[] split) {
    	/*
    	 * /resident
    	 * /resident ?
    	 *TODO: /resident [resident]
    	 * /resident list
    	 *TODO: /resident delete [resident] *Admin
    	 */
    	
    	if (split.length == 0) {
    		try {
	    		Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	    		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(resident));
    		} catch (NotRegisteredException x) {
    			plugin.sendErrorMsg(player, "You are not registered");
    		}
    	} else {
    		if (split[0].equalsIgnoreCase("?")) {
    			showResidentHelp(player);
	        } else if (split[0].equalsIgnoreCase("list")) {
	    		listResidents(player);
	        }
    	}
    }
 
    /**
     * Send a list of all resident commands to player
     * Command: /resident ?
     * @param player
     */
    
    public void showResidentHelp(Player player) {
    	player.sendMessage(ChatTools.formatTitle("/resident"));
    	player.sendMessage(ChatTools.formatCommand("", "/resident", "", "Your status"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/resident", "[resident]", "Target player's status"));
    	player.sendMessage(ChatTools.formatCommand("", "/resident", "list", "List all active players"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/resident", "delete [resident]", ""));
    }
    
    /**
     * Send a list of all active residents in the universe to player
     * Command: /resident list
     * @param player
     */
    
    public void listResidents(Player player) {
    	player.sendMessage(ChatTools.formatTitle("Residents"));
		String colour;
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Resident resident : plugin.getTownyUniverse().getActiveResidents()) {
			if (resident.isMayor())
				colour = Colors.LightBlue;
			else if (resident.isKing())
				colour = Colors.Gold;
			else
				colour = Colors.White;
			formatedList.add(colour + resident.getName() + Colors.White);
		}
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
    }
    
    public void parseTownCommand(Player player, String[] split) {
    	/*
    	 * /town
		 *TODO: /town ?
		 * /town list
		 *TODO: /town leave
		 * /town new [town] [mayor] *Admin
		 *TODO: /town givebonus [town] [bonus] *Admin
		 *TODO: /town delete [town] *Admin
		 *TODO: /town add [resident] *Mayor
		 *TODO: /town kick [resident] *Mayor
		 *TODO: /town wall
		 *TODO: /town setboard [message]
		 *TODO: /town setlord [town] [lord]
		 *TODO: /town sethome
		 *TODO: /town protect [on/off/buildonly]
		 *TODO: /town pvp [on/off]
    	 */
    	if (split.length == 0) {
    		try {
	    		Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	    		Town town = resident.getTown();
	    		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
    		} catch (NotRegisteredException x) {
    			plugin.sendErrorMsg(player, "You are not registered");
    		} catch (TownyException x) {
    			plugin.sendErrorMsg(player, x.getError());
    		}
    	} else {
    		if (split[0].equalsIgnoreCase("?")) {
	    		showTownHelp(player);
	        } else if (split[0].equalsIgnoreCase("list")) {
	    		listTowns(player);
	        } else if (split[0].equalsIgnoreCase("new")) {
	        	if (split.length == 1) {
	        		plugin.sendErrorMsg(player, "Specify town name");
	        	} else if (split.length == 2) {
	        		newTown(player, split[1], player.getName());
	        	} else {
	        		newTown(player, split[1], split[2]);
	        	}
	        }
    	}
    }

	/**
     * Send a list of all town commands to player
     * Command: /town ?
     * @param player
     */
    
    public void showTownHelp(Player player) {
    	String newTownReq = plugin.getTownyUniverse().getSettings().isTownCreationAdminOnly() ? "Admin" : "";
    	
    	player.sendMessage(ChatTools.formatTitle("/town"));
    	player.sendMessage(ChatTools.formatCommand("", "/town", "", "Your town's status"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/town", "[town]", "Selected town's status"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/town", "here", "Shortcut to the town's status of your location."));
    	player.sendMessage(ChatTools.formatCommand("", "/town", "list", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/town", "leave", ""));
    	player.sendMessage(ChatTools.formatCommand(newTownReq, "/town", "new [town] *[mayor]", "Create a new town."));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "add [resident]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "kick [resident]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "setboard [message]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "protect [on/off/buildonly]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "pvp [on/off]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "assistant [+/-] [player]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall [type] [height]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall remove", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "setlord [lord]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Admin", "/town", "givebonus [town] [bonus]", ""));
    	//TODO: player.sendMessage(ChatTools.formatCommand("Admin", "/town", "delete [town]", ""));
    }
    
    /**
     * Send a list of all towns in the universe to player
     * Command: /town list
     * @param player
     */
    
    public void listTowns(Player player) {
    	player.sendMessage(ChatTools.formatTitle("Towns"));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Town town : plugin.getTownyUniverse().getTowns())
			formatedList.add(Colors.LightBlue + town.getName() + Colors.Blue + " [" + town.getNumResidents() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
    }
    
    /**
     * Create a new town.
     * Command: /town new [town] *[mayor]
     * @param player
     */
    
    public void newTown(Player player, String name, String mayorName) {
    	TownyUniverse universe = plugin.getTownyUniverse();
    	TownySettings settings = universe.getSettings();
		try {
    		Resident resident = universe.getResident(mayorName);
    		if (resident.hasTown())
    			throw new TownyException("Target already belongs to a town.");
    		
    		TownyWorld world = universe.getWorld(player.getWorld().getName());
    		Coord key = Coord.parseCoord(settings, player);
    		if (world.hasTownBlock(key))
    			throw new TownyException("This area already belongs to someone.");
    		
    		if (resident.pay(settings.getNewTownPrice()))
    			throw new TownyException("You can't afford to settle a new town here.");
    		
    		world.newTownBlock(key);
    		universe.newTown(name);
			Town town = universe.getTown(name);
			town.addResident(resident);
			town.setMayor(resident);
			TownBlock townblock = world.getTownBlock(key);
			town.setHomeBlock(townblock);
			world.addTown(town);
			
			universe.getDataSource().saveResident(resident);
			universe.getDataSource().saveTown(town);
			universe.getDataSource().saveWorld(world);
			
			universe.sendGlobalMessage(settings.getNewTownMsg(player.getName(), town.getName()));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			//TODO: delete town data that might have been done
		}
    }
    
    public void parseNationCommand(Player player, String[] split) {
    	/*
    	 * /nation
    	 * /nation list
    	 *TODO: /nation leave *Mayor
    	 * /nation new [nation] [capital] *Admin
    	 *TODO: /nation delete [nation] *Admin
    	 *TODO: /nation add [town] *King
    	 *TODO: /nation kick [town] *King
    	 */
    	
    	if (split.length == 0) {
    		try {
	    		Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	    		Town town = resident.getTown();
	    		Nation nation = town.getNation();
	    		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(nation));
    		} catch (NotRegisteredException x) {
    			plugin.sendErrorMsg(player, "You are not registered");
    		} catch (TownyException x) {
    			plugin.sendErrorMsg(player, x.getError());
    		}
    	} else {
    		if (split[0].equalsIgnoreCase("?")) {
    			showNationHelp(player);
	        } else if (split[0].equalsIgnoreCase("list")) {
	    		listNations(player);
	        } else if (split[0].equalsIgnoreCase("new")) {
	        	//TODO: Make an overloaded function newNation(Player,String,Town) 
	        	if (split.length == 1) {
	        		plugin.sendErrorMsg(player, "Specify nation name");
	        	} else if (split.length == 2) {
	        		try { //TODO: Make sure of the error catching
	        			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	        			newNation(player, split[1], resident.getTown().getName());
	        		} catch (TownyException x) {
	         			plugin.sendErrorMsg(player, x.getError());
	         		}
	        	} else {
	        		newNation(player, split[1], split[2]);
	        	}
	        }
    	}
    }
    
    /**
     * Send a list of all nation commands to player
     * Command: /nation ?
     * @param player
     */
    
    public void showNationHelp(Player player) {
    	player.sendMessage(ChatTools.formatTitle("/nation"));
    	player.sendMessage(ChatTools.formatCommand("", "/nation", "", "Your nation's status"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/nation", "[nation]", "Target nation's status"));
    	player.sendMessage(ChatTools.formatCommand("", "/nation", "list", "List all nations"));
    	//TODO: player.sendMessage(ChatTools.formatCommand("", "/nation", "delete [nation]", ""));
    }
    
    /**
     * Send a list of all nations in the universe to player
     * Command: /nation list
     * @param player
     */
    
    public void listNations(Player player) {
    	player.sendMessage(ChatTools.formatTitle("Nations"));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Nation nation : plugin.getTownyUniverse().getNations())
			formatedList.add(Colors.LightBlue + nation.getName() + Colors.Blue + " [" + nation.getNumTowns() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
    }
    
    /**
     * Create a new nation.
     * Command: /nation new [nation] *[capital]
     * @param player
     */
    
    public void newNation(Player player, String name, String capitalName) {
    	TownyUniverse universe = plugin.getTownyUniverse();
    	TownySettings settings = universe.getSettings();
		try {
    		Town town = universe.getTown(capitalName);
    		if (town.hasNation())
    			throw new TownyException("Target already belongs to a nation.");
    		
    		universe.newNation(name);
			Nation nation = universe.getNation(name);
			nation.addTown(town);
			nation.setCapital(town);
			
			universe.getDataSource().saveTown(town);
			universe.getDataSource().saveNation(nation);
			
			universe.sendGlobalMessage(settings.getNewTownMsg(player.getName(), town.getName()));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			//TODO: delete town data that might have been done
		}
    }
}