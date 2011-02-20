package ca.xshade.bukkit.towny.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.xshade.bukkit.towny.object.TownyUniverse;

public abstract class TownyCommand extends Command {
	
	public TownyCommand(String name) {
		super(name);
	}

	protected static TownyUniverse universe;
	
	public static void setUniverse(TownyUniverse u) {
		universe = u;
	}
	
	public void consoleUseOnly(Player player) {
		universe.getPlugin().sendErrorMsg(player, "This command was designed for use in the console only.");
	}
	
	public void inGameUseOnly(CommandSender sender) {
		sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
	}
	
	public boolean sendErrorMsg(CommandSender sender, String msg) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			universe.getPlugin().sendErrorMsg(player, msg);
		} else
			// Console
			sender.sendMessage("[Towny] ConsoleError: " + msg);
		
		return false;
	}
}
