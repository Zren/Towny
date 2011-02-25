package ca.xshade.bukkit.towny.object;

import org.bukkit.plugin.Plugin;

import ca.xshade.bukkit.towny.IConomyException;
import ca.xshade.bukkit.towny.Towny;

import com.nijikokun.bukkit.iConomy.Account;
import com.nijikokun.bukkit.iConomy.iConomy;

;

public class TownyIConomyObject extends TownyObject {
	private static Towny plugin;

	public static Towny getPlugin() {
		return plugin;
	}

	public static void setPlugin(Towny plugin) {
		TownyIConomyObject.plugin = plugin;
	}

	public boolean pay(int n) throws IConomyException {
		int balance = getIConomyBalance();

		if (balance < n || balance - n < 0)
			return false;

		iConomy.Bank.getAccount(getIConomyName()).setBalance(balance - n);
		return true;
	}

	public void collect(int n) throws IConomyException {
		int balance = getIConomyBalance();
		iConomy.Bank.getAccount(getIConomyName()).setBalance(balance + n);
	}

	public boolean pay(int n, TownyIConomyObject collector) throws IConomyException {
		if (pay(n)) {
			collector.collect(n);
			return true;
		} else
			return false;
	}

	public String getIConomyName() {
		// TODO: Make this less hard coded.
		if (this instanceof Nation)
			return "nation-" + getName();
		else if (this instanceof Town)
			return "town-" + getName();
		else
			return getName();
	}

	public int getIConomyBalance() throws IConomyException {
		checkIConomy();
		return (int)getIConomyAccount().getBalance();
	}
	
	public Account getIConomyAccount() throws IConomyException {
		return iConomy.Bank.getAccount(getIConomyName());
	}
	
	public boolean canPay(int n) throws IConomyException {
		int balance = getIConomyBalance();

		if (balance < n || balance - n < 0)
			return false;
		else
			return true;
	}

	public static iConomy checkIConomy() throws IConomyException {
		if (plugin == null)
			throw new IConomyException("IConomyObject has not had plugin configured.");

		Plugin test = plugin.getServer().getPluginManager().getPlugin("iConomy");

		if (test != null)
			return (iConomy) test;
		else
			throw new IConomyException("IConomy has not been installed.");
	}
	
	@SuppressWarnings("static-access")
	public static String getIConomyCurrency() {
		try {
			return checkIConomy().currency;
		} catch (IConomyException e) {
			return "";
		}
	}
}
