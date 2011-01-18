package com.shade.bukkit.util;

import java.util.TimerTask;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerBroadCastTimerTask extends TimerTask {
	private JavaPlugin plugin;
	String msg;
	
	public ServerBroadCastTimerTask(JavaPlugin plugin, String msg) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		for (Player player : plugin.getServer().getOnlinePlayers())
			player.sendMessage(msg);
	}

}
