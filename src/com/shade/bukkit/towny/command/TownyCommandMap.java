package com.shade.bukkit.towny.command;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

import com.shade.util.StringMgmt;

public class TownyCommandMap extends TownyCommand {
	SimpleCommandMap subCommands;
	
	public TownyCommandMap(Server server) {
		super("towny");
		subCommands = new SimpleCommandMap(server);
		
		TownyHelpCommand helpCommand = new TownyHelpCommand();
		subCommands.register("", "", helpCommand);
		subCommands.register("?", "", helpCommand);
		subCommands.register("help", "", helpCommand);
		subCommands.register("tree", "", new TownyTreeCommand());
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		return subCommands.dispatch(sender, StringMgmt.join(args, " "));
	}

}
