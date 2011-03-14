package ca.xshade.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.TownyFormatter;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;


public class TownyPricesCommand extends TownyCommand {
	
	public TownyPricesCommand() {
		super("prices");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		Town town = null;
		if (args.length > 0)
			try {
				town = universe.getTown(args[0]);
			} catch (NotRegisteredException x) {
				sendErrorMsg(sender, x.getError());
			}
		if (sender instanceof Player) {
			Player player = (Player)sender;
			if (args.length == 0)
				try {
					Resident resident = universe.getResident(player.getName());
					town = resident.getTown();
				} catch (NotRegisteredException x) {
				}
			for (String line : getTownyPrices(town))
				player.sendMessage(line);
		} else
			// Console
			for (String line : getTownyPrices(town))
				sender.sendMessage(Colors.strip(line));
		return true;
	}
	
	/**
	 * Send the list of costs for iConomy to player Command: /towny prices
	 * 
	 * @param player
	 */

	/*
	 * [New] Town: 100 | Nation: 500
	 * [Upkeep] Town: 10 | Nation: 100
	 * Town [Elden]:
	 *     [Price] Plot: 100 | Outpost: 250
	 *     [Upkeep] Resident: 20 | Plot: 50
	 * Nation [Albion]:
	 *     [Upkeep] Town: 100 | Neutrality: 100 
	 */
	
	//TODO: Proceduralize and make parse function for /towny prices [town]
	public List<String> getTownyPrices(Town town) {
		List<String> output = new ArrayList<String>();
		
		output.add(ChatTools.formatTitle("Prices"));
		output.add(Colors.Yellow + "[New] "
				+ Colors.Green + "Town: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getNewTownPrice())
				+ Colors.Gray + " | "
				+ Colors.Green + "Nation: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getNewNationPrice()));
		output.add(Colors.Yellow + "[Upkeep] "
				+ Colors.Green + "Town: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getTownUpkeepCost())
				+ Colors.Gray + " | "
				+ Colors.Green + "Nation: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getNationUpkeepCost()));
		if (town != null) {
			output.add(Colors.Yellow + "Town ["+universe.getFormatter().getFormattedName(town)+"]");
			output.add(Colors.Rose + "    [Price] "
					+ Colors.Green + "Plot: " + Colors.LightGreen + Integer.toString(town.getPlotPrice())
					+ Colors.Gray + " | "
					+ Colors.Green + "Outpost: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getOutpostCost()));
			output.add(Colors.Rose + "    [Upkeep] "
					+ Colors.Green + "Resident: " + Colors.LightGreen + Integer.toString(town.getTaxes())
					+ Colors.Gray + " | "
					+ Colors.Green + "Plot: " + Colors.LightGreen + Integer.toString(town.getPlotTax()));
			
			Nation nation = null;
			try {
				nation = town.getNation();
			} catch (NotRegisteredException e) {
			}
			if (nation != null) {
				output.add(Colors.Yellow + "Nation ["+universe.getFormatter().getFormattedName(nation)+"]");
				output.add(Colors.Rose + "    [Upkeep] "
					+ Colors.Green + "Town: " + Colors.LightGreen + Integer.toString(nation.getTaxes())
					+ Colors.Gray + " | "
					+ Colors.Green + "Neutrality: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getNationNeutralityCost()));
			}
		}
		return output;
	}
}