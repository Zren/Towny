package ca.xshade.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.xshade.bukkit.towny.IConomyException;
import ca.xshade.bukkit.towny.TownyFormatter;
import ca.xshade.bukkit.towny.object.ResidentList;
import ca.xshade.bukkit.towny.object.TownBlockOwner;
import ca.xshade.bukkit.towny.object.TownyIConomyObject;
import ca.xshade.bukkit.towny.object.TownyObject;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;
import ca.xshade.util.KeyValue;
import ca.xshade.util.KeyValueTable;


public class TownyTopCommand extends TownyCommand {
	public final List<String> output = new ArrayList<String>();
	
	public TownyTopCommand() {
		super("top");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			output.add(ChatTools.formatTitle("/towny top"));
			output.add(ChatTools.formatCommand("", "/towny top", "money [all/resident/town/nation]", ""));
			output.add(ChatTools.formatCommand("", "/towny top", "residents [all/town/nation]", ""));
			output.add(ChatTools.formatCommand("", "/towny top", "land [all/resident/town]", ""));
		} else if (args[0].equalsIgnoreCase("money"))
			try {
				if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
					List<TownyIConomyObject> list = new ArrayList<TownyIConomyObject>(universe.getResidents());
					list.addAll(universe.getTowns());
					list.addAll(universe.getNations());
					output.add(ChatTools.formatTitle("Top Bank Accounts"));
					output.addAll(getTopBankBalance(list, 10));
				} else if (args[1].equalsIgnoreCase("resident")) {
					output.add(ChatTools.formatTitle("Top Resident Bank Accounts"));
					output.addAll(getTopBankBalance(new ArrayList<TownyIConomyObject>(universe.getResidents()), 10));
				} else if (args[1].equalsIgnoreCase("town")) {
					output.add(ChatTools.formatTitle("Top Town Bank Accounts"));
					output.addAll(getTopBankBalance(new ArrayList<TownyIConomyObject>(universe.getTowns()), 10));
				} else if (args[1].equalsIgnoreCase("nation")) {
					output.add(ChatTools.formatTitle("Top Nation Bank Accounts"));
					output.addAll(getTopBankBalance(new ArrayList<TownyIConomyObject>(universe.getNations()), 10));
				} else 
					sendErrorMsg(sender, "Invalid sub command.");
			} catch (IConomyException e) {
				sendErrorMsg(sender, "IConomy error.");
				sendErrorMsg(sender, e.getError());
			}
		else if (args[0].equalsIgnoreCase("residents"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<ResidentList> list = new ArrayList<ResidentList>(universe.getTowns());
				list.addAll(universe.getNations());
				output.add(ChatTools.formatTitle("Most Residents"));
				output.addAll(getMostResidents(list, 10));
			} else if (args[1].equalsIgnoreCase("town")) {
				output.add(ChatTools.formatTitle("Most Residents in a Town"));
				output.addAll(getMostResidents(new ArrayList<ResidentList>(universe.getTowns()), 10));
			} else if (args[1].equalsIgnoreCase("nation")) {
				output.add(ChatTools.formatTitle("Most Residents in a Nation"));
				output.addAll(getMostResidents(new ArrayList<ResidentList>(universe.getNations()), 10));
			} else
				sendErrorMsg(sender, "Invalid sub command.");
		else if (args[0].equalsIgnoreCase("land"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<TownBlockOwner> list = new ArrayList<TownBlockOwner>(universe.getResidents());
				list.addAll(universe.getTowns());
				output.add(ChatTools.formatTitle("Most Land Owned"));
				output.addAll(getMostLand(list, 10));
			} else if (args[1].equalsIgnoreCase("resident")) {
				output.add(ChatTools.formatTitle("Most Land Owned by Resident"));
				output.addAll(getMostLand(new ArrayList<TownBlockOwner>(universe.getResidents()), 10));
			} else if (args[1].equalsIgnoreCase("town")) {
				output.add(ChatTools.formatTitle("Most Land Owned by Town"));
				output.addAll(getMostLand(new ArrayList<TownBlockOwner>(universe.getTowns()), 10));
			} else
				sendErrorMsg(sender, "Invalid sub command.");
		else
			sendErrorMsg(sender, "Invalid sub command.");
		
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
	
	public List<String> getTopBankBalance(List<TownyIConomyObject> list, int maxListing) throws IConomyException {
		List<String> output = new ArrayList<String>();
		KeyValueTable<TownyIConomyObject,Double> kvTable = new KeyValueTable<TownyIConomyObject,Double>();
		for (TownyIConomyObject obj : list)
			kvTable.put(obj, obj.getIConomyBalance());
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue<TownyIConomyObject,Double> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			TownyIConomyObject town = (TownyIConomyObject)kv.key;
			output.add(String.format(
					Colors.LightGray + "%-20s "+Colors.Gold+"|"+Colors.Blue+" %s",
					universe.getFormatter().getFormattedName(town),
					TownyFormatter.formatMoney((Double)kv.value)));
		}
		return output;
	}
	
	public List<String> getMostResidents(List<ResidentList> list, int maxListing) {
		List<String> output = new ArrayList<String>();
		KeyValueTable<ResidentList,Integer> kvTable = new KeyValueTable<ResidentList,Integer>();
		for (ResidentList obj : list)
			kvTable.put(obj, obj.getResidents().size());
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue<ResidentList,Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			ResidentList residentList = (ResidentList)kv.key;
			output.add(String.format(
					Colors.Blue + "%30s "+Colors.Gold+"|"+Colors.LightGray+" %10d",
					universe.getFormatter().getFormattedName((TownyObject)residentList),
					(Integer)kv.value));
		}
		return output;
	}
	
	public List<String> getMostLand(List<TownBlockOwner> list, int maxListing) {
		List<String> output = new ArrayList<String>();
		KeyValueTable<TownBlockOwner,Integer> kvTable = new KeyValueTable<TownBlockOwner,Integer>();
		for (TownBlockOwner obj : list)
			kvTable.put(obj, obj.getTownBlocks().size());
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue<TownBlockOwner,Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			TownBlockOwner town = (TownBlockOwner)kv.key;
			output.add(String.format(
					Colors.Blue + "%30s "+Colors.Gold+"|"+Colors.LightGray+" %10d",
					universe.getFormatter().getFormattedName(town),
					(Integer)kv.value));
		}
		return output;
	}
}