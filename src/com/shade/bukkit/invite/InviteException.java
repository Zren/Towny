package com.shade.bukkit.invite;

public class InviteException extends Exception {
	private static final long serialVersionUID = -2919578280367348694L;
	public String error;
	
	public InviteException() {
		super();
		error = "unknown";
	}
	
	public InviteException(String error) {
		super(error);
		this.error = error;
	}
	
	public String getError() {
		return error;
	}
}
