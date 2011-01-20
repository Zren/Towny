package com.shade.bukkit.wallgen;

import java.util.List;

public interface Walled {
	public List<WallSection> getWallSections();

	public void setWallSections(List<WallSection> wallSections);

	public boolean hasWallSection(WallSection wallSection);

	public void addWallSection(WallSection wallSection);

	public void removeWallSection(WallSection wallSection);
}
