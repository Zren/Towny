package com.shade.bukkit.towny;

public class TownyPermission {
	boolean residentBuild, residentDestroy, outsiderBuild, outsiderDestroy, allies, switches;
	
	public TownyPermission() {
		reset();
	}
	
	public void reset() {
		residentBuild = false;
		residentDestroy = false;
		outsiderBuild = false;
		outsiderDestroy = false;
		allies = false;
		switches = false;
	}
	
	public void load(String s) {
		String[] tokens = s.split(",");
		for (String token : tokens) {
			if (token.equalsIgnoreCase("residentBuild"))
				residentBuild = true;
			else if (token.equalsIgnoreCase("residentDestroy"))
				residentDestroy = true;
			else if (token.equalsIgnoreCase("outsiderBuild"))
				outsiderBuild = true;
			else if (token.equalsIgnoreCase("outsiderDestroy"))
				outsiderDestroy = true;
			else if (token.equalsIgnoreCase("allies"))
				allies = true;
			else if (token.equalsIgnoreCase("switches"))
				switches = true;
		}
	}
	
	public String toString() {
		String out = "";
		if (residentBuild)
			out += "residentBuild";
		else if (residentDestroy)
			out += (out.length() > 0 ? "," : "") + "residentDestroy";
		else if (outsiderBuild)
			out += (out.length() > 0 ? "," : "") + "outsiderBuild";
		else if (outsiderDestroy)
			out += (out.length() > 0 ? "," : "") + "outsiderDestroy";
		else if (allies)
			out += (out.length() > 0 ? "," : "") + "allies";
		else if (switches)
			out += (out.length() > 0 ? "," : "") + "switches";
		return out;
	}
}
