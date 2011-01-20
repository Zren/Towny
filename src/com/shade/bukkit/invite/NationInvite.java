package com.shade.bukkit.invite;

import com.shade.bukkit.towny.Nation;
import com.shade.bukkit.towny.Resident;
import com.shade.bukkit.towny.Town;
import com.shade.bukkit.towny.TownyException;
import com.shade.bukkit.towny.TownyUniverse;

public class NationInvite extends Invite {
	private String invitorName, nationName;
	private TownyUniverse universe;

	public NationInvite(String inviteeName, String invitorName,
			TownyUniverse universe, String nationName) {
		super(inviteeName);
		this.universe = universe;
		this.invitorName = invitorName;
		this.nationName = nationName;
	}

	@Override
	public String getInviteString() {
		return invitorName + " has invited you to the nation " + nationName
				+ ".";
	}

	@Override
	public void accept() throws InviteException {
		try {
			Resident invitor = universe.getResident(invitorName);
			Resident invitee = universe.getResident(inviteeName);
			Town town = invitee.getTown();
			Nation nation = universe.getNation(nationName);
			if (!town.isMayor(invitee))
				throw new InviteException(
						"You no longer has permission to accept this task.");
			if (nation.isKing(invitor) || nation.hasAssistant(invitor)) {
				nation.addTown(town);
			} else {
				throw new InviteException(
						"Invitor no longer has permission to do this task.");
			}
		} catch (TownyException e) {
			throw new InviteException(e.getError());
		}
	}

	@Override
	public void deny() {

	}

}
