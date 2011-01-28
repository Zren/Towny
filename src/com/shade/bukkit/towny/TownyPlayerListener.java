package com.shade.bukkit.towny;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nijikokun.bukkit.iConomy.iConomy;
import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;
import com.shade.bukkit.util.Compass;
import com.shade.util.MemMgmt;

/**
 * Handle events for all Player related events
 * 
 * @author Shade
 */
public class TownyPlayerListener extends PlayerListener {
	private final Towny plugin;

	public TownyPlayerListener(Towny instance) {
		plugin = instance;
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		Player player = event.getPlayer();
		if (TownySettings.isUsingChatPrefix())
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				String colour, formatedName = "";
				if (resident.isKing())
					colour = Colors.Gold;
				else if (resident.isMayor())
					colour = Colors.LightBlue;
				else
					colour = Colors.White;
				formatedName = colour + plugin.getTownyUniverse().getFormatter().getNamePrefix(resident)
					+ "%1$s" + plugin.getTownyUniverse().getFormatter().getNamePostfix(resident) + Colors.White;
				String formatString = event.getFormat();
				int index = formatString.indexOf("%1$s");
				formatString = formatString.substring(0, index) + formatedName + formatString.substring(index+4);
				event.setFormat(formatString);
			} catch (NotRegisteredException e) {
			}
	}

	@Override
	public void onPlayerJoin(PlayerEvent event) {
		Player player = event.getPlayer();
		try {
			plugin.getTownyUniverse().onLogin(player);
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	@Override
	public void onPlayerQuit(PlayerEvent event) {
		plugin.getTownyUniverse().onLogout(event.getPlayer());
		
		plugin.deleteCache(event.getPlayer());
	}

	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();

		Coord fromCoord = Coord.parseCoord(from);
		Coord toCoord = Coord.parseCoord(to);
		if (!fromCoord.equals(toCoord))
			onPlayerMoveChunk(player, fromCoord, toCoord, from, to);
	}

	@Override
	public void onPlayerTeleport(PlayerMoveEvent event) {
		onPlayerMove(event);
	}

	public void onPlayerMoveChunk(Player player, Coord from, Coord to, Location fromLoc, Location toLoc) {
		TownyUniverse universe = plugin.getTownyUniverse();
		
		plugin.getCache(player).updateCoord(to);
		
		
		// TODO: Player mode
		// map: send the map
		// claim: attempt to claim area
		// claim remove: remove area from town

		// Check if player has entered a new town/wilderness
		if (TownySettings.getShowTownNotifications()) {
			boolean fromWild = false, toWild = false;
			TownBlock fromTownBlock, toTownBlock;
			Town fromTown = null, toTown = null;
			Resident fromResident = null, toResident = null;
			try {
				fromTownBlock = universe.getWorld(fromLoc.getWorld().getName()).getTownBlock(from);
				try {
					fromTown = fromTownBlock.getTown();
				} catch (NotRegisteredException e) {
				}
				try {
					fromResident = fromTownBlock.getResident();
				} catch (NotRegisteredException e) {
				}
			} catch (NotRegisteredException e) {
				fromWild = true;
			}

			try {
				toTownBlock = universe.getWorld(toLoc.getWorld().getName()).getTownBlock(to);
				try {
					toTown = toTownBlock.getTown();
				} catch (NotRegisteredException e) {
				}
				try {
					toResident = toTownBlock.getResident();
				} catch (NotRegisteredException e) {
				}
			} catch (NotRegisteredException e) {
				toWild = true;
			}
			
			boolean sendToMsg = false;
			String toMsg = Colors.Gold + " ~ ";

			if (fromWild ^ toWild || !fromWild && !toWild && fromTown != null && toTown != null && fromTown != toTown) {
				sendToMsg = true;
				if (toWild)
					toMsg += Colors.Green + TownySettings.getUnclaimedZoneName();
				else
					toMsg += universe.getFormatter().getFormattedName(toTown);
			}
			
			if (toResident != null && fromResident != toResident) {
				if (!sendToMsg)
					sendToMsg = true;
				else
					toMsg += Colors.LightGray + "  -  ";
				toMsg += Colors.LightGreen + universe.getFormatter().getFormattedName(toResident);
			}
			
			if (sendToMsg)
				player.sendMessage(toMsg);
			
			if (TownySettings.getDebug())
				System.out.println("[Towny] Debug: onPlayerMoveChunk: " + fromWild + " ^ " + toWild + " " + fromTown + " = " + toTown);
		}
		
	}

	@Override
	public void onPlayerCommand(PlayerChatEvent event) {
		if (event.isCancelled())
			return;

		long start = System.currentTimeMillis();
		
		String[] split = event.getMessage().split(" ");
		Player player = event.getPlayer();

		String[] newSplit = new String[split.length - 1];
		System.arraycopy(split, 1, newSplit, 0, split.length - 1);
		
		if (TownySettings.getResidentCommands().contains(split[0]))
			parseResidentCommand(player, newSplit);
		else if (TownySettings.getTownCommands().contains(split[0]))
			parseTownCommand(player, newSplit);
		else if (TownySettings.getNationCommands().contains(split[0]))
			parseNationCommand(player, newSplit);
		else if (TownySettings.getPlotCommands().contains(split[0]))
			parsePlotCommand(player, newSplit);
		else if (TownySettings.getTownyCommands().contains(split[0]))
			parseTownyCommand(player, newSplit);
		else if (TownySettings.getTownyAdminCommands().contains(split[0]))
			parseTownyAdminCommand(player, newSplit);
		else if (TownySettings.getTownChatCommands().contains(split[0]))
			parseTownChatCommand(player, event.getMessage().substring(4));
		else if (TownySettings.getNationChatCommands().contains(split[0]))
			parseNationChatCommand(player, event.getMessage().substring(4));
		else
			return;

		if (TownySettings.getDebug())
			System.out.println("[Towny] Debug: onCommand took " + (System.currentTimeMillis() - start) + "ms");
		event.setCancelled(true);
	}

	public void parseTownChatCommand(Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			String line = Colors.Blue + "[" + town.getName() + "] "
					+ player.getDisplayName() + ": "
					+ Colors.LightBlue + msg;
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	public void parseNationChatCommand(Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Nation nation = resident.getTown().getNation();
			String line = Colors.Gold + "[" + nation.getName() + "] "
					+ player.getDisplayName() + ": "
					+ Colors.Yellow + msg;
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	public void parseResidentCommand(Player player, String[] split) {
		/*
		 * /resident
		 * /resident ?
		 * /resident [resident]
		 * /resident list
		 * /resident set [] .. []
		 * /resident friend [add/remove] [resident]
		 * TODO: /resident delete [resident] *Admin
		 */

		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident( player.getName());
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(resident));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, "You are not registered");
			}
		else if (split[0].equalsIgnoreCase("?"))
			showResidentHelp(player);
		else if (split[0].equalsIgnoreCase("list"))
			listResidents(player);
		else if (split[0].equalsIgnoreCase("set")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			residentSet(player, newSplit);
		} else if (split[0].equalsIgnoreCase("friend")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			residentFriend(player, newSplit);
		} else
			try {
				Resident resident = plugin.getTownyUniverse().getResident(split[0]);
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(resident));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, split[0] + " is not registered");
			}
	}

	/**
	 * Send a list of all resident commands to player Command: /resident ?
	 * 
	 * @param player
	 */

	public void showResidentHelp(Player player) {
		player.sendMessage(ChatTools.formatTitle("/resident"));
		player.sendMessage(ChatTools.formatCommand("", "/resident", "", "Your status"));
		player.sendMessage(ChatTools.formatCommand("", "/resident", "[resident]", "Target player's status"));
		player.sendMessage(ChatTools.formatCommand("", "/resident", "list", "List all active players"));
		player.sendMessage(ChatTools.formatCommand("", "/resident", "set [] .. []", "'/resident set' for help"));
		player.sendMessage(ChatTools.formatCommand("", "/resident", "friend [add/remove] [resident]", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("", "/resident", "delete [resident]", ""));
	}

	/**
	 * Send a list of all active residents in the universe to player Command:
	 * /resident list
	 * 
	 * @param player
	 */

	public void listResidents(Player player) {
		player.sendMessage(ChatTools.formatTitle("Residents"));
		String colour;
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Resident resident : plugin.getTownyUniverse().getActiveResidents()) {
			if (resident.isKing())
				colour = Colors.Gold;
			else if (resident.isMayor())
				colour = Colors.LightBlue;
			else
				colour = Colors.White;
			formatedList.add(colour + resident.getName() + Colors.White);
		}
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
	}

	/**
	 * 
	 * Command: /resident set [] ... []
	 * 
	 * @param player
	 * @param split
	 */

	/*
	 * perm [resident/outsider] [build/destroy] [on/off]
	 */

	public void residentSet(Player player, String[] split) {
		if (split.length == 0)
			player.sendMessage(ChatTools.formatCommand("", "/resident set", "perm ...", "'/town set perm' for help"));
		else {
			Resident resident;
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("perm")) {
				String[] newSplit = new String[split.length - 1];
				System.arraycopy(split, 1, newSplit, 0, split.length - 1);
				setTownBlockOwnerPermissions(player, resident, newSplit);
			} else {
				plugin.sendErrorMsg(player, "Invalid town property.");
				return;
			}

			plugin.getTownyUniverse().getDataSource().saveResident(resident);
		}
	}

	public void residentFriend(Player player, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/resident friend", "add [resident]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/resident friend", "remove [resident]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/resident friend", "clearlist", ""));
		} else {
			Resident resident;
			try {
				resident = plugin.getTownyUniverse().getResident(
						player.getName());
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("add")) {
				String[] names = new String[split.length - 1];
				System.arraycopy(split, 1, names, 0, split.length - 1);
				residentFriendAdd(player, resident, getOnlineResidents(player, names));
			} else if (split[0].equalsIgnoreCase("remove")) {
				String[] names = new String[split.length - 1];
				System.arraycopy(split, 1, names, 0, split.length - 1);
				residentFriendRemove(player, resident, getOnlineResidents(player, names));
			} else if (split[0].equalsIgnoreCase("clearlist"))
				residentFriendRemove(player, resident, resident.getFriends());

		}
	}

	public void residentFriendAdd(Player player, Resident resident, List<Resident> invited) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newFriend : invited)
			try {
				resident.addFriend(newFriend);
			} catch (AlreadyRegisteredException e) {
				remove.add(newFriend);
			}
		for (Resident newFriend : remove)
			invited.remove(newFriend);

		if (invited.size() > 0) {
			String msg = "Added ";
			for (Resident newFriend : invited) {
				msg += newFriend.getName() + ", ";
				Player p = plugin.getServer().getPlayer(newFriend.getName());
				if (p != null)
					plugin.sendMsg(p, player.getName() + " added you as a friend.");
			}
			msg += "to your friend list.";
			plugin.sendMsg(player, msg);
			plugin.getTownyUniverse().getDataSource().saveResident(resident);
		} else
			plugin.sendErrorMsg(player, "None of those names were valid.");
	}

	public void residentFriendRemove(Player player, Resident resident, List<Resident> kicking) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident friend : kicking)
			try {
				resident.removeFriend(friend);
			} catch (NotRegisteredException e) {
				remove.add(friend);
			}
		for (Resident friend : remove)
			kicking.remove(friend);

		if (kicking.size() > 0) {
			String msg = "Removed ";
			for (Resident member : kicking) {
				msg += member.getName() + ", ";
				Player p = plugin.getServer().getPlayer(member.getName());
				if (p != null)
					plugin.sendMsg(p, player.getName() + " removed you as a friend.");
			}
			msg += "from your friend list.";
			plugin.sendMsg(player, msg);
			plugin.getTownyUniverse().getDataSource().saveResident(resident);
		} else
			plugin.sendErrorMsg(player, "Non of those names were valid.");
	}

	public void parsePlotCommand(Player player, String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			player.sendMessage(ChatTools.formatCommand("Resident", "/plot claim", "", "Claim this town block"));
		else {
			Resident resident;
			TownyWorld world;
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
				world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			if (split[0].equalsIgnoreCase("claim"))
				try {
					Coord coord = Coord.parseCoord(player);
					residentClaim(resident, world, coord);

					plugin.sendMsg(player, "Successfully claimed (" + coord + ").");

					plugin.updateCache(coord);
					plugin.getTownyUniverse().getDataSource().saveResident(resident);
					plugin.getTownyUniverse().getDataSource().saveWorld(world);
				} catch (TownyException x) {
					plugin.sendErrorMsg(player, x.getError());
				} catch (IConomyException x) {
					plugin.sendErrorMsg(player, x.getError());
				}
		}
	}

	public boolean residentClaim(Resident resident, TownyWorld world, Coord coord) throws TownyException, IConomyException {
		if (plugin.getTownyUniverse().isWarTime())
			throw new TownyException("You cannot do this when the world is at war.");
		
		if (resident.hasTown())
			try {
				TownBlock townBlock = world.getTownBlock(coord);
				Town town = townBlock.getTown();
				if (resident.getTown() != town)
					throw new TownyException("Selected area is not part of your town.");

				try {
					Resident owner = townBlock.getResident();
					throw new AlreadyRegisteredException("This area has already been claimed by: "
									+ owner.getName());
				} catch (NotRegisteredException e) {
					if (TownySettings.isUsingIConomy() && !resident.pay(town.getPlotPrice(), town))
						throw new TownyException("You don't have enough money to purchase this plot");

					townBlock.setResident(resident);
					return true;
				}
			} catch (NotRegisteredException e) {
				throw new TownyException("Selected area is not part of your town.");
			}
		else
			throw new TownyException("You must belong to a town in order to claim plots.");
	}

	public void parseTownCommand(Player player, String[] split) {
		/*
		 * /town
		 * /town ?
		 * /town list
		 * TODO: /town leave 
		 * /town here 
		 * /town spawn
		 * /town claim ... 
		 * /town new [town] [mayor] *Admin
		 * TODO: /town givebonus [town] [bonus] *Admin
		 * TODO: /town delete [town] *Admin
		 * /town add [resident] .. [resident] *Mayor
		 * TODO: /town add+ [resident] *Mayor (For inviting offline residents) 
		 * /town kick [resident] .. [resident] *Mayor
		 * TODO: /town kick+ [resident] *Mayor (For kicking offline residents)
		 * TODO: /town wall 
		 * /town set [] ... [] *Mayor *Admin
		 * /town assistant add [resident] .. [resident]
		 */
		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				Town town = resident.getTown();
				plugin.getTownyUniverse().sendMessage(player,
						plugin.getTownyUniverse().getStatus(town));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, "You don't belong to a town.");
			}
		else if (split[0].equalsIgnoreCase("?"))
			showTownHelp(player);
		else if (split[0].equalsIgnoreCase("here"))
			showTownStatusHere(player);
		else if (split[0].equalsIgnoreCase("list"))
			listTowns(player);
		else if (split[0].equalsIgnoreCase("new")) {
			if (split.length == 1)
				plugin.sendErrorMsg(player, "Specify town name");
			else if (split.length == 2)
				newTown(player, split[1], player.getName());
			else
				// TODO: Check if player is an admin
				newTown(player, split[1], split[2]);
		} else if (split[0].equalsIgnoreCase("add")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			townAdd(player, newSplit);
		} else if (split[0].equalsIgnoreCase("kick")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			townKick(player, newSplit);
		} else if (split[0].equalsIgnoreCase("spawn"))
			townSpawn(player, false);
		else if (split[0].equalsIgnoreCase("claim")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			parseTownClaimCommand(player, newSplit);
		} else if (split[0].equalsIgnoreCase("set")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			townSet(player, newSplit);
		} else if (split[0].equalsIgnoreCase("assistant")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			townAssistant(player, newSplit);
		} else if (split[0].equalsIgnoreCase("leave"))
			townLeave(player);
		else
			try {
				Town town = plugin.getTownyUniverse().getTown(split[0]);
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, split[0]+ " is not registered.");
			}
	}

	/**
	 * Send a list of all town commands to player Command: /town ?
	 * 
	 * @param player
	 */

	public void showTownHelp(Player player) {
		String newTownReq = TownySettings.isTownCreationAdminOnly() ? "Admin" : "";

		player.sendMessage(ChatTools.formatTitle("/town"));
		player.sendMessage(ChatTools.formatCommand("", "/town", "", "Your town's status"));
		player.sendMessage(ChatTools.formatCommand("", "/town", "[town]", "Selected town's status"));
		player.sendMessage(ChatTools.formatCommand("", "/town", "here", "Shortcut to the town's status of your location."));
		player.sendMessage(ChatTools.formatCommand("", "/town", "list", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("", "/town", "leave", ""));
		player.sendMessage(ChatTools.formatCommand("", "/town", "claim", "'/town claim ?' for help"));
		// TODO: player.sendMessage(ChatTools.formatCommand("", "/town", "unclaim", "'/town unclaim ?' for help"));
		player.sendMessage(ChatTools.formatCommand("", "/town", "spawn", "Teleport to town's spawn."));
		player.sendMessage(ChatTools.formatCommand(newTownReq, "/town", "new [town] *[mayor]", "Create a new town."));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "add [resident] .. []", "Add online residents."));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "add+ [resident]", "Add resident"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "kick [resident] .. []", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "kick+ [resident]", "Kick resident"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "set [] .. []", "'/town set' for help"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "assistant [add/remove] [player]", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall [type] [height]", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall remove", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("Admin", "/town", "givebonus [town] [bonus]", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("Admin", "/town", "delete [town]", ""));
	}

	/**
	 * Send a the status of the town the player is physically at to him
	 * 
	 * @param player
	 */

	public void showTownStatusHere(Player player) {
		try {
			TownyWorld world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			Coord coord = Coord.parseCoord(player);
			showTownStatusAtCoord(player, world, coord);
		} catch (TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
		}
	}

	/**
	 * Send a the status of the town at the target coordinates to the player
	 * 
	 * @param player
	 * @param world
	 * @param coord
	 * @throws TownyException
	 */
	public void showTownStatusAtCoord(Player player, TownyWorld world, Coord coord) throws TownyException {
		if (!world.hasTownBlock(coord))
			throw new TownyException("This area (" + coord + ") hasn't been claimed.");

		Town town = world.getTownBlock(coord).getTown();
		plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
	}

	/**
	 * Send a list of all towns in the universe to player Command: /town list
	 * 
	 * @param player
	 */

	public void listTowns(Player player) {
		player.sendMessage(ChatTools.formatTitle("Towns"));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Town town : plugin.getTownyUniverse().getTowns())
			formatedList.add(Colors.LightBlue + town.getName() + Colors.Blue + " [" + town.getNumResidents() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
	}

	/**
	 * Create a new town. Command: /town new [town] *[mayor]
	 * 
	 * @param player
	 */

	public void newTown(Player player, String name, String mayorName) {
		TownyUniverse universe = plugin.getTownyUniverse();
		try {
			if (universe.isWarTime())
				throw new TownyException("You cannot do this when the world is at war.");
			
			Resident resident = universe.getResident(mayorName);
			if (resident.hasTown())
				throw new TownyException(resident.getName() + " already belongs to a town.");

			TownyWorld world = universe.getWorld(player.getWorld().getName());
			Coord key = Coord.parseCoord(player);
			if (world.hasTownBlock(key))
				throw new TownyException("This area (" + key + ") already belongs to someone.");

			if (TownySettings.isUsingIConomy() && !resident.pay(TownySettings.getNewTownPrice()))
				throw new TownyException("You can't afford to settle a new town here.");

			world.newTownBlock(key);
			universe.newTown(name);
			Town town = universe.getTown(name);
			town.addResident(resident);
			town.setMayor(resident);
			TownBlock townBlock = world.getTownBlock(key);
			townBlock.setTown(town);
			town.setHomeBlock(townBlock);
			town.setSpawn(player.getLocation());
			world.addTown(town);

			universe.getDataSource().saveResident(resident);
			universe.getDataSource().saveTown(town);
			universe.getDataSource().saveWorld(world);
			universe.getDataSource().saveTownList();

			plugin.updateCache();
			
			universe.sendGlobalMessage(TownySettings.getNewTownMsg(player.getName(), town.getName()));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			// TODO: delete town data that might have been done
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	public void townLeave(Player player) {
		Resident resident;
		Town town;
		try {
			//TODO: Allow leaving town during war.
			if (plugin.getTownyUniverse().isWarTime()) 
				throw new TownyException("You cannot do this when the world is at war.");
			
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		try {
			town.removeResident(resident);
		} catch (EmptyTownException et) {
			plugin.getTownyUniverse().removeTown(et.getTown());
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		plugin.updateCache();
		
		plugin.getTownyUniverse().sendTownMessage(town, resident.getName() + " left town");
		plugin.sendMsg(player, "You left "+town.getName()+".");
	}
	
	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void townAdd(Player player, String[] names) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			if (!resident.isMayor())
				if (!town.hasAssistant(resident))
					throw new TownyException("You are not the mayor or an assistant.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		townAddResidents(player, town, getOnlineResidents(player, names));
		
		plugin.updateCache();
	}

	// TODO: Move somewhere more useful
	public List<Resident> getOnlineResidents(Player player, String[] names) {
		List<Resident> invited = new ArrayList<Resident>();
		for (String name : names) {
			List<Player> matches = plugin.getServer().matchPlayer(name);
			if (matches.size() > 1) {
				String line = "Multiple players selected";
				for (Player p : matches)
					line += ", " + p.getName();
				plugin.sendErrorMsg(player, line);
			} else if (matches.size() == 1)
				try {
					Resident target = plugin.getTownyUniverse().getResident(matches.get(0).getName());
					invited.add(target);
				} catch (TownyException x) {
					plugin.sendErrorMsg(player, x.getError());
				}
		}
		return invited;
	}

	public void townAddResidents(Player player, Town town, List<Resident> invited) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newMember : invited)
			try {
				town.addResident(newMember);
				plugin.getTownyUniverse().getDataSource().saveResident(newMember);
			} catch (AlreadyRegisteredException e) {
				remove.add(newMember);
			}
		for (Resident newMember : remove)
			invited.remove(newMember);

		if (invited.size() > 0) {
			String msg = player.getName() + " invited ";
			for (Resident newMember : invited)
				msg += newMember.getName() + ", ";
			msg += "to town.";
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else
			plugin.sendErrorMsg(player, "None of those names were valid.");
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and kick them from town. Command: /town kick
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void townKick(Player player, String[] names) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			if (!resident.isMayor())
				if (!town.hasAssistant(resident))
					throw new TownyException("You are not the mayor or an assistant.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		townKickResidents(player, resident, town, getOnlineResidents(player, names));
		
		plugin.updateCache();
	}

	public void townKickResidents(Player player, Resident resident, Town town, List<Resident> kicking) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident member : kicking)
			if (resident == member || member.isMayor() || town.hasAssistant(member))
				remove.add(member);
			else
				try {
					town.removeResident(member);
					plugin.getTownyUniverse().getDataSource().saveResident(member);
				} catch (NotRegisteredException e) {
					remove.add(member);
				} catch (EmptyTownException e) {
					// You can't kick yourself and only the mayor can kick
					// assistants
					// so there will always be at least one resident.
				}
		for (Resident member : remove)
			kicking.remove(member);

		if (kicking.size() > 0) {
			String msg = player.getName() + " kicked ";
			for (Resident member : kicking) {
				msg += member.getName() + ", ";
				Player p = plugin.getServer().getPlayer(member.getName());
				if (p != null)
					p.sendMessage("You were kicked from town by " + player.getName());
			}
			msg += "from town.";
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else
			plugin.sendErrorMsg(player, "Non of those names were valid.");
	}
	
	public void townAssistant(Player player, String[] split) {
		if (split.length == 0) {
			//TODO: assistant help
		} else if (split[0].equalsIgnoreCase("add")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			townAssistantsAdd(player, newSplit);
		} else if (split[0].equalsIgnoreCase("remove")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			townAssistantsRemove(player, newSplit);
		}
	}
	
	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void townAssistantsAdd(Player player, String[] names) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			if (!resident.isMayor())
				throw new TownyException("You are not the mayor.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		townAssistantsAdd(player, town, getOnlineResidents(player, names));
	}

	public void townAssistantsAdd(Player player, Town town, List<Resident> invited) {
		//TODO: change variable names from townAdd copypasta
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newMember : invited)
			try {
				town.addAssistant(newMember);
				plugin.getTownyUniverse().getDataSource().saveResident(newMember);
			} catch (AlreadyRegisteredException e) {
				remove.add(newMember);
			}
		for (Resident newMember : remove)
			invited.remove(newMember);

		if (invited.size() > 0) {
			String msg = player.getName() + " raised ";
			for (Resident newMember : invited)
				msg += newMember.getName() + ", ";
			msg += "to town assistants.";
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else
			plugin.sendErrorMsg(player, "None of those names were valid.");
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and kick them from town. Command: /town kick
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void townAssistantsRemove(Player player, String[] names) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			if (!resident.isMayor())
				throw new TownyException("You are not the mayor.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		townAssistantsRemove(player, resident, town, getOnlineResidents(player, names));
	}

	public void townAssistantsRemove(Player player, Resident resident, Town town, List<Resident> kicking) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident member : kicking)
			try {
				town.removeAssistant(member);
				plugin.getTownyUniverse().getDataSource().saveResident(member);
			} catch (NotRegisteredException e) {
				remove.add(member);
			}
		for (Resident member : remove)
			kicking.remove(member);

		if (kicking.size() > 0) {
			String msg = player.getName() + " removed ";
			for (Resident member : kicking) {
				msg += member.getName() + ", ";
				Player p = plugin.getServer().getPlayer(member.getName());
				if (p != null)
					p.sendMessage("You were lowered to a regular resident by " + player.getName());
			}
			msg += "were lowered to a regular resident.";
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveTown(town);
		} else
			plugin.sendErrorMsg(player, "Non of those names were valid.");
	}

	public void parseTownClaimCommand(Player player, String[] split) {
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town claim"));
			player.sendMessage(ChatTools.formatCommand("", "/town claim", "", "Claim this town block"));
			// TODO: player.sendMessage(ChatTools.formatCommand("", "/town claim", "auto", "Automatically expand town area till max"));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim", "rect [radius]", "Attempt to claim around you."));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim", "rect auto", "Detemine the maximum radius"));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				if (plugin.getTownyUniverse().isWarTime())
					throw new TownyException("You cannot do this when the world is at war.");
				
				resident = plugin.getTownyUniverse().getResident(player.getName());
				town = resident.getTown();
				if (!resident.isMayor())
					if (!town.hasAssistant(resident))
						throw new TownyException("You are not the mayor or an assistant.");
				world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			if (split.length == 0)
				try {
					int available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();

					if (available - 1 < 0)
						throw new TownyException("You've already claimed your maximum amount of town blocks.");

					townClaim(town, world, Coord.parseCoord(player), true);

					plugin.getTownyUniverse().getDataSource().saveTown(town);
					plugin.getTownyUniverse().getDataSource().saveWorld(world);
					
					plugin.updateCache();
				} catch (TownyException x) {
					plugin.sendErrorMsg(player, x.getError());
				}
			else if (split[0].equalsIgnoreCase("rect")) {
				String[] newSplit = new String[split.length - 1];
				System.arraycopy(split, 1, newSplit, 0, split.length - 1);
				townClaimRect(player, newSplit, resident, town, world);
				
				plugin.updateCache();
			} else if (split[0].equalsIgnoreCase("auto")) {
				// TODO: Attempt to claim edge blocks recursively.
			}
		}
	}

	public void townClaimRect(Player player, String[] split, Resident resident, Town town, TownyWorld world) {
		int available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town claim rect"));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim rect", "[radius]", "Claim around you."));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim rect", "auto", "Detemine the maximum radius"));
		} else {
			int r = 0;
			Coord pos = Coord.parseCoord(player);

			if (split[0].equalsIgnoreCase("auto")) {
				// Automatically determine the maximum area to claim
				// by taking the radius and getting the diameter and
				// checking if the town has enough available town blocks
				r = 0;
				while (available - Math.pow((r + 1) * 2 - 1, 2) >= 0)
					r++;
			} else
				try {
					r = Integer.parseInt(split[0]);
				} catch (NumberFormatException e) {
					plugin.sendErrorMsg(player, "Invalid radius. Use an integer or 'auto'.");
					return;
				}

			// A radius of 1 requires expanding the selected position by 0,
			// or the target radius - 1.
			r--;

			List<Coord> coords = new ArrayList<Coord>();
			for (int z = pos.getZ() - r; z <= pos.getZ() + r; z++)
				for (int x = pos.getX() - r; x <= pos.getX() + r; x++)
					coords.add(new Coord(x, z));

			player.sendMessage(String.format("Claiming %d town blocks within the radius of %d.", (int)Math.pow((r + 1) * 2 - 1, 2), r));
			try {
				int n = townClaim(town, world, coords.toArray(new Coord[0]));
				if (n > 0) {
					plugin.getTownyUniverse().getDataSource().saveTown(town);
					plugin.getTownyUniverse().getDataSource().saveWorld(world);

					player.sendMessage("Successfully claimed " + n + " town blocks.");
				} else
					plugin.sendErrorMsg(player, "None of the selected townblocks were valid to claim.");
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
			}
		}
	}

	public int townClaim(Town town, TownyWorld world, Coord[] coords) throws TownyException {
		if (!isTownEdge(town, world, coords))
			throw new TownyException("Selected area is not connected to town.");

		int n = 0;
		for (Coord coord : coords)
			try {
				if (townClaim(town, world, coord, false))
					n++;
			} catch (TownyException e) {
				// Ignore complaints
			}
		return n;
	}

	public boolean townClaim(Town town, TownyWorld world, Coord coord, boolean checkEdge) throws TownyException {
		try {
			TownBlock townBlock = world.getTownBlock(coord);
			try {
				throw new AlreadyRegisteredException("This area has already been claimed by: " + townBlock.getTown().getName());
			} catch (NotRegisteredException e) {
				throw new AlreadyRegisteredException("This area has already been claimed.");
			}
		} catch (NotRegisteredException e) {
			if (checkEdge && town.getTownBlocks().size() > 0)
				if (!isTownEdge(town, world, coord))
					throw new TownyException("Selected area is not connected to town.");
			TownBlock townBlock = world.newTownBlock(coord);
			townBlock.setTown(town);
			return true;
		}
	}

	public boolean isTownEdge(Town town, TownyWorld world, Coord[] coords) {
		// TODO: Better algorithm that doesn't duplicates checks.

		for (Coord coord : coords)
			if (isTownEdge(town, world, coord))
				return true;
		return false;
	}

	public boolean isTownEdge(Town town, TownyWorld world, Coord coord) {

		System.out.print("[Towny] Debug: isTownEdge(" + coord.toString() + ") = ");
		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = world.getTownBlock(new Coord(coord.getX() + offset[i][0], coord.getZ() + offset[i][1]));
				if (edgeTownBlock.getTown() == town) {
					if (TownySettings.getDebug())
						System.out.println("true");
					return true;
				}
			} catch (NotRegisteredException e) {
			}
		if (TownySettings.getDebug())
			System.out.println("false");
		return false;
	}

	/**
	 * 
	 * Command: /town set [] ... []
	 * 
	 * @param player
	 * @param split
	 */

	/*
	 * board [message ... ] mayor [mayor] *[town] homeblock spawn perm
	 * [resident/outsider/ally] [build/destroy] [on/off] pvp [on/off] taxes [$]
	 */

	public void townSet(Player player, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/town set"));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "board [message ... ]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "mayor [mayor] *[town]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "homeblock", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "spawn", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "perm ...", "'/town set perm' for help"));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "pvp [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "taxes [$]", ""));
		} else {
			Resident resident;
			Town town;
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
				town = resident.getTown();
				if (!resident.isMayor())
					if (!town.hasAssistant(resident))
						throw new TownyException("You are not the mayor or an assistant.");
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("board")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /town set board Today's the day!");
				else {
					String line = split[1];
					for (int i = 2; i < split.length; i++)
						line += " " + split[i];
					town.setTownBoard(line);
					plugin.getTownyUniverse().sendTownBoard(player, town);
				}
			} else if (split[0].equalsIgnoreCase("mayor")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /town set mayor Dumbo");
				else
					try {
						Resident newMayor = plugin.getTownyUniverse().getResident(split[1]);
						town.setMayor(newMayor);
						plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getNewMayorMsg(newMayor.getName()));
					} catch (TownyException e) {
						plugin.sendErrorMsg(player, e.getError());
					}
			} else if (split[0].equalsIgnoreCase("taxes")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /town set taxes 7");
				else
					try {
						town.setTaxes(Integer.parseInt(split[1]));
						plugin.getTownyUniverse().sendTownMessage(town, player.getName() + " has set the taxes at " + split[1]);
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, "Taxes must be an interger.");
					}
			} else if (split[0].equalsIgnoreCase("name")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /town set name BillyBobTown");
				else
					townRename(player, town, split[1]);
			} else if (split[0].equalsIgnoreCase("homeblock")) {
				Coord coord = Coord.parseCoord(player);
				TownBlock townBlock;
				try {
					if (plugin.getTownyUniverse().isWarTime())
						throw new TownyException("You cannot do this when the world is at war.");
					
					townBlock = plugin.getTownyUniverse().getWorld(player.getWorld().getName()).getTownBlock(coord);
					town.setHomeBlock(townBlock);
					plugin.sendMsg(player, "Successfully changed town's home block to " + coord.toString());
				} catch (TownyException e) {
					plugin.sendErrorMsg(player, e.getError());
				}
			} else if (split[0].equalsIgnoreCase("spawn"))
				try {
					town.setSpawn(player.getLocation());
					plugin.sendMsg(player, "Successfully changed town's spawn.");
				} catch (TownyException e) {
					plugin.sendErrorMsg(player, e.getError());
				}
			else if (split[0].equalsIgnoreCase("perm")) {
				String[] newSplit = new String[split.length - 1];
				System.arraycopy(split, 1, newSplit, 0, split.length - 1);
				setTownBlockOwnerPermissions(player, town, newSplit);
			} else if (split[0].equalsIgnoreCase("pvp")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /town set pvp [on/off]");
				else
					try {
						town.setPVP(parseOnOff(split[1]));
						plugin.sendMsg(player, "Successfully changed town's pvp setting.");
						// TODO: send message to all with town
					} catch (Exception e) {
					}
			} else {
				plugin.sendErrorMsg(player, "Invalid town property.");
				return;
			}

			plugin.getTownyUniverse().getDataSource().saveTown(town);
		}
	}

	public void setTownBlockOwnerPermissions(Player player, TownBlockOwner townBlockOwner, String[] split) {
		// TODO: switches
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/... set perm"));
			player.sendMessage(ChatTools.formatCommand("", "", "[on/off]", "Toggle all permissions"));
			player.sendMessage(ChatTools.formatCommand("", "", "[resident/ally/outsider] [on/off]", "Toggle specifics"));
			player.sendMessage(ChatTools.formatCommand("", "", "[resident/ally/outsider] [build/destroy] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("Eg", "/town set", "ally off", ""));
			player.sendMessage(ChatTools.formatCommand("Eg", "/resident set", "resident build on", ""));
			player.sendMessage("'resident' is equivalent to 'friends' for plot permissions.");
			player.sendMessage("Resident plots don't make use of outsider permissions.");
		} else {
			TownyPermission perm = townBlockOwner.getPermissions();
			if (split.length == 1)
				try {
					perm.setAll(parseOnOff(split[0]));
				} catch (Exception e) {
				}
			else if (split.length == 2)
				try {
					boolean b = parseOnOff(split[1]);
					if (split[0].equalsIgnoreCase("resident")) {
						perm.residentBuild = b;
						perm.residentDestroy = b;
					} else if (split[0].equalsIgnoreCase("outsider")) {
						perm.outsiderBuild = b;
						perm.outsiderDestroy = b;
					} else if (split[0].equalsIgnoreCase("ally")) {
						perm.allyBuild = b;
						perm.allyDestroy = b;
					}
				} catch (Exception e) {
				}
			else if (split.length == 3)
				try {
					boolean b = parseOnOff(split[2]);
					String s = "";
					if ((split[0].equalsIgnoreCase("resident") || split[0].equalsIgnoreCase("outsider") || split[0].equalsIgnoreCase("ally"))
							&& (split[1].equalsIgnoreCase("build") || split[1].equalsIgnoreCase("destroy")))
						s = split[0] + split[1];
					perm.set(s, b);
				} catch (Exception e) {
				}
			plugin.sendMsg(player, "Successfully changed permissions to " + townBlockOwner.getPermissions().toString() + ".");
		}
	}

	public boolean parseOnOff(String s) throws Exception {
		if (s.equalsIgnoreCase("on"))
			return true;
		else if (s.equalsIgnoreCase("off"))
			return false;
		else
			throw new Exception();
	}

	public void townRename(Player player, Town town, String newName) {
		try {
			plugin.getTownyUniverse().renameTown(town, newName);
			plugin.getTownyUniverse().sendTownMessage(town, player.getName() + " renamed town to " + newName);
		} catch (TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
		}

	}

	/**
	 * Teleports the player to his town's spawn location. If town doesn't have a
	 * spawn or player has no town, and teleport is forced, then player is sent
	 * to the world's spawn location.
	 * 
	 * @param player
	 * @param forceTeleport
	 */

	public void townSpawn(Player player, boolean forceTeleport) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			player.teleportTo(town.getSpawn());
		} catch (TownyException x) {
			if (forceTeleport) {
				// TODO: When API supports:
				// player.teleportTo(player.getWorld().getSpawnLocation());
			} else
				plugin.sendErrorMsg(player, x.getError());
		}
	}

	public void parseNationCommand(Player player, String[] split) {
		/*
		 * /nation
		 * /nation list
		 * /nation [nation]
		 * TODO: /nation leave *Mayor
		 * /nation new [nation] [capital] *Admin
		 * TODO: /nation delete [nation]*Admin
		 * TODO: /nation add [town] *King
		 * TODO: /nation kick [town] *King
		 */

		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				Town town = resident.getTown();
				Nation nation = town.getNation();
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(nation));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, "You don't belong to a nation.");
			}
		else if (split[0].equalsIgnoreCase("?"))
			showNationHelp(player);
		else if (split[0].equalsIgnoreCase("list"))
			listNations(player);
		else if (split[0].equalsIgnoreCase("new")) {
			// TODO: Make an overloaded function
			// newNation(Player,String,Town)
			if (split.length == 1)
				plugin.sendErrorMsg(player, "Specify nation name.");
			else if (split.length == 2)
				try { // TODO: Make sure of the error catching
					Resident resident = plugin.getTownyUniverse().getResident(player.getName());
					newNation(player, split[1], resident.getTown().getName());
				} catch (TownyException x) {
					plugin.sendErrorMsg(player, x.getError());
				}
			else
				// TODO: Check if player is an admin
				newNation(player, split[1], split[2]);
		} else if (split[0].equalsIgnoreCase("leave"))
			nationLeave(player);
		else if (split[0].equalsIgnoreCase("add")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			nationAdd(player, newSplit);
		} else if (split[0].equalsIgnoreCase("kick")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			nationKick(player, newSplit);
		} else if (split[0].equalsIgnoreCase("assistant")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			nationAssistant(player, newSplit);
		} else if (split[0].equalsIgnoreCase("set")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			nationSet(player, newSplit);
		} else 
			try {
				Nation nation = plugin.getTownyUniverse().getNation(split[0]);
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(nation));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, split[0] + " is not registered.");
			}
	}

	/**
	 * Send a list of all nation commands to player Command: /nation ?
	 * 
	 * @param player
	 */

	public void showNationHelp(Player player) {
		String newTownReq = TownySettings.isTownCreationAdminOnly() ? "Admin" : "";

		player.sendMessage(ChatTools.formatTitle("/nation"));
		player.sendMessage(ChatTools.formatCommand("", "/nation", "", "Your nation's status"));
		player.sendMessage(ChatTools.formatCommand("", "/nation", "[nation]", "Target nation's status"));
		player.sendMessage(ChatTools.formatCommand("", "/nation", "list", "List all nations"));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "leave", "Leave your nation"));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "assistant [add/remove]", "Leave your nation"));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "set [] .. []", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("King", "/nation", "ally [add/remove] [nation]", "Set you alliance."));
		// TODO: player.sendMessage(ChatTools.formatCommand("King", "/nation", "enemy [add/remove] [nation]", "Set you enemys."));
		player.sendMessage(ChatTools.formatCommand(newTownReq, "/nation", "new [nation] *[capital]", "Create a new nation"));
		// TODO: player.sendMessage(ChatTools.formatCommand("Admin", "/nation", "delete [nation]", ""));
	}

	/**
	 * Send a list of all nations in the universe to player Command: /nation
	 * list
	 * 
	 * @param player
	 */

	public void listNations(Player player) {
		player.sendMessage(ChatTools.formatTitle("Nations"));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Nation nation : plugin.getTownyUniverse().getNations())
			formatedList.add(Colors.LightBlue + nation.getName() + Colors.Blue + " [" + nation.getNumTowns() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
	}

	/**
	 * Create a new nation. Command: /nation new [nation] *[capital]
	 * 
	 * @param player
	 */

	public void newNation(Player player, String name, String capitalName) {
		TownyUniverse universe = plugin.getTownyUniverse();
		try {
			Town town = universe.getTown(capitalName);
			if (town.hasNation())
				throw new TownyException("Target already belongs to a nation.");

			universe.newNation(name);
			Nation nation = universe.getNation(name);
			nation.addTown(town);
			nation.setCapital(town);

			universe.getDataSource().saveTown(town);
			universe.getDataSource().saveNation(nation);
			universe.getDataSource().saveNationList();

			universe.sendGlobalMessage(TownySettings.getNewNationMsg(player.getName(), nation.getName()));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			// TODO: delete town data that might have been done
		}
	}
	
	public void nationLeave(Player player) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			Nation nation = town.getNation();
			if (!resident.isMayor())
				if (!town.hasAssistant(resident))
					throw new TownyException("You are not the mayor or an assistant.");
			nation.removeTown(town);
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		} catch (EmptyNationException en) {
			plugin.getTownyUniverse().removeNation(en.getNation());
		}
	}
	
	public void nationAdd(Player player, String[] names) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				if (!nation.hasAssistant(resident))
					throw new TownyException("You are not the king or an assistant.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		nationAdd(player, nation, plugin.getTownyUniverse().getTowns(names));
	}

	public void nationAdd(Player player, Nation nation, List<Town> invited) {
		ArrayList<Town> remove = new ArrayList<Town>();
		for (Town town : invited)
			try {
				nation.addTown(town);
				plugin.getTownyUniverse().getDataSource().saveTown(town);
			} catch (AlreadyRegisteredException e) {
				remove.add(town);
			}
		for (Town town : remove)
			invited.remove(town);

		if (invited.size() > 0) {
			String msg = player.getName() + " invited ";
			for (Town town : invited)
				msg += town.getName() + ", ";
			msg += "to the nation.";
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, "None of those names were valid.");
	}
	
	public void nationKick(Player player, String[] names) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				if (!nation.hasAssistant(resident))
					throw new TownyException("You are not the king or an assistant.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		nationKick(player, resident, nation, plugin.getTownyUniverse().getTowns(names));
	}

	public void nationKick(Player player, Resident resident, Nation nation, List<Town> kicking) {
		ArrayList<Town> remove = new ArrayList<Town>();
		for (Town town : kicking)
			if (town.isCapital())
				remove.add(town);
			else
				try {
					nation.removeTown(town);
					plugin.getTownyUniverse().getDataSource().saveTown(town);
				} catch (NotRegisteredException e) {
					remove.add(town);
				} catch (EmptyNationException e) {
					// You can't kick yourself and only the mayor can kick assistants
					// so there will always be at least one resident.
				}
		for (Town town : remove)
			kicking.remove(town);

		if (kicking.size() > 0) {
			String msg = player.getName() + " kicked ";
			for (Town town : kicking) {
				msg += town.getName() + ", ";
				plugin.getTownyUniverse().sendTownMessage(town, "Your town was kicked from the nation by " + player.getName());
			}
			msg += "from the nation.";
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, "Non of those names were valid.");
	}
	
	public void nationAssistant(Player player, String[] split) {
		if (split.length == 0) {
			//TODO: assistant help
		} else if (split[0].equalsIgnoreCase("add")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			nationAssistantsAdd(player, newSplit);
		} else if (split[0].equalsIgnoreCase("remove")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			nationAssistantsRemove(player, newSplit);
		}
	}
	
	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void nationAssistantsAdd(Player player, String[] names) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				throw new TownyException("You are not the king.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		nationAssistantsAdd(player, nation, plugin.getTownyUniverse().getResidents(names));
	}

	public void nationAssistantsAdd(Player player, Nation nation, List<Resident> invited) {
		//TODO: change variable names from townAdd copypasta
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newMember : invited)
			try {
				nation.addAssistant(newMember);
				plugin.getTownyUniverse().getDataSource().saveResident(newMember);
			} catch (AlreadyRegisteredException e) {
				remove.add(newMember);
			}
		for (Resident newMember : remove)
			invited.remove(newMember);

		if (invited.size() > 0) {
			String msg = player.getName() + " raised ";
			for (Resident newMember : invited)
				msg += newMember.getName() + ", ";
			msg += "to nation assistants.";
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, "None of those names were valid.");
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and kick them from town. Command: /town kick
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void nationAssistantsRemove(Player player, String[] names) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				throw new TownyException("You are not the king.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		nationAssistantsRemove(player, resident, nation, plugin.getTownyUniverse().getResidents(names));
	}

	public void nationAssistantsRemove(Player player, Resident resident, Nation nation, List<Resident> kicking) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident member : kicking)
			try {
				nation.removeAssistant(member);
				plugin.getTownyUniverse().getDataSource().saveResident(member);
			} catch (NotRegisteredException e) {
				remove.add(member);
			}
		for (Resident member : remove)
			kicking.remove(member);

		if (kicking.size() > 0) {
			String msg = player.getName() + " removed ";
			for (Resident member : kicking) {
				msg += member.getName() + ", ";
				Player p = plugin.getServer().getPlayer(member.getName());
				if (p != null)
					p.sendMessage("You were lowered to a regular resident by " + player.getName());
			}
			msg += "were lowered to a regular resident.";
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, "Non of those names were valid.");
	}
	
	public void nationSet(Player player, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/nation set"));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "king [resident] *[nation]", ""));
			//TODO: player.sendMessage(ChatTools.formatCommand("", "/nation set", "capital [town] *[nation]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "taxes [$]", ""));
		} else {
			Resident resident;
			Nation nation;
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
				nation = resident.getTown().getNation();
				if (!resident.isKing())
					if (!nation.hasAssistant(resident))
						throw new TownyException("You are not the king or an assistant.");
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("king")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /nation set king Dumbo");
				else
					try {
						Resident newKing = plugin.getTownyUniverse().getResident(split[1]);
						nation.setKing(newKing);
						plugin.getTownyUniverse().sendNationMessage(nation, TownySettings.getNewKingMsg(newKing.getName()));
					} catch (TownyException e) {
						plugin.sendErrorMsg(player, e.getError());
					}
			} else if (split[0].equalsIgnoreCase("taxes")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /nation set taxes 70");
				else
					try {
						nation.setTaxes(Integer.parseInt(split[1]));
						plugin.getTownyUniverse().sendNationMessage(nation, player.getName() + " has set the taxes at " + split[1]);
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, "Taxes must be an interger.");
					}
			} else if (split[0].equalsIgnoreCase("name")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /nation set name Plutoria");
				else
					nationRename(player, nation, split[1]);
			} else if (split[0].equalsIgnoreCase("neutral")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /town set neutral [on/off]");
				else
					try {
						nation.setNeutral(parseOnOff(split[1]));
						plugin.sendMsg(player, "Successfully changed nation's neutrality.");
						plugin.getTownyUniverse().sendNationMessage(nation, "You nation is now" + (nation.isNeutral() ? Colors.Green : Colors.Red + " not") + " neutral.");
					} catch (Exception e) {
					}
			} else {
				plugin.sendErrorMsg(player, "Invalid nation property.");
				return;
			}

			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		}
	}
	
	public void nationRename(Player player, Nation nation, String newName) {
		try {
			plugin.getTownyUniverse().renameNation(nation, newName);
			plugin.getTownyUniverse().sendNationMessage(nation, player.getName() + " renamed nation to " + newName);
		} catch (TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
		}

	}

	public void parseTownyCommand(Player player, String[] split) {
		/*
		 * /towny
		 * /towny ?
		 * /towny map
		 * /towny version
		 * /towny universe
		 * /towny war stats
		 * /towny war scores
		 * 
		 * Debug only:
		 * /towny seed
		 * /towny tree
		 * /towny newday
		 */

		if (split.length == 0)
			showHelp(player);
		else if (split[0].equalsIgnoreCase("?"))
			showTownyHelp(player);
		else if (split[0].equalsIgnoreCase("map"))
			showMap(player);
		else if (split[0].equalsIgnoreCase("version"))
			showTownyVersion(player);
		else if (split[0].equalsIgnoreCase("universe"))
			showUniverseStats(player);
		else if (split[0].equalsIgnoreCase("war"))
				if (split.length == 2) {
					if (plugin.getTownyUniverse().isWarTime()) {
						if (split[1].equalsIgnoreCase("stats"))
							plugin.getTownyUniverse().getWarEvent().sendStats(player);
						else if (split[1].equalsIgnoreCase("scores"))
							plugin.getTownyUniverse().getWarEvent().sendScores(player);
					} else //TODO: Remove smartassery
						plugin.sendErrorMsg(player, "The world isn't currently going to hell.");
				}
		else if (TownySettings.getDebug())
			if (split[0].equalsIgnoreCase("tree"))
				showUniverseTree();
			else if (split[0].equalsIgnoreCase("seed"))
				seedTowny();
			else if (split[0].equalsIgnoreCase("newday"))
				plugin.getTownyUniverse().newDay();
	}

	/**
	 * Show a general help and list other help commands to player Command:
	 * /towny
	 * 
	 * @param player
	 */

	public void showHelp(Player player) {
		player.sendMessage(ChatTools.formatTitle("General Towny Help"));
		player.sendMessage("Try the following commands to learn more about towny.");
		player.sendMessage(ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getResidentCommands()), "?", "")
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getTownCommands()), "?", "") 
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getNationCommands()), "?", "")
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getPlotCommands()), "?", "")
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getTownyCommands()), "?", ""));
		player.sendMessage(ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getTownChatCommands()), " [msg]", "Town Chat")
				+ ", " + ChatTools.formatCommand("", TownySettings.getFirstCommand(TownySettings.getNationChatCommands()), " [msg]", "Nation Chat"));
		player.sendMessage(ChatTools.formatCommand("Admin", TownySettings.getFirstCommand(TownySettings.getTownyAdminCommands()), "?", ""));
	}

	/**
	 * Send a list of all towny commands to player Command: /towny ?
	 * 
	 * @param player
	 */

	public void showTownyHelp(Player player) {
		player.sendMessage(ChatTools.formatTitle("/towny"));
		player.sendMessage(ChatTools.formatCommand("", "/towny", "", "General help for Towny"));
		player.sendMessage(ChatTools.formatCommand("", "/towny", "map", "Displays a map of the nearby townblocks"));
		player.sendMessage(ChatTools.formatCommand("", "/towny", "version", "Displays the version of Towny"));
		player.sendMessage(ChatTools.formatCommand("", "/towny", "universe", "Displays stats"));
	}

	/**
	 * Send a map of the nearby townblocks status to player Command: /towny map
	 * 
	 * @param player
	 */

	public void showMap(Player player) {
		TownyUniverse universe = plugin.getTownyUniverse();
		boolean hasTown = false;
		Resident resident;
		int lineCount = 0;

		try {
			resident = universe.getResident(player.getName());
			if (resident.hasTown())
				hasTown = true;
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		TownyWorld world;
		try {
			world = universe.getWorld(player.getWorld().getName());
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, "You are not in a registered world.");
			return;
		}
		Coord pos = Coord.parseCoord(player);

		player.sendMessage(ChatTools.formatTitle("Towny Map " + Colors.White + "(" + pos.toString() + ")"));

		String[][] townyMap = new String[31][7];
		int x, y = 0;
		for (int tby = pos.getZ() - 15; tby <= pos.getZ() + 15; tby++) {
			x = 0;
			for (int tbx = pos.getX() - 3; tbx <= pos.getX() + 3; tbx++) {
				try {
					TownBlock townblock = world.getTownBlock(tbx, tby);
					//TODO: possibly claim outside of towns
					if (!townblock.hasTown())
						throw new TownyException();
					if (x == 3 && y == 15)
						// location
						townyMap[y][x] = Colors.Gold;
					else if (hasTown) {
						if (resident.getTown() == townblock.getTown())
							if (resident == townblock.getResident())
								//own plot
								townyMap[y][x] = Colors.Yellow;
							else
								// own town
								townyMap[y][x] = Colors.LightGreen;
						else if (resident.hasNation()) {
							if (resident.getTown().getNation().hasTown(townblock.getTown()))
								// towns
								townyMap[y][x] = Colors.Green;
							else if (townblock.getTown().hasNation()) {
								Nation nation = resident.getTown().getNation();
								if (nation.hasAlly(townblock.getTown().getNation()))
									townyMap[y][x] = Colors.Green;
								else if (nation.hasEnemy(townblock.getTown().getNation()))
									// towns
									townyMap[y][x] = Colors.Red;
								else
									townyMap[y][x] = Colors.White;
							} else
								townyMap[y][x] = Colors.White;
						} else
							townyMap[y][x] = Colors.White;
					} else
						townyMap[y][x] = Colors.White;

					// Registered town block
					townyMap[y][x] += "+";
				} catch (TownyException e) {
					if (x == 3 && y == 15)
						townyMap[y][x] = Colors.Gold;
					else
						townyMap[y][x] = Colors.Gray;

					// Unregistered town block
					townyMap[y][x] += "-";
				}
				x++;
			}
			y++;
		}

		Compass.Point dir = Compass.getCompassPointForDirection(player.getLocation().getYaw());
		
		String[] compass = {
				Colors.Black + "  -----  ",
				Colors.Black + "  -" + (dir == Compass.Point.NW ? Colors.Gold + "\\" : "-")
				+ (dir == Compass.Point.N ? Colors.Gold : Colors.White) + "N"
				+ (dir == Compass.Point.NE ? Colors.Gold + "/" + Colors.Black : Colors.Black + "-") + "-  ",
				Colors.Black + "  -" + (dir == Compass.Point.W ? Colors.Gold + "W" : Colors.White + "W") + Colors.LightGray + "+"
				+ (dir == Compass.Point.E ? Colors.Gold : Colors.White) + "E" + Colors.Black  + "-  ",
				Colors.Black + "  -" + (dir == Compass.Point.SW ? Colors.Gold + "/" : "-")
				+ (dir == Compass.Point.S ? Colors.Gold : Colors.White) + "S"
				+ (dir == Compass.Point.SE ? Colors.Gold + "\\" + Colors.Black : Colors.Black + "-") + "-  "};

		String[] help = {
				"  " + Colors.Gray + "-" + Colors.LightGray + " = Unclaimed",
				"  " + Colors.Gray + "+" + Colors.LightGray + " = Claimed",
				"  " + Colors.LightGreen + "+" + Colors.LightGray + " = Your town",
				"  " + Colors.Yellow + "+" + Colors.LightGray + " = Your plot",
				"  " + Colors.Green + "+" + Colors.LightGray + " = Ally",
				"  " + Colors.Red + "+" + Colors.LightGray + " = Enemy",
				"  " + Colors.White + "+" + Colors.LightGray + " = Other"
		};
		
		String line;
		// Variables have been rotated to fit N/S/E/W properly
		for (int my = 0; my < 7; my++) {
			line = compass[0];
			if (lineCount < compass.length)
				line = compass[lineCount];

			for (int mx = 30; mx >= 0; mx--)
				line += townyMap[mx][my];
			
			if (lineCount < help.length)
				line += help[lineCount];
			
			player.sendMessage(line);
			lineCount++;
		}

		// Current town block data
		try {
			TownBlock townblock = world.getTownBlock(pos);
			plugin.sendMsg(player, ("Town: " + (townblock.hasTown() ? townblock.getTown().getName() : "None") + " : "
					+ "Owner: " + (townblock.hasResident() ? townblock.getResident().getName() : "None")));
		} catch (TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
		}
	}

	/**
	 * Send the version of towny to player Command: /towny version
	 * 
	 * @param player
	 */

	public void showTownyVersion(Player player) {
		player.sendMessage("Towny version: " + plugin.getVersion());
	}

	/**
	 * Send some stats about the towny universe to the player Command: /towny
	 * universe
	 * 
	 * @param player
	 */

	public void showUniverseStats(Player player) {
		player.sendMessage("§0-§4###§0---§4###§0-");
		player.sendMessage("§4#§c###§4#§0-§4#§c###§4#§0   §6[§eTowny " + plugin.getVersion() + "§6]");
		player.sendMessage("§4#§c####§4#§c####§4#   §3By: §bChris H (Shade)");
		player.sendMessage("§0-§4#§c#######§4#§0-");
		player.sendMessage("§0--§4##§c###§4##§0-- §3Residents: §b" + Integer.toString(plugin.getTownyUniverse().getResidents().size()));
		player.sendMessage("§0----§4#§c#§4#§0---- §3Towns: §b" + Integer.toString(plugin.getTownyUniverse().getTowns().size()));
		player.sendMessage("§0-----§4#§0----- §3Nations: §b" + Integer.toString(plugin.getTownyUniverse().getNations().size()));
	}

	/**
	 * Show the current universe in the console. Command: /towny tree
	 */

	public void showUniverseTree() {
		TownyUniverse universe = plugin.getTownyUniverse();
		System.out.println("|-Universe");
		for (TownyWorld world : universe.getWorlds()) {
			System.out.println("|---World: " + world.getName());
			for (TownBlock townBlock : world.getTownBlocks())
				try {
					System.out.println("|------TownBlock: " + townBlock.getX() + "," + townBlock.getZ() + " "
							+ "Town: " + (townBlock.hasTown() ? townBlock.getTown().getName() : "None") + " : "
							+ "Owner: " + (townBlock.hasResident() ? townBlock.getResident().getName() : "None"));
				} catch (TownyException e) {
				}
			for (Resident resident : universe.getResidents()) {
				try {
					System.out.println("|---Resident: " + resident.getName()
							+ " " + (resident.hasTown() ? resident.getTown().getName() : "")
							+ (resident.hasNation() ? resident.getTown().getNation().getName() : ""));
				} catch (TownyException e) {
				}
				for (TownBlock townBlock : resident.getTownBlocks())
					try {
						System.out.println("|------TownBlock: " + townBlock.getX() + "," + townBlock.getZ() + " "
								+ "Town: " + (townBlock.hasTown() ? townBlock.getTown().getName() : "None") + " : "
								+ "Owner: " + (townBlock.hasResident() ? townBlock.getResident().getName() : "None"));
					} catch (TownyException e) {
					}
			}
			for (Town town : universe.getTowns()) {
				try {
					System.out.println("|---Town: " + town.getName() + " " + (town.hasNation() ? town.getNation().getName() : ""));
				} catch (TownyException e) {
				}
				for (TownBlock townBlock : town.getTownBlocks())
					try {
						System.out.println("|------TownBlock: "  + "," + townBlock.getZ() + " "
								+ "Town: " + (townBlock.hasTown() ? townBlock.getTown().getName() : "None") + " : "
								+ "Owner: " + (townBlock.hasResident() ? townBlock.getResident().getName() : "None"));
					} catch (TownyException e) {
					}
			}
		}
	}

	public void seedTowny() {
		TownyUniverse townyUniverse = plugin.getTownyUniverse();
		Random r = new Random();
		for (int i = 0; i < 1000; i++) {

			try {
				townyUniverse.newNation(Integer.toString(r.nextInt()));
			} catch (TownyException e) {
			}
			try {
				townyUniverse.newTown(Integer.toString(r.nextInt()));
			} catch (TownyException e) {
			}
			try {
				townyUniverse.newResident(Integer.toString(r.nextInt()));
			} catch (TownyException e) {
			}
		}
	}

	public void parseTownyAdminCommand(Player player, String[] split) {
		/*
		 * /townyadmin
		 * /townyadmin ?
		 * /townyadmin set [] .. []
		 * /townyadmin war toggle [on/off]
		 * /townyadmin reload
		 */

		if (split.length == 0)
			showAdminPanel(player);
		else if (split[0].equalsIgnoreCase("?"))
			showTownyAdminHelp(player);
		else if (split[0].equalsIgnoreCase("set")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			adminSet(player, newSplit);
		} else if (split[0].equalsIgnoreCase("war")) {
			String[] newSplit = new String[split.length - 1];
			System.arraycopy(split, 1, newSplit, 0, split.length - 1);
			parseWarCommand(player, newSplit);
		} else if (split[0].equalsIgnoreCase("reload"))
			reloadTowny(player);
	}

	@SuppressWarnings("static-access")
	public void showAdminPanel(Player player) {
		Runtime run = Runtime.getRuntime();
		player.sendMessage(ChatTools.formatTitle("Towny Admin Panel"));
		player.sendMessage(Colors.Blue + "[" + Colors.LightBlue + "Towny" + Colors.Blue + "] "
				+ Colors.Green + " WarTime: " + Colors.LightGreen + plugin.getTownyUniverse().isWarTime());
		try {
			TownyIConomyObject.checkIConomy();
			player.sendMessage(Colors.Blue + "[" + Colors.LightBlue + "iConomy" + Colors.Blue + "] "
					+ Colors.Green + " Economy: " + Colors.LightGreen + getTotalEconomy() + " " + iConomy.currency + Colors.Gray + " | "
					+ Colors.Green + "Bank Accounts: " + Colors.LightGreen + iConomy.db.accounts.returnMap().size());
		} catch (Exception e) {
		}
		player.sendMessage(Colors.Blue + "[" + Colors.LightBlue + "Server" + Colors.Blue + "] "
				+ Colors.Green + " Memory: " + Colors.LightGreen + MemMgmt.getMemSize(run.totalMemory()) + Colors.Gray + " | "
				+ Colors.Green + "Threads: " + Colors.LightGreen + Thread.getAllStackTraces().keySet().size() + Colors.Gray + " | "
				+ Colors.Green + "Time: " + Colors.LightGreen + plugin.getTownyUniverse().getFormatter().getTime());
		player.sendMessage(Colors.Yellow + MemMgmt.getMemoryBar(50, run));

	}

	public int getTotalEconomy() {
		int total = 0;
		try {
			@SuppressWarnings("static-access")
			Map<String, String> map = iConomy.db.accounts.returnMap();
			Set<String> keys = map.keySet();

			for (String key : keys)
				total += iConomy.db.get_balance(key);
		} catch (Exception e) {
		}
		return total;
	}

	/**
	 * Send a list of all towny admin commands to player Command: /townyadmin ?
	 * 
	 * @param player
	 */

	public void showTownyAdminHelp(Player player) {
		player.sendMessage(ChatTools.formatTitle("/townyadmin"));
		player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "", "Admin panel"));
		player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "set [] .. []", "'/townyadmin set' for help"));
		player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "war toggle [on/off]", ""));
		player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "reload", "reload Towny"));
	}

	public void adminSet(Player player, String[] split) {

	}
	
	public void reloadTowny(Player player) {
		plugin.onLoad();
		plugin.sendMsg(player, "Towny's settings was reloaded.");
	}
	
	public void parseWarCommand(Player player, String[] split) {
		if (split.length == 0) {
			
		} else if (split[0].equalsIgnoreCase("toggle")) {
			boolean isWarTime = plugin.getTownyUniverse().isWarTime();
			boolean choice;
			if (split.length == 2)
				try {
					choice = parseOnOff(split[1]);
				} catch (Exception e) {
					plugin.sendErrorMsg(player, "Invalid choice");
					return;
				}
			else
				choice = !isWarTime;
			
			if (isWarTime && choice)
				plugin.sendErrorMsg(player, "War already in process");
			else if (choice) {
				plugin.getTownyUniverse().startWarEvent();
				plugin.sendMsg(player, "Started the war countdown.");
			} else {
				plugin.getTownyUniverse().endWarEvent();
				plugin.sendMsg(player, "Ended the current war.");
			}
		}
	}
}