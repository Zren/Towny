package com.shade.bukkit.invite;

public abstract class Invite {
	protected String inviteeName;

	public Invite(String inviteeName) {
		this.inviteeName = inviteeName;
	}

	public String getInviteeName() {
		return inviteeName;
	}

	public abstract String getInviteString();

	public abstract void accept() throws InviteException;

	public abstract void deny();
}
