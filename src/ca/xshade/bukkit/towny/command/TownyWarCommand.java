package ca.xshade.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;

public class TownyWarCommand extends TownyCommand {
	public final List<String> output = new ArrayList<String>();
	
	public TownyWarCommand() {
		super("war");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			output.add(ChatTools.formatTitle("/towny war"));
			output.add(ChatTools.formatCommand("", "/towny war", "stats", ""));
			output.add(ChatTools.formatCommand("", "/towny war", "scores", ""));
			output.add(ChatTools.formatCommand("", "/towny war", "map", ""));
		} else
			if (universe.isWarTime()) {
				if (args[0].equalsIgnoreCase("stats"))
					output.addAll(universe.getWarEvent().getStats());
				else if (args[0].equalsIgnoreCase("scores"))
					output.addAll(universe.getWarEvent().getScores(-1));
				else
					sendErrorMsg(sender, "Invalid sub command.");
			} else //TODO: Remove smartassery
				sendErrorMsg(sender, "The world isn't currently going to hell.");
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			for (String line : output)
				player.sendMessage(line);
		} else
			// Console
			for (String line : output)
				sender.sendMessage(Colors.strip(line));
		
		output.clear();
		return true;
	}
}