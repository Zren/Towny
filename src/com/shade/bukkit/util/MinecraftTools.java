package com.shade.bukkit.util;

import java.util.HashMap;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * A class of functions related to minecraft in general.
 * 
 * @author Shade (Chris H)
 * @version 1.0
 */

public class MinecraftTools {
	public static long convertToTicks(long t) {
		return t / 50;
	}
	
	public static HashMap<String,Integer> getPlayersPerWorld(Server server) {
		HashMap<String,Integer> m = new HashMap<String,Integer>();
		for (World world : server.getWorlds())
			m.put(world.getName(), 0);
		for (Player player : server.getOnlinePlayers())
			m.put(player.getWorld().getName(), m.get(player.getWorld().getName()) + 1);
		return m;
	}
	
	public static Block getBlockOffset(Block block, int xOffset, int yOffset, int zOffset) {
		return block.getWorld().getBlockAt(block.getX()+xOffset, block.getY()+yOffset, block.getZ()+zOffset);
	}
}
