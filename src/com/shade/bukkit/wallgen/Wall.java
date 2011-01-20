package com.shade.bukkit.wallgen;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

public class Wall {
	private List<WallSection> sections = new ArrayList<WallSection>();
	private int blockType, height, walkwayHeight;

	public boolean add(WallSection wallSection) {
		return sections.add(wallSection);
	}

	public void clear() {
		sections.clear();
	}

	public boolean remove(WallSection wallSection) {
		return sections.remove(wallSection);
	}

	public List<WallSection> getSections() {
		return sections;
	}

	public void setSections(List<WallSection> sections) {
		this.sections = sections;
	}

	public int getBlockType() {
		return blockType;
	}

	public void setBlockType(int blockType) {
		this.blockType = blockType;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWalkwayHeight() {
		return walkwayHeight;
	}

	public void setWalkwayHeight(int walkwayHeight) {
		this.walkwayHeight = walkwayHeight;
	}

	public Wall() {
		blockType = Material.COBBLESTONE.getId();
		height = 2;
		walkwayHeight = 0;
	}
}
