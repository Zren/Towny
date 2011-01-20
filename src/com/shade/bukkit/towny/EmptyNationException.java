package com.shade.bukkit.towny;

public class EmptyNationException extends TownyException {
	private static final long serialVersionUID = 6093696939107516795L;
	private Nation nation;

	public EmptyNationException(Nation nation) {
		this.setNation(nation);
	}

	public void setNation(Nation nation) {
		this.nation = nation;
	}

	public Nation getNation() {
		return nation;
	}
}
