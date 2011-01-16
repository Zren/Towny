package com.shade.bukkit.invite;

import com.shade.bukkit.towny.Resident;
import com.shade.bukkit.towny.Town;
import com.shade.bukkit.towny.TownyException;
import com.shade.bukkit.towny.TownyUniverse;

public class TownInvite extends Invite {
	private String invitorName, townName;
	private TownyUniverse universe;
	
	public TownInvite(String inviteeName, String invitorName, TownyUniverse universe, String townName) {
		super(inviteeName);
		this.universe = universe;
		this.invitorName = invitorName;
		this.townName = townName;
	}
	
	@Override
	public String getInviteString() {
		return invitorName + " has invited you to the town " + townName + ".";
	}

	@Override
	public void accept() throws InviteException {
		try {
			Resident invitor = universe.getResident(invitorName);
			Resident invitee = universe.getResident(inviteeName);
			Town town = universe.getTown(townName);
			if (town.isMayor(invitor) || town.hasAssistant(invitor)) {
				town.addResident(invitee);
			} else {
				throw new InviteException("Invitor no longer has permission to do this task.");
			}
		} catch(TownyException e) {
			throw new InviteException(e.getError());
		}
	}

	@Override
	public void deny() {
		
	}

}
