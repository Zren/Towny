package com.shade.bukkit.towny.object;

public class TownyPermission {
	public boolean residentBuild, residentDestroy, residentSwitch,
		outsiderBuild, outsiderDestroy, outsiderSwitch,
		allyBuild, allyDestroy, allySwitch;

	public TownyPermission() {
		reset();
	}

	public void reset() {
		setAll(false);
	}

	public void setAll(boolean b) {
		residentBuild = b;
		residentDestroy = b;
		residentSwitch = b;
		outsiderBuild = b;
		outsiderDestroy = b;
		outsiderSwitch = b;
		allyBuild = b;
		allyDestroy = b;
		allySwitch = b;
	}

	public void set(String s, boolean b) {
		if (s.equalsIgnoreCase("residentBuild"))
			residentBuild = b;
		else if (s.equalsIgnoreCase("residentDestroy"))
			residentDestroy = b;
		else if (s.equalsIgnoreCase("residentSwitch"))
			residentSwitch = b;
		else if (s.equalsIgnoreCase("outsiderBuild"))
			outsiderBuild = b;
		else if (s.equalsIgnoreCase("outsiderDestroy"))
			outsiderDestroy = b;
		else if (s.equalsIgnoreCase("outsiderSwitch"))
			outsiderSwitch = b;
		else if (s.equalsIgnoreCase("allyBuild"))
			allyBuild = b;
		else if (s.equalsIgnoreCase("allyDestroy"))
			allyDestroy = b;
		else if (s.equalsIgnoreCase("allySwitch"))
			allySwitch = b;
	}

	public void load(String s) {
		String[] tokens = s.split(",");
		for (String token : tokens)
			set(token, true);
	}

	@Override
	public String toString() {
		String out = "";
		if (residentBuild)
			out += "residentBuild";
		if (residentDestroy)
			out += (out.length() > 0 ? "," : "") + "residentDestroy";
		if (residentSwitch)
			out += (out.length() > 0 ? "," : "") + "residentSwitch";
		if (outsiderBuild)
			out += (out.length() > 0 ? "," : "") + "outsiderBuild";
		if (outsiderDestroy)
			out += (out.length() > 0 ? "," : "") + "outsiderDestroy";
		if (outsiderSwitch)
			out += (out.length() > 0 ? "," : "") + "outsiderSwitches";
		if (allyBuild)
			out += (out.length() > 0 ? "," : "") + "allyBuild";
		if (allyDestroy)
			out += (out.length() > 0 ? "," : "") + "allyDestroy";
		if (allySwitch)
			out += (out.length() > 0 ? "," : "") + "allySwitch";
		return out;
	}
}
