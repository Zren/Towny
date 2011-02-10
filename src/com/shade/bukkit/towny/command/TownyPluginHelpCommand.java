package com.shade.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.shade.bukkit.towny.TownySettings;
import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;

public class TownyPluginHelpCommand extends TownyCommand {
	public static final List<String> output = new ArrayList<String>();
	
	static {
		output.add(ChatTools.formatTitle("General Towny Help"));
		output.add("Try the following commands to learn more about towny.");
		output.add(ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getResidentCommands()), "?", "")
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getTownCommands()), "?", "") 
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getNationCommands()), "?", "")
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getPlotCommands()), "?", "")
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getTownyCommands()), "?", ""));
		output.add(ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getTownChatCommands()), " [msg]", "Town Chat")
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getNationChatCommands()), " [msg]", "Nation Chat"));
		output.add(ChatTools.formatCommand("Admin", TownySettings.getFirstCommand(TownySettings.getTownyAdminCommands()), "?", ""));
	}
	
	public TownyPluginHelpCommand() {
		super("");
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
