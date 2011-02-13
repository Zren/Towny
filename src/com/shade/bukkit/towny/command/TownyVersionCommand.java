package com.shade.bukkit.towny.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.shade.bukkit.util.Colors;

/**
 * Send the version of towny to player
 * Command: /towny version
 */

public class TownyVersionCommand extends TownyCommand {
	
	public TownyVersionCommand() {
		super("version");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		String output = Colors.Green + "Towny version: " + Colors.LightGreen + universe.getPlugin().getVersion();
		if (sender instanceof Player) {
			Player player = (Player)sender;
			player.sendMessage(output);
		} else
			// Console
			sender.sendMessage(Colors.strip(output));
		return true;
	}
}