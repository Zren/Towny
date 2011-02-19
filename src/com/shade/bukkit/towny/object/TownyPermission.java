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
		setAll(false);
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
	
	public enum ActionType {
		BUILD,
		DESTROY,
		SWITCH;
		
		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	};
	
	public boolean getResident(ActionType type) {
		switch (type) {
			case BUILD: return residentBuild;
			case DESTROY: return residentDestroy;
			case SWITCH: return residentSwitch;
			default: throw new UnsupportedOperationException();
		}
	}
	
	public boolean getOutsider(ActionType type) {
		switch (type) {
			case BUILD: return outsiderBuild;
			case DESTROY: return outsiderDestroy;
			case SWITCH: return outsiderSwitch;
			default: throw new UnsupportedOperationException();
		}
	}
	
	public boolean getAlly(ActionType type) {
		switch (type) {
			case BUILD: return allyBuild;
			case DESTROY: return allyDestroy;
			case SWITCH: return allySwitch;
			default: throw new UnsupportedOperationException();
		}
	}
	
	public static boolean getUnclaimedZone(ActionType type, TownyWorld world) {
		switch (type) {
			case BUILD: return world.getUnclaimedZoneBuild();
			case DESTROY: return world.getUnclaimedZoneDestroy();
			case SWITCH: return world.getUnclaimedZoneSwitch();
			default: throw new UnsupportedOperationException();
		}
	}
}
