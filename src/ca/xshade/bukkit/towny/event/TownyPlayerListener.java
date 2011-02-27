package ca.xshade.bukkit.towny.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerItemEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.EmptyNationException;
import ca.xshade.bukkit.towny.EmptyTownException;
import ca.xshade.bukkit.towny.IConomyException;
import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.PlayerCache;
import ca.xshade.bukkit.towny.PlayerCache.TownBlockStatus;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.TownyFormatter;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.command.TownyMapCommand;
import ca.xshade.bukkit.towny.object.Coord;
import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.towny.object.TownBlock;
import ca.xshade.bukkit.towny.object.TownBlockOwner;
import ca.xshade.bukkit.towny.object.TownyIConomyObject;
import ca.xshade.bukkit.towny.object.TownyPermission;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.object.TownyWorld;
import ca.xshade.bukkit.towny.object.WorldCoord;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;
import ca.xshade.bukkit.util.MinecraftTools;
import ca.xshade.util.MemMgmt;
import ca.xshade.util.StringMgmt;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

/**
 * Handle events for all Player related events
 * 
 * @author Shade
 * 
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
				formatedName = (colour.equals(Colors.White) ? "" : colour) + plugin.getTownyUniverse().getFormatter().getNamePrefix(resident)
					+ "%1$s" + plugin.getTownyUniverse().getFormatter().getNamePostfix(resident) + (colour.equals(Colors.White) ? "" : Colors.White);
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
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		plugin.sendDebugMsg("onPlayerDeath: " + player.getName());
		try {
			event.setRespawnLocation(plugin.getTownyUniverse().getTownSpawnLocation(player, true));
		} catch (TownyException e) {
		}
	}
	
	
	@Override
	public void onPlayerItem(PlayerItemEvent event) {
		if (event.isCancelled())
			return;
		
		long start = System.currentTimeMillis();
		
		if (TownySettings.isItemUseId(event.getItem().getTypeId()))
			onPlayerItemEvent(event, true);
		
		plugin.sendDebugMsg("onPlayerItemEvent took " + (System.currentTimeMillis() - start) + "ms");
		
	}
	
	public void onPlayerItemEvent(PlayerItemEvent event, boolean firstCall) {	
		Player player = event.getPlayer();
		
		WorldCoord worldCoord;
		try {
			worldCoord = new WorldCoord(plugin.getTownyUniverse().getWorld(player.getWorld().getName()), Coord.parseCoord(player));
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, "This world has not been configured by Towny.");
			event.setCancelled(true);
			return;
		}

		// Check cached permissions first
		try {
			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);
			if (!cache.getItemUsePermission())
				event.setCancelled(true);
			if (cache.hasBlockErrMsg())
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old destroy permission was null, update it
				TownBlockStatus status = plugin.cacheStatus(player, worldCoord, plugin.getStatusCache(player, worldCoord));
				plugin.cacheItemUse(player, worldCoord, getItemUsePermission(player, status, worldCoord));
				onPlayerItemEvent(event, false);
			} else
				plugin.sendErrorMsg(player, "Error updating item use permissions cache.");
		}
	}
	
	public boolean getItemUsePermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.ITEM_USE);
	}
	
	
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();
		
		try {
			TownyWorld fromWorld = plugin.getTownyUniverse().getWorld(from.getWorld().getName());
			WorldCoord fromCoord = new WorldCoord(fromWorld, Coord.parseCoord(from));
			WorldCoord toCoord = new WorldCoord((from.getWorld() == to.getWorld() ? fromWorld : plugin.getTownyUniverse().getWorld(to.getWorld().getName())), Coord.parseCoord(to));
			if (!fromCoord.equals(toCoord))
				onPlayerMoveChunk(player, fromCoord, toCoord, from, to);
		} catch (NotRegisteredException e) {
			plugin.sendErrorMsg(player, e.getError());
		}
	}

	@Override
	public void onPlayerTeleport(PlayerMoveEvent event) {
		onPlayerMove(event);
	}

	public void onPlayerMoveChunk(Player player, WorldCoord from, WorldCoord to, Location fromLoc, Location toLoc) {
		TownyUniverse universe = plugin.getTownyUniverse();
		
		plugin.getCache(player).updateCoord(to);
		
		
		// TODO: Player mode
		if (plugin.hasPlayerMode(player, "townclaim"))
			parseTownClaimCommand(player, new String[]{});
		if (plugin.hasPlayerMode(player, "townunclaim"))
			parseTownUnclaimCommand(player, new String[]{});
		if (plugin.hasPlayerMode(player, "map"))
			TownyMapCommand.showMap(player);

		// claim: attempt to claim area
		// claim remove: remove area from town

		// Check if player has entered a new town/wilderness
		if (to.getWorld().isUsingTowny() && TownySettings.getShowTownNotifications()) {
			boolean fromWild = false, toWild = false, toForSale = false, toHomeBlock = false;
			TownBlock fromTownBlock, toTownBlock;
			Town fromTown = null, toTown = null;
			Resident fromResident = null, toResident = null;
			try {
				fromTownBlock = from.getTownBlock();
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
				toTownBlock = to.getTownBlock();
				try {
					toTown = toTownBlock.getTown();
				} catch (NotRegisteredException e) {
				}
				try {
					toResident = toTownBlock.getResident();
				} catch (NotRegisteredException e) {
				}
				
				toForSale = toTownBlock.isForSale();
				toHomeBlock = toTownBlock.isHomeBlock();
			} catch (NotRegisteredException e) {
				toWild = true;
			}
			
			boolean sendToMsg = false;
			String toMsg = Colors.Gold + " ~ ";

			if (fromWild ^ toWild || !fromWild && !toWild && fromTown != null && toTown != null && fromTown != toTown) {
				sendToMsg = true;
				if (toWild)
					toMsg += Colors.Green + to.getWorld().getUnclaimedZoneName();
				else
					toMsg += universe.getFormatter().getFormattedName(toTown);
			}
			
			if (fromResident != toResident && !toWild) {
				if (!sendToMsg)
					sendToMsg = true;
				else
					toMsg += Colors.LightGray + "  -  ";
				
				if (toResident != null)
					toMsg += Colors.LightGreen + universe.getFormatter().getFormattedName(toResident);
				else
					toMsg += Colors.LightGreen + TownySettings.getUnclaimedPlotName();
			}
			
			if (toTown != null && (toForSale || toHomeBlock)) {
				if (!sendToMsg)
					sendToMsg = true;
				else
					toMsg += Colors.LightGray + "  -  ";
				if (toHomeBlock)
					toMsg += Colors.LightBlue + "[Home]";
				if (toForSale)
					toMsg += Colors.Yellow + "[For Sale: "+toTown.getPlotPrice()+"]";
			}
			
			if (sendToMsg)
				player.sendMessage(toMsg);
			
			plugin.sendDebugMsg("onPlayerMoveChunk: " + fromWild + " ^ " + toWild + " " + fromTown + " = " + toTown);
		}
		
	}
	
	@Override
	public void onPlayerCommandPreprocess(PlayerChatEvent event) {
		if (event.isCancelled())
			return;

		long start = System.currentTimeMillis();
		
		String[] split = event.getMessage().split(" ");
		Player player = event.getPlayer();

		String[] newSplit = StringMgmt.remFirstArg(split);
		
		if (TownySettings.getResidentCommands().contains(split[0]))
			parseResidentCommand(player, newSplit);
		else if (TownySettings.getTownCommands().contains(split[0]))
			parseTownCommand(player, newSplit);
		else if (TownySettings.getNationCommands().contains(split[0]))
			parseNationCommand(player, newSplit);
		else if (TownySettings.getWorldCommands().contains(split[0]))
			parseWorldCommand(player, newSplit);
		else if (TownySettings.getPlotCommands().contains(split[0]))
			parsePlotCommand(player, newSplit);
		/*else if (TownySettings.getTownyCommands().contains(split[0]))
			parseTownyCommand(player, newSplit);*/
		else if (TownySettings.getTownyAdminCommands().contains(split[0]))
			if (!plugin.isTownyAdmin(player))
				plugin.sendErrorMsg(player, "Only an admin can use this command.");
			else
				parseTownyAdminCommand(player, newSplit);
		else if (TownySettings.getTownChatCommands().contains(split[0]))
			parseTownChatCommand(player, event.getMessage().substring(4));
		else if (TownySettings.getNationChatCommands().contains(split[0]))
			parseNationChatCommand(player, event.getMessage().substring(4));
		else
			return;

		plugin.sendDebugMsg("onCommand took " + (System.currentTimeMillis() - start) + "ms");
		event.setCancelled(true);
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
		player.sendMessage(ChatTools.formatCommand("", "/resident", "friend [add/remove] [resident]", "Online match"));
		player.sendMessage(ChatTools.formatCommand("", "/resident", "friend [add+/remove+] [resident]", "Exact name"));
		player.sendMessage(ChatTools.formatCommand("Admin", "/resident", "delete [resident]", ""));
	}
	
	public void parseResidentCommand(Player player, String[] split) {
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
			String[] newSplit = StringMgmt.remFirstArg(split);
			residentSet(player, newSplit);
		} else if (split[0].equalsIgnoreCase("friend")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			residentFriend(player, newSplit);
		} else if (split[0].equalsIgnoreCase("delete")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			residentDelete(player, newSplit);
		} else
			try {
				Resident resident = plugin.getTownyUniverse().getResident(split[0]);
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(resident));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, split[0] + " is not registered");
			}
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
		for (String line : ChatTools.list(formatedList))
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
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/resident set", "perm ...", "'/town set perm' for help"));
			player.sendMessage(ChatTools.formatCommand("", "/resident set", "mode ...", "'/town set mode' for help"));
		} else {
			Resident resident;
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("perm")) {
				String[] newSplit = StringMgmt.remFirstArg(split);
				setTownBlockOwnerPermissions(player, resident, newSplit);
			} else if (split[0].equalsIgnoreCase("mode")) {
				String[] newSplit = StringMgmt.remFirstArg(split);
				setMode(player, newSplit);
			} else {
				plugin.sendErrorMsg(player, "Invalid town property.");
				return;
			}

			plugin.getTownyUniverse().getDataSource().saveResident(resident);
		}
	}

	private void setMode(Player player, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/resident set mode", "reset", ""));
			player.sendMessage(ChatTools.formatCommand("", "/resident set mode", "[mode] ...[mode]", ""));
			player.sendMessage(ChatTools.formatCommand("", "    Mode:", "map", "Show the map between each townblock"));
			player.sendMessage(ChatTools.formatCommand("", "    Mode:", "townclaim", "Attempt to claim the new area"));
			player.sendMessage(ChatTools.formatCommand("Eg", "/resident set mode", "map townclaim", ""));
		} else if (split[0].equalsIgnoreCase("reset"))
			plugin.removePlayerMode(player);
		else
			plugin.setPlayerMode(player, split);
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
				String[] names = StringMgmt.remFirstArg(split);
				residentFriendAdd(player, resident, plugin.getTownyUniverse().getOnlineResidents(player, names));
			} else if (split[0].equalsIgnoreCase("remove")) {
				String[] names = StringMgmt.remFirstArg(split);
				residentFriendRemove(player, resident, plugin.getTownyUniverse().getOnlineResidents(player, names));
			} else if (split[0].equalsIgnoreCase("add+")) {
				String[] names = StringMgmt.remFirstArg(split);
				residentFriendAdd(player, resident, getResidents(player, names));
			} else if (split[0].equalsIgnoreCase("remove+")) {
				String[] names = StringMgmt.remFirstArg(split);
				residentFriendRemove(player, resident, getResidents(player, names));
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
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatCommand("Resident", "/plot claim", "", "Claim this town block"));
			player.sendMessage(ChatTools.formatCommand("Resident/Mayor", "/plot notforsale", "", "Take down a plot for sale"));
			player.sendMessage(ChatTools.formatCommand("Resident/Mayor", "/plot forsale", "", "Put this area up for auction."));
		} else {
			Resident resident;
			TownyWorld world;
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
				world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			try {
				if (split[0].equalsIgnoreCase("claim")) {
					WorldCoord coord = new WorldCoord(world, Coord.parseCoord(player));
					residentClaim(resident, new WorldCoord(world, Coord.parseCoord(player)));

					plugin.sendMsg(player, "Successfully claimed (" + coord + ").");

					plugin.updateCache(coord);
					plugin.getTownyUniverse().getDataSource().saveResident(resident);
					plugin.getTownyUniverse().getDataSource().saveWorld(world);
				} else if (split[0].equalsIgnoreCase("unclaim")) {
					WorldCoord coord = new WorldCoord(world, Coord.parseCoord(player));
					residentUnclaim(resident, new WorldCoord(world, Coord.parseCoord(player)));

					plugin.sendMsg(player, "Successfully unclaimed (" + coord + ").");

					plugin.updateCache(coord);
					plugin.getTownyUniverse().getDataSource().saveResident(resident);
					plugin.getTownyUniverse().getDataSource().saveWorld(world);
				} else if (split[0].equalsIgnoreCase("notforsale")) {
					WorldCoord worldCoord = new WorldCoord(world, Coord.parseCoord(player));
					setPlotForSale(resident, worldCoord, false);
				} else if (split[0].equalsIgnoreCase("forsale")) {
					WorldCoord worldCoord = new WorldCoord(world, Coord.parseCoord(player));
					setPlotForSale(resident, worldCoord, true);
				}
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
			} catch (IConomyException x) {
				plugin.sendErrorMsg(player, x.getError());
			}
		}
	}
	
	public boolean residentClaim(Resident resident, WorldCoord worldCoord) throws TownyException, IConomyException {
		if (plugin.getTownyUniverse().isWarTime())
			throw new TownyException("You cannot do this when the world is at war.");
		
		if (resident.hasTown())
			try {
				TownBlock townBlock = worldCoord.getTownBlock();
				Town town = townBlock.getTown();
				if (resident.getTown() != town)
					throw new TownyException("Selected area is not part of your town.");

				try {
					Resident owner = townBlock.getResident();
					if (townBlock.isForSale()) {
						if (TownySettings.isUsingIConomy() && !resident.pay(town.getPlotPrice(), owner))
							throw new TownyException("You don't have enough money to purchase this plot.");
						townBlock.setResident(resident);
						townBlock.setForSale(false);
						plugin.getTownyUniverse().getDataSource().saveResident(owner);
						plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getBuyResidentPlotMsg(resident.getName(), owner.getName()));
						return true;
					} else if (town.isMayor(resident) || town.hasAssistant(resident)) {
						if (TownySettings.isUsingIConomy() && !town.pay(town.getPlotPrice(), owner))
							throw new TownyException("The town doesn't have enough money to purchase back this plot.");
						townBlock.setResident(null);
						townBlock.setForSale(false);
						plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getBuyResidentPlotMsg(town.getName(), owner.getName()));
						return true;
					} else
						throw new AlreadyRegisteredException("This area has already been claimed by: " + owner.getName());
				} catch (NotRegisteredException e) {
					if (!townBlock.isForSale())
						throw new TownyException("This plot is not for sale.");
					
					if (TownySettings.isUsingIConomy() && !resident.pay(town.getPlotPrice(), town))
						throw new TownyException("You don't have enough money to purchase this plot.");

					townBlock.setForSale(false);
					townBlock.setResident(resident);
					return true;
				}
			} catch (NotRegisteredException e) {
				throw new TownyException("Selected area is not part of your town.");
			}
		else
			throw new TownyException("You must belong to a town in order to claim plots.");
	}
	
	public boolean residentUnclaim(Resident resident, WorldCoord worldCoord) throws TownyException {
		if (plugin.getTownyUniverse().isWarTime())
			throw new TownyException("You cannot do this when the world is at war.");
		
		try {
			TownBlock townBlock = worldCoord.getTownBlock();
			Resident owner = townBlock.getResident();
			if (resident == owner) {
				townBlock.setResident(null);
				townBlock.setForSale(true);
				plugin.getTownyUniverse().getDataSource().saveResident(resident);
				return true;
			} else
				throw new TownyException("You do not own the selected area.");
		} catch (NotRegisteredException e) {
			throw new TownyException("This place is not owned by anyone.");
		}
	}
	
	public void setPlotForSale(Resident resident, WorldCoord worldCoord, boolean forSale) throws TownyException {
		if (resident.hasTown())
			try {
				TownBlock townBlock = worldCoord.getTownBlock();
				Town town = townBlock.getTown();
				if (resident.getTown() != town)
					throw new TownyException("Selected area is not part of your town.");

				if (town.isMayor(resident) || town.hasAssistant(resident))
					townBlock.setForSale(forSale);
				else
					try {
						Resident owner = townBlock.getResident();
						if (resident != owner)
							throw new AlreadyRegisteredException("This area does not belong to you.");
						townBlock.setForSale(forSale);
					} catch (NotRegisteredException e) {
						throw new TownyException("This area does not belong to you.");
					}
				if (forSale)
					plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getPlotForSaleMsg(resident.getName(), worldCoord));
			} catch (NotRegisteredException e) {
				throw new TownyException("Selected area is not part of your town.");
			}
		else
			throw new TownyException("You must belong to a town.");
	}
	
	public void residentDelete(Player player, String[] split) {
		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				plugin.getTownyUniverse().removeResident(resident);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelResidentMsg(resident));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
		else
			try {
				if (!plugin.isTownyAdmin(player))
					throw new TownyException("Only an admin can delete other resident data.");
				Resident resident = plugin.getTownyUniverse().getResident(split[0]);
				plugin.getTownyUniverse().removeResident(resident);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelResidentMsg(resident));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
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
		player.sendMessage(ChatTools.formatCommand("", "/town", "leave", ""));
		player.sendMessage(ChatTools.formatCommand("", "/town", "spawn", "Teleport to town's spawn."));
		player.sendMessage(ChatTools.formatCommand(newTownReq, "/town", "new [town] *[mayor]", "Create a new town."));
		player.sendMessage(ChatTools.formatCommand("Resident", "/town", "deposit [$]", ""));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "mayor ?", "List commands for mayors."));
		player.sendMessage(ChatTools.formatCommand("Admin", "/town", "delete [town]", ""));
	}
	
	public void showTownMayorHelp(Player player) {
		player.sendMessage(ChatTools.formatTitle("Town Mayor Help"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "withdraw [$]", ""));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "claim", "'/town claim ?' for help"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "unclaim", "'/town unclaim ?' for help"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "[add/kick] [resident] .. []", "Online match"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "[add+/kick+] [resident]", "Exact name"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "set [] .. []", "'/town set' for help"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "assistant [add/remove] [player]", "Online match"));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "assistant [add+/remove+] [player]", "Exact"));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall [type] [height]", ""));
		// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "wall remove", ""));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/town", "delete", ""));
	}

	public void parseTownCommand(Player player, String[] split) {
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
		} else if (split[0].equalsIgnoreCase("leave"))
			townLeave(player);
		else if (split[0].equalsIgnoreCase("spawn"))
			try {
				if (split.length == 1) {
					boolean isTownyAdmin = plugin.isTownyAdmin(player);
					if (!TownySettings.isAllowingTownSpawn() && !isTownyAdmin && !plugin.hasPermission(player, "towny.spawntp"))
						throw new TownyException("Town spawn travel is forbidden.");
					Resident resident = plugin.getTownyUniverse().getResident(player.getName());
					if (!isTownyAdmin && TownySettings.isUsingIConomy() && !resident.pay(TownySettings.getTownSpawnTravelPrice()))
						throw new TownyException("Cannot afford to teleport to your town's spawn.");
					if (plugin.checkEssentialsTeleport(player))
						plugin.getTownyUniverse().townSpawn(player, false);
				} else {
					boolean isTownyAdmin = plugin.isTownyAdmin(player);
					if (!TownySettings.isAllowingTownSpawnTravel() && !isTownyAdmin && !plugin.hasPermission(player, "towny.publicspawntp"))
						throw new TownyException("Town spawn travel is forbidden.");
					Resident resident = plugin.getTownyUniverse().getResident(player.getName());
					Town town = plugin.getTownyUniverse().getTown(split[1]);
					if (!isTownyAdmin && TownySettings.isUsingIConomy() && !resident.pay(TownySettings.getTownSpawnTravelPrice()))
						throw new TownyException("Cannot afford to teleport to "+town.getName()+".");
					if (!isTownyAdmin && !town.isPublic())
						throw new TownyException("That town is not public.");
					if (plugin.checkEssentialsTeleport(player))
						player.teleportTo(town.getSpawn());
				}
			} catch (TownyException e) {
				plugin.sendErrorMsg(player, e.getMessage());
			} catch (IConomyException e) {
				plugin.sendErrorMsg(player, e.getMessage());
			}
		else if (split[0].equalsIgnoreCase("withdraw")) {
			if (split.length == 2)
				try {
					townWithdraw(player, Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {
					plugin.sendErrorMsg(player, "Amount must be an integer.");
				}
			else
				plugin.sendErrorMsg(player, "Must specify amount. Eg: /town withdraw 54");
		} else if (split[0].equalsIgnoreCase("deposit")) {
			if (split.length == 2)
				try {
					townDeposit(player, Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {
					plugin.sendErrorMsg(player, "Amount must be an integer.");
				}
			else
				plugin.sendErrorMsg(player, "Must specify amount. Eg: /town withdraw 54");
		} else {
			String[] newSplit = StringMgmt.remFirstArg(split);
			
			if (split[0].equalsIgnoreCase("set"))
				townSet(player, newSplit);
			else if (split[0].equalsIgnoreCase("mayor"))
				townMayor(player, newSplit);
			else if (split[0].equalsIgnoreCase("assistant"))
				townAssistant(player, newSplit);
			else if (split[0].equalsIgnoreCase("delete"))
				townDelete(player, newSplit);
			else if (split[0].equalsIgnoreCase("add"))
				townAdd(player, newSplit, true);
			else if (split[0].equalsIgnoreCase("kick"))
				townKick(player, newSplit, true);
			else if (split[0].equalsIgnoreCase("add+"))
				townAdd(player, newSplit, false);
			else if (split[0].equalsIgnoreCase("kick+"))
				townKick(player, newSplit, false);
			else if (split[0].equalsIgnoreCase("claim"))
				parseTownClaimCommand(player, newSplit);
			else if (split[0].equalsIgnoreCase("unclaim"))
				parseTownUnclaimCommand(player, newSplit);
			else
				try {
					Town town = plugin.getTownyUniverse().getTown(split[0]);
					plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(town));
				} catch (NotRegisteredException x) {
					plugin.sendErrorMsg(player, split[0]+ " is not registered.");
				}
		}
	}

	private void townWithdraw(Player player, int amount) {
		Resident resident;
		Town town;
		try {
			if (amount < 0)
				throw new TownyException("Negative money is stupid. Weirdo."); //TODO
			
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			
			town.withdrawFromBank(resident, amount);
			plugin.getTownyUniverse().sendTownMessage(town, resident.getName() + " withdrew " + amount + " from the town bank.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	private void townDeposit(Player player, int amount) {
		Resident resident;
		Town town;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			town = resident.getTown();
			
			if (amount < 0)
				throw new TownyException("Negative money is stupid. Weirdo."); //TODO
			
			if (!resident.pay(amount, town))
				throw new TownyException("You don't have that much.");
			
			plugin.getTownyUniverse().sendTownMessage(town, resident.getName() + " deposited " + amount + " into the town bank.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
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
		for (String line : ChatTools.list(formatedList))
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
			
			if (TownySettings.isTownCreationAdminOnly() && !plugin.isTownyAdmin(player) && !plugin.hasPermission(player, "towny.town.new"))
				throw new TownyException(TownySettings.getNotPermToNewTownLine());
			
			if (TownySettings.hasTownLimit() && universe.getTowns().size() >= TownySettings.getTownLimit())
				throw new TownyException("The universe cannot hold any more towns.");
			
			Resident resident = universe.getResident(mayorName);
			if (resident.hasTown())
				throw new TownyException(resident.getName() + " already belongs to a town.");

			TownyWorld world = universe.getWorld(player.getWorld().getName());
			Coord key = Coord.parseCoord(player);
			if (world.hasTownBlock(key))
				throw new TownyException("This area (" + key + ") already belongs to someone.");

			if (TownySettings.isUsingIConomy() && !resident.pay(TownySettings.getNewTownPrice()))
				throw new TownyException("You can't afford to settle a new town here.");

			newTown(universe, world, name, resident, key, player.getLocation());			
			universe.sendGlobalMessage(TownySettings.getNewTownMsg(player.getName(), name));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			// TODO: delete town data that might have been done
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	public Town newTown(TownyUniverse universe, TownyWorld world, String name, Resident resident, Coord key, Location spawn) throws TownyException {
		world.newTownBlock(key);
		universe.newTown(name);
		Town town = universe.getTown(name);
		town.addResident(resident);
		town.setMayor(resident);
		TownBlock townBlock = world.getTownBlock(key);
		townBlock.setTown(town);
		town.setHomeBlock(townBlock);
		town.setSpawn(spawn);
		world.addTown(town);

		universe.getDataSource().saveResident(resident);
		universe.getDataSource().saveTown(town);
		universe.getDataSource().saveWorld(world);
		universe.getDataSource().saveTownList();
		
		plugin.updateCache();
		return town;
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
		
		if (resident.isMayor()) {
			plugin.sendErrorMsg(player, TownySettings.getMayorAbondonMsg());
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
	
	public void townMayor(Player player, String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			showTownMayorHelp(player);
	}
	
	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void townAdd(Player player, String[] names, boolean matchOnline) {
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

		townAddResidents(player, town, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
		
		plugin.updateCache();
	}
	
	public List<Resident> getResidents(Player player, String[] names) {
		List<Resident> invited = new ArrayList<Resident>();
		for (String name : names)
			try {
				Resident target = plugin.getTownyUniverse().getResident(name);
				invited.add(target);
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
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

	public void townKick(Player player, String[] names, boolean matchOnline) {
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

		townKickResidents(player, resident, town, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
		
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
			String[] newSplit = StringMgmt.remFirstArg(split);
			townAssistantsAdd(player, newSplit, true);
		} else if (split[0].equalsIgnoreCase("remove")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			townAssistantsRemove(player, newSplit, true);
		} else if (split[0].equalsIgnoreCase("add+")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			townAssistantsAdd(player, newSplit, false);
		} else if (split[0].equalsIgnoreCase("remove+")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			townAssistantsRemove(player, newSplit, false);
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

	public void townAssistantsAdd(Player player, String[] names, boolean matchOnline) {
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

		townAssistantsAdd(player, town, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
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

	public void townAssistantsRemove(Player player, String[] names, boolean matchOnline) {
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

		townAssistantsRemove(player, resident, town, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
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
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim", "", "Claim this town block"));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim", "outpost", "Claim area not attrached to town"));
			// TODO: player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim", "auto", "Automatically expand town area till max"));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim", "rect [radius]", "Claim around you"));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town claim", "rect auto", "Detemine the maximum radius"));
		} else {
			Resident resident;
			Town town;
			TownyWorld world;
			try {
				if (plugin.getTownyUniverse().isWarTime())
					throw new TownyException("You cannot do this when the world is at war.");
				
				if (!plugin.isTownyAdmin(player) && !plugin.hasPermission(player, "towny.town.claim"))
					throw new TownyException("You do not have permission to expand your town.");
				
				resident = plugin.getTownyUniverse().getResident(player.getName());
				town = resident.getTown();
				if (!resident.isMayor() && !town.hasAssistant(resident))
					throw new TownyException("You are not the mayor or an assistant.");
				world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
				
				

				double blockCost = 0;
				List<WorldCoord> selection;
				boolean attachedToEdge = true;
				
				if (split.length == 1 && split[0].equalsIgnoreCase("outpost")) {
					if (TownySettings.isAllowingOutposts()) {
						selection = new ArrayList<WorldCoord>();
						selection.add(new WorldCoord(world, Coord.parseCoord(player)));
						blockCost = TownySettings.getOutpostCost();
						attachedToEdge = false;
					} else
						throw new TownyException("Outposts are not available.");
				} else {
					selection = selectWorldCoordArea(town, new WorldCoord(world, Coord.parseCoord(player)), split);
					blockCost = TownySettings.getClaimPrice();
				}
				
				plugin.sendDebugMsg("townClaim: Pre-Filter Selection " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				selection = removeTownOwnedBlocks(selection);
				plugin.sendDebugMsg("townClaim: Post-Filter Selection " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				checkIfSelectionIsValid(town, selection, attachedToEdge, blockCost, false);
				
				try {
					double cost = blockCost * selection.size();
					if (TownySettings.isUsingIConomy() && !town.pay(cost))
						throw new TownyException("Town cannot afford to claim " + selection.size() + " town blocks costing " + cost + TownyIConomyObject.getIConomyCurrency() + ". Add more money into the town bank.");
				} catch (IConomyException e1) {
					throw new TownyException("Iconomy Error");
				}
				
				for (WorldCoord worldCoord : selection)
					townClaim(town, worldCoord);
				
				plugin.getTownyUniverse().getDataSource().saveTown(town);
				plugin.getTownyUniverse().getDataSource().saveWorld(world);
				
				plugin.sendMsg(player, "Annexed area " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				plugin.updateCache();
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
		}
	}
	
	public void parseTownUnclaimCommand(Player player, String[] split) {
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/town unclaim"));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town unclaim", "", "Unclaim this town block"));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town unclaim", "rect [radius]", "Attempt to unclaim around you."));
			player.sendMessage(ChatTools.formatCommand("Mayor", "/town unclaim", "all", "Attempt to unclaim all townblocks."));
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
				
				List<WorldCoord> selection;
				if (split.length == 1 && split[0].equalsIgnoreCase("all"))
					townUnclaimAll(town);
				else {
					selection = selectWorldCoordArea(town, new WorldCoord(world, Coord.parseCoord(player)), split);
					selection = filterOwnedBlocks(town, selection);
					
					for (WorldCoord worldCoord : selection)
						townUnclaim(town, worldCoord, false);
	
					plugin.sendMsg(player, "Abandoned area " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				}
				plugin.getTownyUniverse().getDataSource().saveTown(town);
				plugin.getTownyUniverse().getDataSource().saveWorld(world);
				plugin.updateCache();
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
		}
	}
	
	public List<WorldCoord> selectWorldCoordArea(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		
		if (args.length == 0)
			if (pos.getWorld().isClaimable())
				out.add(pos);
			else
				throw new TownyException("This world is not claimable.");
		else if (args[0].equalsIgnoreCase("rect")) {
			if (args.length < 2) {
				//show help
			} else {
				int r;
				if (args[1].equalsIgnoreCase("auto")) {
					if (owner instanceof Town) {
						Town town = (Town)owner;
						int available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
						r = 0;
						while (available - Math.pow((r + 1) * 2 - 1, 2) >= 0)
							r += 1;
					} else
						throw new TownyException("Only towns can use rect auto.");
				} else
					try {
						r = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						throw new TownyException("Invalid radius. Use an integer or 'auto'.");
					}	
				
				r -= 1;
				
				for (int z = pos.getZ() - r; z <= pos.getZ() + r; z++)
					for (int x = pos.getX() - r; x <= pos.getX() + r; x++)
						if (pos.getWorld().isClaimable())
							out.add(new WorldCoord(pos.getWorld(), x, z));
			}
		} else if (args[0].equalsIgnoreCase("auto"))
			//TODO
			throw new TownyException("Not yet supported.");
		
		return out;
	}
	
	public void checkIfSelectionIsValid(TownBlockOwner owner, List<WorldCoord> selection, boolean attachedToEdge, double blockCost, boolean force) throws TownyException {
		if (force)
			return;
		
		if (attachedToEdge && !isEdgeBlock(owner, selection))
			throw new TownyException("Selected area not attached to edge.");
		
		if (owner instanceof Town) {
			Town town = (Town)owner;
			int available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
			plugin.sendDebugMsg("Claim Check Available: " + available);
			if (available - selection.size() < 0)
				throw new TownyException("Not enough available town blocks to claim this selection.");
		}
		
		try {
			double cost = blockCost * selection.size();
			if (TownySettings.isUsingIConomy() && !owner.canPay(cost))
				throw new TownyException("Town cannot afford to claim "+selection.size() + " town blocks costing " + cost + TownyIConomyObject.getIConomyCurrency());
		} catch (IConomyException e1) {
			throw new TownyException("Iconomy Error");
		}
	}
	
	public List<WorldCoord> removeTownOwnedBlocks(List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (!worldCoord.getTownBlock().hasTown())
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
				out.add(worldCoord);
			}
		return out;
	}
	
	public List<WorldCoord> removeResidentOwnedBlocks(List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (!worldCoord.getTownBlock().hasResident())
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
				out.add(worldCoord);
			}
		return out;
	}
	
	public List<WorldCoord> filterOwnedBlocks(TownBlockOwner owner, List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().isOwner(owner))
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
			}
		return out;
	}
	
	public boolean townClaim(Town town, WorldCoord worldCoord) throws TownyException {		
		try {
			TownBlock townBlock = worldCoord.getTownBlock();
			try {
				throw new AlreadyRegisteredException("This area has already been claimed by: " + townBlock.getTown().getName());
			} catch (NotRegisteredException e) {
				throw new AlreadyRegisteredException("This area has already been claimed.");
			}
		} catch (NotRegisteredException e) {
			TownBlock townBlock = worldCoord.getWorld().newTownBlock(worldCoord);
			townBlock.setTown(town);
			if (!town.hasHomeBlock())
				town.setHomeBlock(townBlock);
			return true;
		}
	}
	
	public boolean townUnclaim(Town town, WorldCoord worldCoord, boolean force) throws TownyException {
		try {
			TownBlock townBlock = worldCoord.getTownBlock();
			if (town != townBlock.getTown() && !force)
				throw new TownyException("This area does not belong to you.");
			
			plugin.getTownyUniverse().removeTownBlock(townBlock);
			
			return true;
		} catch (NotRegisteredException e) {
			throw new TownyException("This area has not been claimed.");
		}
	}
	
	public boolean isEdgeBlock(TownBlockOwner owner, List<WorldCoord> worldCoords) {
		// TODO: Better algorithm that doesn't duplicates checks.

		for (WorldCoord worldCoord : worldCoords)
			if (isEdgeBlock(owner, worldCoord))
				return true;
		return false;
	}

	public boolean isEdgeBlock(TownBlockOwner owner, WorldCoord worldCoord) {
		if (TownySettings.getDebug())
			System.out.print("[Towny] Debug: isEdgeBlock(" + worldCoord.toString() + ") = ");
		
		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				if (edgeTownBlock.isOwner(owner)) {
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
	
	public boolean townUnclaimAll(Town town) {
		plugin.getTownyUniverse().removeTownBlocks(town);
		plugin.getTownyUniverse().sendTownMessage(town, "Your town abadoned the area");
		
		return true;
	}

	/**
	 * 
	 * Command: /town set [] ... []
	 * 
	 * @param player
	 * @param split
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
			player.sendMessage(ChatTools.formatCommand("", "/town set", "plottax [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "plotprice [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "name [name]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/town set", "public [on/off]", ""));
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
						plugin.getTownyUniverse().sendTownMessage(town, player.getName() + " has set the daily resident tax at " + split[1]);
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, "Taxes must be an interger.");
					}
			} else if (split[0].equalsIgnoreCase("plottax")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /town set plottax 10");
				else
					try {
						town.setPlotTax(Integer.parseInt(split[1]));
						plugin.getTownyUniverse().sendTownMessage(town, player.getName() + " has set the daily tax of plots at " + split[1]);
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, "The tax must be an interger.");
					}
			} else if (split[0].equalsIgnoreCase("plotprice")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /town set plotprice 50");
				else
					try {
						town.setPlotPrice(Integer.parseInt(split[1]));
						plugin.getTownyUniverse().sendTownMessage(town, player.getName() + " has set the price of plots at " + split[1]);
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, "The price must be an interger.");
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
				String[] newSplit = StringMgmt.remFirstArg(split);
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
			} else if (split[0].equalsIgnoreCase("public")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /town set public [on/off]");
				else
					try {
						town.setPublic(parseOnOff(split[1]));
						plugin.sendMsg(player, "Successfully made town "+(town.isPublic() ? "" : "not ")+"public.");
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
			player.sendMessage(ChatTools.formatCommand("Level", "[resident/ally/outsider]", "", ""));
			player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("Eg", "/town set perm", "ally off", ""));
			player.sendMessage(ChatTools.formatCommand("Eg", "/resident set perm", "friend build on", ""));
			player.sendMessage("Use 'friend' instead of 'resident' for plot permissions.");
			player.sendMessage("Resident plots don't make use of outsider permissions.");
		} else {
			TownyPermission perm = townBlockOwner.getPermissions();
			String arg0 = split[0];
			if (arg0.equalsIgnoreCase("friend"))
				arg0 = "resident";
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
						perm.residentSwitch = b;
						perm.residentItemUse = b;
					} else if (split[0].equalsIgnoreCase("outsider")) {
						perm.outsiderBuild = b;
						perm.outsiderDestroy = b;
						perm.outsiderSwitch = b;
						perm.outsiderItemUse = b;
					} else if (split[0].equalsIgnoreCase("ally")) {
						perm.allyBuild = b;
						perm.allyDestroy = b;
						perm.allySwitch = b;
						perm.allyItemUse = b;
					} else if (split[0].equalsIgnoreCase("build")) {
						perm.residentBuild = b;
						perm.outsiderBuild = b;
						perm.allyBuild = b;
					} else if (split[0].equalsIgnoreCase("destroy")) {
						perm.residentDestroy = b;
						perm.outsiderDestroy = b;
						perm.allyDestroy = b;
					} else if (split[0].equalsIgnoreCase("switch")) {
						perm.residentSwitch = b;
						perm.outsiderSwitch = b;
						perm.allySwitch = b;
					} else if (split[0].equalsIgnoreCase("itemuse")) {
						perm.residentItemUse = b;
						perm.outsiderItemUse = b;
						perm.allyItemUse = b;
					}
				} catch (Exception e) {
				}
			else if (split.length == 3)
				try {
					boolean b = parseOnOff(split[2]);
					String s = "";
					if ((arg0.equalsIgnoreCase("resident") || arg0.equalsIgnoreCase("outsider") || arg0.equalsIgnoreCase("ally"))
							&& (split[1].equalsIgnoreCase("build") || split[1].equalsIgnoreCase("destroy") || split[1].equalsIgnoreCase("switch") || split[1].equalsIgnoreCase("itemuse")))
						s = arg0 + split[1];
					perm.set(s, b);
				} catch (Exception e) {
				}
			String perms = townBlockOwner.getPermissions().toString();
			if (perms.length() == 0)
				perms = "deny all";
			else
				perms = "only allow ";
			plugin.sendMsg(player, "Successfully changed permissions to " + perms + ".");
			
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
	
	public void townDelete(Player player, String[] split) {
		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				Town town = resident.getTown();
				if (!resident.isMayor())
					throw new TownyException("You are not the mayor.");
				plugin.getTownyUniverse().removeTown(town);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelTownMsg(town));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
		else
			try {
				if (!plugin.isTownyAdmin(player))
					throw new TownyException("Only an admin can delete other town data.");
				Town town = plugin.getTownyUniverse().getTown(split[0]);
				plugin.getTownyUniverse().removeTown(town);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelTownMsg(town));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
	}

	/**
	 * Send a list of all nation commands to player Command: /nation ?
	 * 
	 * @param player
	 */

	public void showNationHelp(Player player) {
		player.sendMessage(ChatTools.formatTitle("/nation"));
		player.sendMessage(ChatTools.formatCommand("", "/nation", "", "Your nation's status"));
		player.sendMessage(ChatTools.formatCommand("", "/nation", "[nation]", "Target nation's status"));
		player.sendMessage(ChatTools.formatCommand("", "/nation", "list", "List all nations"));
		player.sendMessage(ChatTools.formatCommand("Resident", "/nation", "deposit [$]", ""));
		player.sendMessage(ChatTools.formatCommand("Mayor", "/nation", "leave", "Leave your nation"));
		if (!TownySettings.isNationCreationAdminOnly())
			player.sendMessage(ChatTools.formatCommand("Mayor", "/nation", "new [nation]", "Create a new nation"));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "king ?", "List the king commands"));
		player.sendMessage(ChatTools.formatCommand("Admin", "/nation", "new [nation] *[capital]", "Create a new nation"));
		player.sendMessage(ChatTools.formatCommand("Admin", "/nation", "delete [nation]", ""));
	}
	
	public void showNationKingHelp(Player player) {
		player.sendMessage(ChatTools.formatTitle("Nation King Help"));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "withdraw [$]", ""));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "[add/kick] [town] .. [town]", ""));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "assistant [add/remove] [resident]", "Match"));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "assistant [add+/remove+] [resident]", "Exact"));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "set [] .. []", ""));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "ally [add/remove] [nation]", "Set you alliance."));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "enemy [add/remove] [nation]", "Set you enemys."));
		player.sendMessage(ChatTools.formatCommand("King", "/nation", "delete", ""));
	}

	public void parseNationCommand(Player player, String[] split) {
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
		else if (split[0].equalsIgnoreCase("withdraw")) {
			if (split.length == 2)
				try {
					nationWithdraw(player, Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {
					plugin.sendErrorMsg(player, "Amount must be an integer.");
				}
			else
				plugin.sendErrorMsg(player, "Must specify amount. Eg: /nation withdraw 54");
		} else if (split[0].equalsIgnoreCase("deposit")) {
			if (split.length == 2)
				try {
					nationDeposit(player, Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {
					plugin.sendErrorMsg(player, "Amount must be an integer.");
				}
			else
				plugin.sendErrorMsg(player, "Must specify amount. Eg: /nation deposit 54");
		} else {
			String[] newSplit = StringMgmt.remFirstArg(split);
			
			if (split[0].equalsIgnoreCase("king"))
				nationKing(player, newSplit);
			else if (split[0].equalsIgnoreCase("add"))
				nationAdd(player, newSplit);
			else if (split[0].equalsIgnoreCase("kick"))
				nationKick(player, newSplit);
			else if (split[0].equalsIgnoreCase("assistant"))
				nationAssistant(player, newSplit);
			else if (split[0].equalsIgnoreCase("set"))
				nationSet(player, newSplit);
			else if (split[0].equalsIgnoreCase("ally"))
				nationAlly(player, newSplit);
			else if (split[0].equalsIgnoreCase("enemy"))
				nationEnemy(player, newSplit);
			else if (split[0].equalsIgnoreCase("delete"))
				nationDelete(player, newSplit);
			else 
				try {
					Nation nation = plugin.getTownyUniverse().getNation(split[0]);
					plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(nation));
				} catch (NotRegisteredException x) {
					plugin.sendErrorMsg(player, split[0] + " is not registered.");
				}
		}
	}
		
	private void nationWithdraw(Player player, int amount) {
		Resident resident;
		Nation nation;
		try {
			if (amount < 0)
				throw new TownyException("Negative money is stupid. Weirdo."); //TODO
			
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			
			nation.withdrawFromBank(resident, amount);
			plugin.getTownyUniverse().sendNationMessage(nation, resident.getName() + " withdrew " + amount + " from the nation bank.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	private void nationDeposit(Player player, int amount) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			
			if (amount < 0)
				throw new TownyException("Negative money is stupid. Weirdo."); //TODO
			
			if (!resident.pay(amount, nation))
				throw new TownyException("You don't have that much.");
			
			plugin.getTownyUniverse().sendNationMessage(nation, resident.getName() + " deposited " + amount + " to the nation bank.");
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	/**
	 * Send a list of all nations in the universe to player Command: /nation list
	 * 
	 * @param player
	 */

	public void listNations(Player player) {
		player.sendMessage(ChatTools.formatTitle("Nations"));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Nation nation : plugin.getTownyUniverse().getNations())
			formatedList.add(Colors.LightBlue + nation.getName() + Colors.Blue + " [" + nation.getNumTowns() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList))
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
			if (TownySettings.isNationCreationAdminOnly() && !plugin.isTownyAdmin(player) && !plugin.hasPermission(player, "towny.nation.new"))
				throw new TownyException(TownySettings.getNotPermToNewNationLine());
			
			Town town = universe.getTown(capitalName);
			if (town.hasNation())
				throw new TownyException("Target town already belongs to a nation.");
			
			if (TownySettings.isUsingIConomy() && !town.pay(TownySettings.getNewNationPrice()))
				throw new TownyException("The town can't afford to start a new nation.");

			newNation(universe, name, town);
			/*universe.newNation(name);
			Nation nation = universe.getNation(name);
			nation.addTown(town);
			nation.setCapital(town);

			universe.getDataSource().saveTown(town);
			universe.getDataSource().saveNation(nation);
			universe.getDataSource().saveNationList();*/

			universe.sendGlobalMessage(TownySettings.getNewNationMsg(player.getName(), name));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			// TODO: delete town data that might have been done
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	public Nation newNation(TownyUniverse universe, String name, Town town) throws AlreadyRegisteredException, NotRegisteredException {
		universe.newNation(name);
		Nation nation = universe.getNation(name);
		nation.addTown(town);
		nation.setCapital(town);

		universe.getDataSource().saveTown(town);
		universe.getDataSource().saveNation(nation);
		universe.getDataSource().saveNationList();
		
		return nation;
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
	
	public void nationDelete(Player player, String[] split) {
		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				Town town = resident.getTown();
				Nation nation = town.getNation();
				if (!resident.isKing())
					throw new TownyException("You are not the king.");
				plugin.getTownyUniverse().removeNation(nation);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelNationMsg(nation));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
		else
			try {
				if (!plugin.isTownyAdmin(player))
					throw new TownyException("Only an admin can delete other nation data.");
				Nation nation = plugin.getTownyUniverse().getNation(split[0]);
				plugin.getTownyUniverse().removeNation(nation);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelNationMsg(nation));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
	}
	
	public void nationKing(Player player, String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			showNationKingHelp(player);
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
			String[] newSplit = StringMgmt.remFirstArg(split);
			nationAssistantsAdd(player, newSplit, true);
		} else if (split[0].equalsIgnoreCase("remove")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			nationAssistantsRemove(player, newSplit, true);
		} else if (split[0].equalsIgnoreCase("add+")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			nationAssistantsAdd(player, newSplit, false);
		} else if (split[0].equalsIgnoreCase("remove+")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			nationAssistantsRemove(player, newSplit, false);
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

	public void nationAssistantsAdd(Player player, String[] names, boolean matchOnline) {
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

		nationAssistantsAdd(player, nation, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
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

	public void nationAssistantsRemove(Player player, String[] names, boolean matchOnline) {
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

		nationAssistantsRemove(player, resident, nation, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
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
	
	public void nationAlly(Player player, String[] names) {
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
		
		nationAlly(player, nation, plugin.getTownyUniverse().getNations(names));
	}
	
	public void nationAlly(Player player, Nation nation, List<Nation> newAllies) {
		ArrayList<Nation> remove = new ArrayList<Nation>();
		for (Nation newAlly : newAllies)
			try {
				nation.addAlly(newAlly);
				//plugin.getTownyUniverse().getDataSource().saveNation(newAlly);
			} catch (AlreadyRegisteredException e) {
				remove.add(newAlly);
			}
		for (Nation newAlly : remove)
			newAllies.remove(newAlly);

		if (newAllies.size() > 0) {
			String msg = player.getName() + " allied with the nations ";
			for (Nation newAlly : newAllies)
				msg += newAlly.getName() + ", ";
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, "None of those names were valid.");
	}
	
	public void nationEnemy(Player player, String[] names) {
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
		
		nationEnemy(player, nation, plugin.getTownyUniverse().getNations(names));
	}
	
	public void nationEnemy(Player player, Nation nation, List<Nation> newEnemies) {
		ArrayList<Nation> remove = new ArrayList<Nation>();
		for (Nation newEnemy : newEnemies)
			try {
				nation.addEnemy(newEnemy);
				//plugin.getTownyUniverse().getDataSource().saveNation(newAlly);
			} catch (AlreadyRegisteredException e) {
				remove.add(newEnemy);
			}
		for (Nation newEnemy : remove)
			newEnemies.remove(newEnemy);

		if (newEnemies.size() > 0) {
			String msg = player.getName() + " developed a hatred for the nations ";
			for (Nation newEnemy : newEnemies)
				msg += newEnemy.getName() + ", ";
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, "None of those names were valid.");
	}
	
	public void nationSet(Player player, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/nation set"));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "king [resident]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "capital [town]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "taxes [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "name [name]", ""));
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
			} else if (split[0].equalsIgnoreCase("capital")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /nation set capital 70");
				else
					try {
						Town newCapital = plugin.getTownyUniverse().getTown(split[1]);
						nation.setCapital(newCapital);
						plugin.getTownyUniverse().sendNationMessage(nation, TownySettings.getNewKingMsg(newCapital.getMayor().getName()));
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, "Taxes must be an interger.");
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
						boolean choice = parseOnOff(split[1]);
						
						if (choice && TownySettings.isUsingIConomy() && !nation.pay(TownySettings.getNationNeutralityCost()))
							throw new TownyException("Nation couldn't afford to become a neutral nation.");
							
						nation.setNeutral(choice);
						plugin.sendMsg(player, "Successfully changed nation's neutrality.");
						plugin.getTownyUniverse().sendNationMessage(nation, "You nation is now" + (nation.isNeutral() ? Colors.Green : Colors.Red + " not") + " neutral.");
					} catch (IConomyException e) {
						plugin.sendErrorMsg(player, e.getError());
					} catch (TownyException e) {
						nation.setNeutral(false);
						plugin.sendErrorMsg(player, e.getError());
					} catch (Exception e) {
						plugin.sendErrorMsg(player, "Input error. Please use either on or off.");
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

	public void showWorldHelp(Player player) {
		player.sendMessage(ChatTools.formatTitle("/townyworld"));
		player.sendMessage(ChatTools.formatCommand("", "/townyworld", "", "Current world's status"));
		player.sendMessage(ChatTools.formatCommand("", "/townyworld", "[world]", "Target worlds's status"));
		player.sendMessage(ChatTools.formatCommand("", "/townyworld", "list", "List all worlds"));
		player.sendMessage(ChatTools.formatCommand("Admin", "/townyworld", "set [] .. []", ""));
	}
	
	public void parseWorldCommand(Player player, String[] split) {
		if (split.length == 0)
			try {
				TownyWorld world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(world));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, "This area isn't recognized by Towny.");
			}
		else if (split[0].equalsIgnoreCase("?"))
			showWorldHelp(player);
		else if (split[0].equalsIgnoreCase("list"))
			listWorlds(player);
		else if (split[0].equalsIgnoreCase("set"))
			worldSet(player, StringMgmt.remFirstArg(split));
		else
			try {
				TownyWorld world = plugin.getTownyUniverse().getWorld(split[0]);
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(world));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, split[0] + " is not registered.");
			}
	}
	
	public void listWorlds(Player player) {
		player.sendMessage(ChatTools.formatTitle("Worlds"));
		ArrayList<String> formatedList = new ArrayList<String>();
		HashMap<String,Integer> playersPerWorld = MinecraftTools.getPlayersPerWorld(plugin.getServer());
		for (TownyWorld world : plugin.getTownyUniverse().getWorlds()) {
			int numPlayers = playersPerWorld.containsKey(world.getName()) ? playersPerWorld.get(world.getName()) : 0;
			formatedList.add(Colors.LightBlue + world.getName() + Colors.Blue + " [" + numPlayers + "]" + Colors.White);
		}
		for (String line : ChatTools.list(formatedList))
			player.sendMessage(line);
	}
	
	public void worldSet(Player player, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/townyworld set"));
			player.sendMessage(ChatTools.formatCommand("", "/townyworld set", "claimable [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyworld set", "pvp [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyworld set", "usedefault", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyworld set", "wildperm [perm] .. [perm]", "build,destroy,switch,useitem"));
			player.sendMessage(ChatTools.formatCommand("", "/townyworld set", "wildignore [id] [id] [id]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyworld set", "wildname [name]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyworld set", "usingtowny [on/off]", ""));
		} else {
			TownyWorld world;
			if (!plugin.isTownyAdmin(player)) {
			}
			try {
				if (!plugin.isTownyAdmin(player))
					throw new TownyException("This command is admin only.");
				world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			if (split[0].equalsIgnoreCase("claimable")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set claimable on");
				else
					try {
						boolean choice = parseOnOff(split[1]);
						world.setClaimable(choice);
						plugin.sendMsg(player, "Successfully changed " + world.getName() + "'s claimability to " + split[1]);
					} catch (Exception e) {
						plugin.sendErrorMsg(player, "Input error. Please use either on or off.");
					}
			} else if (split[0].equalsIgnoreCase("pvp")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set pvp off");
				else
					try {
						boolean choice = parseOnOff(split[1]);
						world.setPvP(choice);
						plugin.sendMsg(player, "Successfully changed " + world.getName() + "'s PvP " + split[1]);
					} catch (Exception e) {
						plugin.sendErrorMsg(player, "Input error. Please use either on or off.");
					}
			} else if (split[0].equalsIgnoreCase("usedefault"))
				world.setUsingDefault(true);
			else if (split[0].equalsIgnoreCase("wildperm")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set wildperm build destroy");
				else
					try {
						List<String> perms = Arrays.asList(StringMgmt.remFirstArg(split));
						world.setUnclaimedZoneBuild(perms.contains("build"));
						world.setUnclaimedZoneDestroy(perms.contains("destroy"));
						world.setUnclaimedZoneSwitch(perms.contains("switch"));
						world.setUnclaimedZoneItemUse(perms.contains("itemuse"));
						world.setUsingDefault(false);
						plugin.sendMsg(player, "Successfully changed " + world.getName() + "'s wild permissions " + split[1]);
					} catch (Exception e) {
						plugin.sendErrorMsg(player, "Input error. Please use either on or off.");
					}
			} else if (split[0].equalsIgnoreCase("wildignore")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set wildignore 11,25,45,67");
				else
					try {
						List<Integer> nums = new ArrayList<Integer>();
						for (String s: StringMgmt.remFirstArg(split))
							try {
								nums.add(Integer.parseInt(s));
							} catch (NumberFormatException e) {
							}
						world.setUnclaimedZoneIgnore(nums);
						world.setUsingDefault(false);
						plugin.sendMsg(player, "Successfully changed " + world.getName() + "'s wild ignore blocks to " + Arrays.toString(nums.toArray(new Integer[0])));
					} catch (Exception e) {
						plugin.sendErrorMsg(player, "Input error. Please use either on or off.");
					}
			} else if (split[0].equalsIgnoreCase("wildname")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set wildname Wildy");
				else
					try {
						world.setUnclaimedZoneName(split[1]);
						world.setUsingDefault(false);
						plugin.sendMsg(player, "Successfully changed " + world.getName() + "'s wild name to " + split[1]);
					} catch (Exception e) {
						plugin.sendErrorMsg(player, "Input error. Please use either on or off.");
					}
			} else if (split[0].equalsIgnoreCase("usingtowny")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set usingtowny off");
				else
					try {
						boolean choice = parseOnOff(split[1]);
						world.setUsingTowny(choice);
						if (world.isUsingTowny())
							plugin.sendMsg(player, "This is now follows towny rules.");
						else
							plugin.sendMsg(player, "This world is exempt of all towny interactions.");
					} catch (Exception e) {
						plugin.sendErrorMsg(player, "Input error. Please use either on or off.");
					}
			} else {
				plugin.sendErrorMsg(player, "Invalid world property.");
				return;
			}

			plugin.getTownyUniverse().getDataSource().saveWorld(world);
		}
	}
	
	private void warSeed(Player player) {
		/*Resident r1 = plugin.getTownyUniverse().newResident("r1");
		Resident r2 = plugin.getTownyUniverse().newResident("r2");
		Resident r3 = plugin.getTownyUniverse().newResident("r3");
		Coord key = Coord.parseCoord(player);
		Town t1 = newTown(plugin.getTownyUniverse(), player.getWorld(), "t1", r1, key, player.getLocation());
		Town t2 = newTown(plugin.getTownyUniverse(), player.getWorld(), "t2", r2, new Coord(key.getX() + 1, key.getZ()), player.getLocation());
		Town t3 = newTown(plugin.getTownyUniverse(), player.getWorld(), "t3", r3, new Coord(key.getX(), key.getZ() + 1), player.getLocation());
		Nation n1 = */
		
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
		if (split.length == 0)
			showAdminPanel(player);
		else if (split[0].equalsIgnoreCase("?"))
			showTownyAdminHelp(player);
		else if (split[0].equalsIgnoreCase("set")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			adminSet(player, newSplit);
		} else if (split[0].equalsIgnoreCase("war")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			parseWarCommand(player, newSplit);
		} else if (split[0].equalsIgnoreCase("givebonus"))
			try {
				if (split.length != 3)
					throw new TownyException("Wrong input. Eg: givebonus [town] [n]");
				
				Town town = plugin.getTownyUniverse().getTown(split[1]);
				try {
					town.setBonusBlocks(town.getBonusBlocks() + Integer.parseInt(split[2]));
				} catch (NumberFormatException nfe) {
					throw new TownyException("Bonus amount must be an integer.");
				}
				plugin.getTownyUniverse().getDataSource().saveTown(town);
			} catch (TownyException e) {
				plugin.sendErrorMsg(player, e.getError());
			}
		else if (split[0].equalsIgnoreCase("reload"))
			reloadTowny(player);
		else if (split[0].equalsIgnoreCase("backup"))
			try {
				plugin.getTownyUniverse().getDataSource().backup();
				plugin.sendMsg(player, "Backup sucessful.");
			} catch (IOException e) {
				plugin.sendErrorMsg(player, "Error: " + e.getMessage());
			}
		else if (split[0].equalsIgnoreCase("newday"))
			plugin.getTownyUniverse().newDay();
		else if (split[0].equalsIgnoreCase("tree"))
			plugin.getTownyUniverse().sendUniverseTree(player);
		else if (split[0].equalsIgnoreCase("seed") && TownySettings.getDebug())
			seedTowny();
		else if (split[0].equalsIgnoreCase("warseed") && TownySettings.getDebug())
			warSeed(player);
		else
			plugin.sendErrorMsg(player, "Invalid sub command.");
	}

	public void showAdminPanel(Player player) {
		Runtime run = Runtime.getRuntime();
		player.sendMessage(ChatTools.formatTitle("Towny Admin Panel"));
		player.sendMessage(Colors.Blue + "[" + Colors.LightBlue + "Towny" + Colors.Blue + "] "
				+ Colors.Green + "WarTime: " + Colors.LightGreen + plugin.getTownyUniverse().isWarTime()
				+ Colors.Gray + " | "
				+ Colors.Green + "Health Regen: " + (plugin.getTownyUniverse().isHealthRegenRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off")
				+ Colors.Gray + " | "
				+ Colors.Green + "Mob Removal: " + (plugin.getTownyUniverse().isMobRemovalRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off")
				+ Colors.Gray + " | "
				+ Colors.Green + "Daily: " + (plugin.getTownyUniverse().isDailyTimerRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off"));
		try {
			TownyIConomyObject.checkIConomy();
			player.sendMessage(Colors.Blue + "[" + Colors.LightBlue + "iConomy" + Colors.Blue + "] "
					+ Colors.Green + "Economy: " + Colors.LightGreen + TownyFormatter.formatMoney(getTotalEconomy()) + Colors.Gray + " | "
					+ Colors.Green + "Bank Accounts: " + Colors.LightGreen + getNumBankAccounts());
		} catch (Exception e) {
		}
		player.sendMessage(Colors.Blue + "[" + Colors.LightBlue + "Server" + Colors.Blue + "] "
				+ Colors.Green + "Memory: " + Colors.LightGreen + MemMgmt.getMemSize(run.totalMemory()) + Colors.Gray + " | "
				+ Colors.Green + "Threads: " + Colors.LightGreen + Thread.getAllStackTraces().keySet().size() + Colors.Gray + " | "
				+ Colors.Green + "Time: " + Colors.LightGreen + plugin.getTownyUniverse().getFormatter().getTime());
		player.sendMessage(Colors.Yellow + MemMgmt.getMemoryBar(50, run));

	}

	public double getTotalEconomy() {
		double total = 0;
		try {
			Map<String, Account> map = iConomy.getBank().getAccounts();
			Collection<Account> accounts = map.values();

			for (Account account : accounts)
				total += account.getBalance();
		} catch (Exception e) {
		}
		return total;
	}
	
	public int getNumBankAccounts() {
		try {
			return iConomy.getBank().getAccounts().values().size();
		} catch (Exception e) {
			return 0; 
		}
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
		player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "givebonus [town] [num]", ""));
		//TODO: player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "npc rename [old name] [new name]", ""));
		//TODO: player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "npc list", ""));
		player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "reload", "reload Towny"));
		player.sendMessage(ChatTools.formatCommand("", "/townyadmin", "newday", "Run the new day code"));
	}

	public void adminSet(Player player, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/townyadmin set"));
			//TODO: player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "king [nation] [king]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] [mayor]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] npc", ""));
		} else if (split[0].equalsIgnoreCase("mayor")) {
			if (split.length < 3) {
				player.sendMessage(ChatTools.formatTitle("/townyadmin set mayor"));
				player.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] [mayor]", ""));
				player.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] npc", ""));
			} else
				try {
					Resident newMayor = null;
					if (split[2].equalsIgnoreCase("npc")) {
						String name = nextNpcName();
						plugin.getTownyUniverse().newResident(name);
						newMayor = plugin.getTownyUniverse().getResident(name);
						plugin.getTownyUniverse().getDataSource().saveResident(newMayor);
					} else
						newMayor = plugin.getTownyUniverse().getResident(split[2]);
					Town town = plugin.getTownyUniverse().getTown(split[1]);
					town.setMayor(newMayor);
					plugin.getTownyUniverse().getDataSource().saveTown(town);
					plugin.getTownyUniverse().sendTownMessage(town, TownySettings.getNewMayorMsg(newMayor.getName()));
				} catch (TownyException e) {
					plugin.sendErrorMsg(player, e.getError());
				}
		} else if (split[0].equalsIgnoreCase("devmode"))
			try {
				plugin.setSetting(TownySettings.Bool.DEV_MODE, parseOnOff(split[1]));
				plugin.sendMsg(player, "Turned DevMode " + (TownySettings.isDevMode() ? Colors.Green + "on" : Colors.Red + "off"));
			} catch (Exception e) {
				plugin.sendErrorMsg(player, "Must specify if it's [on/off]");
			}
		else {
			plugin.sendErrorMsg(player, "Invalid administrative property.");
			return;
		}
	}
	
	public String nextNpcName() throws TownyException {
		String name;
		int i = 0;
		do {
			name = TownySettings.getNPCPrefix() + ++i;
			if (!plugin.getTownyUniverse().hasResident(name))
				return name;
			if (i > 100000)
				throw new TownyException("Too many npc's registered.");
		} while (true);
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