package com.shade.bukkit.towny;

import org.bukkit.plugin.Plugin;

import com.nijikokun.bukkit.iConomy.iConomy;;

public class TownyIConomyObject extends TownyObject {
	private static TownySettings settings;
	private static Towny plugin;
	private String iConomyNamePrefix;
	
	public static Towny getPlugin() {
		return plugin;
	}

	public static void setPlugin(Towny plugin) {
		TownyIConomyObject.plugin = plugin;
	}
	
	public static void setSettings(TownySettings settings) {
		TownyIConomyObject.settings = settings;
	}

	public static TownySettings getSettings() {
		return settings;
	}

	@SuppressWarnings("static-access")
	public boolean pay(int n) throws IConomyException {
		iConomy iconomy = getIConomy();
		int balance = iconomy.i.getBalance(getIConomyName());

		if(balance < n || (balance - n) < 0) {
		    return false;
		}

		iconomy.i.setBalance(getIConomyName(), (balance - n));
		return true;
	}
	
	@SuppressWarnings("static-access")
	public void collect(int n) throws IConomyException {
		iConomy iconomy = getIConomy();
		int balance = iconomy.i.getBalance(getIConomyName());
		iconomy.i.setBalance(getIConomyName(), (balance + n));
	}
	
	public boolean pay(int n, TownyIConomyObject collector) throws IConomyException {
		if (pay(n)) {
			collector.collect(n);
			return true;
		} else {
			return false;
		}
	}

	public void setIConomyNamePrefix(String iConomyNamePrefix) {
		this.iConomyNamePrefix = iConomyNamePrefix;
	}
	
	public String getIConomyNamePrefix() {
		return iConomyNamePrefix;
	}
	
	public String getIConomyName() {
		//TODO: Make this less hard coded.
		if (this instanceof Nation)
			return "nation " + getName();
		if (this instanceof Town)
			return "town " + getName();
		else
			return getName();
	}
	
	@SuppressWarnings("static-access")
	public int getIConomyBalance() throws IConomyException {
		iConomy iconomy = getIConomy();
		return iconomy.i.getBalance(getIConomyName());
	}
	
	public iConomy getIConomy() throws IConomyException {
		if (settings == null)
			throw new IConomyException("IConomyObject has not had settings configured.");
		if (plugin == null)
			throw new IConomyException("IConomyObject has not had plugin configured.");
		
		Plugin test = plugin.getServer().getPluginManager().getPlugin("iConomy");

	    if(test != null) {
	    	return (iConomy)test;
	    } else {
	    	throw new IConomyException("IConomy has not been installed.");
	    }
	}
}
