package ca.xshade.bukkit.towny.command;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

import ca.xshade.util.StringMgmt;

public class TownyCommandMap extends TownyCommand {
	SimpleCommandMap subCommands;
	
	public TownyCommandMap(Server server) {
		super("towny");
		subCommands = new SimpleCommandMap(server);
		
		TownyHelpCommand helpCommand = new TownyHelpCommand();
		subCommands.register("", "", new TownyPluginHelpCommand());
		subCommands.register("?", "", helpCommand);
		subCommands.register("help", "", helpCommand);
		subCommands.register("map", "", new TownyMapCommand());
		subCommands.register("prices", "", new TownyPricesCommand());
		subCommands.register("top", "", new TownyTopCommand());
		subCommands.register("tree", "", new TownyTreeCommand());
		subCommands.register("universe", "", new TownyUniverseCommand());
		subCommands.register("v", "", new TownyVersionCommand());
		subCommands.register("war", "", new TownyWarCommand());
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		return subCommands.dispatch(sender, StringMgmt.join(args, " "));
	}
}
