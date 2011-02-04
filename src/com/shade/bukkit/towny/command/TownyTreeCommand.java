package com.shade.bukkit.towny.command;

import org.bukkit.command.CommandSender;

public class TownyTreeCommand extends TownyCommand {

	public TownyTreeCommand() {
		super("tree");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		universe.sendUniverseTree(sender);
		return true;
	}
}
