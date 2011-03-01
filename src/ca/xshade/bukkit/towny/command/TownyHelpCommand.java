package ca.xshade.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;

/**
 * Send a list of all towny commands to player
 * Command: /towny ?
 * Command: /towny help
 */

public class TownyHelpCommand extends TownyCommand {
	public static final List<String> output = new ArrayList<String>();
	
	static {
		output.add(ChatTools.formatTitle("/towny"));
		output.add(ChatTools.formatCommand("", "/towny", "", "General help for Towny"));
		output.add(ChatTools.formatCommand("", "/towny", "map", "Displays a map of the nearby townblocks"));
		output.add(ChatTools.formatCommand("", "/towny", "prices", "Display the prices used with iConomy"));
		output.add(ChatTools.formatCommand("", "/towny", "top", "Display highscores"));
		output.add(ChatTools.formatCommand("", "/towny", "universe", "Displays stats"));
		output.add(ChatTools.formatCommand("", "/towny", "v", "Displays the version of Towny"));
		output.add(ChatTools.formatCommand("", "/towny", "war", "'/towny war' for more info"));
	}
	
	public TownyHelpCommand() {
		super("help");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		for (String line : output)
			sender.sendMessage(line);
		
		if (!(sender instanceof Player))
			sender.sendMessage(Colors.strip(ChatTools.formatCommand("Console", "towny", "tree", "Display universe tree")));
		
		return true;
	}
}
