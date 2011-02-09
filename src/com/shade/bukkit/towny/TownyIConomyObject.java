package com.shade.bukkit.towny;

import org.bukkit.plugin.Plugin;

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
		checkIConomy();
		int balance = iConomy.db.get_balance(getIConomyName());

		if (balance < n || balance - n < 0)
			return false;

		iConomy.db.set_balance(getIConomyName(), (balance - n));
		return true;
	}

	public void collect(int n) throws IConomyException {
		checkIConomy();
		int balance = iConomy.db.get_balance(getIConomyName());
		iConomy.db.set_balance(getIConomyName(), (balance + n));
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
		return iConomy.db.get_balance(getIConomyName());
	}
	
	public boolean canPay(int n) throws IConomyException {
		checkIConomy();
		int balance = iConomy.db.get_balance(getIConomyName());

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
