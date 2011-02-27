package ca.xshade.bukkit.towny.object;

import org.bukkit.plugin.Plugin;

import ca.xshade.bukkit.towny.IConomyException;
import ca.xshade.bukkit.towny.Towny;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

public class TownyIConomyObject extends TownyObject {
	private static Towny plugin;

	public static Towny getPlugin() {
		return plugin;
	}

	public static void setPlugin(Towny plugin) {
		TownyIConomyObject.plugin = plugin;
	}

	public boolean pay(double n) throws IConomyException {
		Account account = getIConomyAccount();
		double balance = account.getBalance();

		if (balance < n || balance - n < 0)
			return false;

		
		account.setBalance(balance - n);
		account.save();
		return true;
	}

	public void collect(double n) throws IConomyException {
		Account account = getIConomyAccount();
		double balance = account.getBalance();
		account.setBalance(balance + n);
		account.save();
	}

	public boolean pay(double n, TownyIConomyObject collector) throws IConomyException {
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

	public double getIConomyBalance() throws IConomyException {
		checkIConomy();
		Account account = getIConomyAccount();
		return account.getBalance();
	}
	
	public Account getIConomyAccount() throws IConomyException {
		Account account = iConomy.getBank().getAccount(getIConomyName());
		if (account == null) {
			iConomy.getBank().addAccount(getIConomyName());
			account = iConomy.getBank().getAccount(getIConomyName());
			account.save();
		}
		return account;
	}
	
	public boolean canPay(double n) throws IConomyException {
		double balance = getIConomyBalance();

		if (balance < n || balance - n < 0)
			return false;
		else
			return true;
	}

	public static iConomy checkIConomy() throws IConomyException {
		if (plugin == null)
			throw new IConomyException("IConomyObject has not had plugin configured.");

		Plugin test = plugin.getServer().getPluginManager().getPlugin("iConomy");
		
		try {
			if (test != null)
				return (iConomy) test;
			else
				throw new IConomyException("IConomy has not been installed.");
		} catch (Exception e) {
			throw new IConomyException("Incorrect iConomy plugin. Try updating.");
		}
	}
	
	@SuppressWarnings("static-access")
	public static String getIConomyCurrency() {
		try {
			return checkIConomy().getBank().getCurrency();
		} catch (IConomyException e) {
			return "";
		}
	}
	
	@SuppressWarnings("static-access")
	public String getFormattedBalance() {
		try {
			return checkIConomy().getBank().format(getIConomyBalance());
		} catch (IConomyException e) {
			return "0 " + getIConomyCurrency();
		}
	}
}
