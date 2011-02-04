package com.shade.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;

public class TownyHelpCommand extends TownyCommand {
	public static final List<String> output = new ArrayList<String>();
	
	static {
		output.add(ChatTools.formatTitle("Towny Help"));
		output.add(ChatTools.formatCommand("Debug", "towny", "tree", "Display universe tree"));
	}
	
	public TownyHelpCommand() {
		super("help");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			for (String line : output)
				player.sendMessage(line);
		} else
			// Console
			for (String line : output)
				sender.sendMessage(Colors.strip(line));
		return true;
	}
}
