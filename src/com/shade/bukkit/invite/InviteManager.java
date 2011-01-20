package com.shade.bukkit.invite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InviteManager {
	private HashMap<String, List<Invite>> allInvites = new HashMap<String, List<Invite>>();

	public int getNumInvites(String name) {
		try {
			return getInvites(name).size();
		} catch (InviteException e) {
			return 0;
		}
	}

	public List<Invite> getInvites(String name) throws InviteException {
		List<Invite> invites = allInvites.get(name);
		if (invites == null)
			throw new InviteException(name + " has no invites.");
		else
			return invites;
	}

	public List<Invite> getInvites(String name, Class<?> t)
			throws InviteException {
		if (!(Invite.class.isAssignableFrom(t)))
			throw new InviteException(t.getName()
					+ " is not an acceptable type.");
		List<Invite> playerInvites = getInvites(name);
		List<Invite> invites = new ArrayList<Invite>();
		for (Invite invite : playerInvites) {
			if (invite.getClass().isAssignableFrom(t)) {
				invites.add(invite);
			}
		}
		return invites;
	}
}
