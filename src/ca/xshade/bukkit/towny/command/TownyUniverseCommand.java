package ca.xshade.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.xshade.bukkit.util.Colors;

/**
 * Send some stats about the towny universe to the player
 * Command: /towny universe
 */

public class TownyUniverseCommand extends TownyCommand {
	public TownyUniverseCommand() {
		super("universe");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			for (String line : getUniverseStats())
				player.sendMessage(line);
		} else
			// Console
			for (String line : getUniverseStats())
				sender.sendMessage(Colors.strip(line));
		return true;
	}
	
	public List<String> getUniverseStats() {
		List<String> output = new ArrayList<String>();
		output.add("§0-§4###§0---§4###§0-");
		output.add("§4#§c###§4#§0-§4#§c###§4#§0   §6[§eTowny " + universe.getPlugin().getVersion() + "§6]");
		output.add("§4#§c####§4#§c####§4#   §3By: §bChris H (Shade)");
		output.add("§0-§4#§c#######§4#§0-");
		output.add("§0--§4##§c###§4##§0-- " 
				+ "§3Residents: §b" + Integer.toString(universe.getResidents().size())
				+ Colors.Gray + " | "
				+ "§3Towns: §b" + Integer.toString(universe.getTowns().size())
				+ Colors.Gray + " | "
				+ "§3Nations: §b" + Integer.toString(universe.getNations().size()));
		output.add("§0----§4#§c#§4#§0---- "
				+ "§3Worlds: §b" + Integer.toString(universe.getWorlds().size())
				+ Colors.Gray + " | "
				+ "§3TownBlocks: §b" + Integer.toString(universe.getAllTownBlocks().size()));
		output.add("§0-----§4#§0----- ");
		return output;
	}
}