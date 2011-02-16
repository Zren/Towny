package com.shade.bukkit.util;

import java.util.HashMap;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

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
}
