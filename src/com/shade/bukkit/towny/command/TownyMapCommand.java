package com.shade.bukkit.towny.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.shade.bukkit.towny.Coord;
import com.shade.bukkit.towny.Nation;
import com.shade.bukkit.towny.NotRegisteredException;
import com.shade.bukkit.towny.Resident;
import com.shade.bukkit.towny.TownBlock;
import com.shade.bukkit.towny.Towny;
import com.shade.bukkit.towny.TownyException;
import com.shade.bukkit.towny.TownyWorld;
import com.shade.bukkit.util.ChatTools;
import com.shade.bukkit.util.Colors;
import com.shade.bukkit.util.Compass;

public class TownyMapCommand extends TownyCommand {
	public TownyMapCommand() {
		super("map");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			showMap(player);
		} else
			// Console
			inGameUseOnly(sender);
		return true;
	}
	
	/**
	 * Send a map of the nearby townblocks status to player Command: /towny map
	 * 
	 * @param player
	 */

	public static void showMap(Player player) {
		Towny plugin = universe.getPlugin();
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
						if (resident.getTown() == townblock.getTown()) {
							// own town
							townyMap[y][x] = Colors.LightGreen;
							try {
								if (resident == townblock.getResident())
									//own plot
									townyMap[y][x] = Colors.Yellow;
							} catch(NotRegisteredException e) {
							}
						} else if (resident.hasNation()) {
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
					if (townblock.isForSale())
						townyMap[y][x] += "$";
					else if (townblock.isHomeBlock())
						townyMap[y][x] += "H";
					else
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
				"  " + Colors.White + "+" + Colors.LightGray + " = Claimed",
				"  " + Colors.White + "$" + Colors.LightGray + " = For sale",
				"  " + Colors.LightGreen + "+" + Colors.LightGray + " = Your town",
				"  " + Colors.Yellow + "+" + Colors.LightGray + " = Your plot",
				"  " + Colors.Green + "+" + Colors.LightGray + " = Ally",
				"  " + Colors.Red + "+" + Colors.LightGray + " = Enemy"
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
}
