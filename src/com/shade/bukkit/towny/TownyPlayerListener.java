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
    	 *TODO: /resident ?
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
	    	if (split[0].equalsIgnoreCase("list")) {
	    		listResidents(player);
	        }
    	}
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
		 *TODO: /town new [town] [mayor] *Admin
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
	    	if (split[0].equalsIgnoreCase("list")) {
	    		listTowns(player);
	        } else if (split[0].equalsIgnoreCase("new")) {
	        	
	        }
    	}
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
    
    public void parseNationCommand(Player player, String[] split) {
    	/*
    	 * /nation
    	 * /nation list
    	 *TODO: /nation leave *Mayor
    	 *TODO: /nation new [nation] [capital] *Admin
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
	    	if (split[0].equalsIgnoreCase("list")) {
	    		listNations(player);
	        }
    	}
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
}