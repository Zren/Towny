package ca.xshade.bukkit.towny.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Show the current universe in the console.
 * Command: /towny tree
 */

public class TownyTreeCommand extends TownyCommand {

	public TownyTreeCommand() {
		super("tree");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			consoleUseOnly(player);
		} else
			// Console
			universe.sendUniverseTree(sender);
		return true;
	}
}
