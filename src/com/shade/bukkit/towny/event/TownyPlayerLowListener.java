package com.shade.bukkit.towny.event;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;

import com.shade.bukkit.towny.NotRegisteredException;
import com.shade.bukkit.towny.Towny;
import com.shade.bukkit.towny.object.Nation;
import com.shade.bukkit.towny.object.Resident;
import com.shade.bukkit.towny.object.Town;
import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;

/**
 * Handle events for all Player related events
 * 
 * @author Shade
 * 
 */
public class TownyPlayerLowListener extends PlayerListener {
	private final Towny plugin;

	public TownyPlayerLowListener(Towny instance) {
		plugin = instance;
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		Player player = event.getPlayer();
		if (plugin.hasPlayerMode(player, "tc"))
			parseTownChatCommand(player, event.getMessage());
		else if (plugin.hasPlayerMode(player, "nc")) 
			parseNationChatCommand(player, event.getMessage());
		else
			return;
		
		event.setCancelled(true);
	}
	
	//TODO: Below is copy pasta :/

	public void parseTownChatCommand(Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			String line = Colors.Blue + "[" + town.getName() + "] "
					+ player.getDisplayName() + ": "
					+ Colors.LightBlue + msg;
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	public void parseNationChatCommand(Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Nation nation = resident.getTown().getNation();
			String line = Colors.Gold + "[" + nation.getName() + "] "
					+ player.getDisplayName() + ": "
					+ Colors.Yellow + msg;
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
}