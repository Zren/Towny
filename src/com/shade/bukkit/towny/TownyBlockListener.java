package com.shade.bukkit.towny;

import org.bukkit.event.player.PlayerListener;

public class TownyBlockListener extends PlayerListener  {
	private final Towny plugin;

    public TownyBlockListener(Towny instance) {
        plugin = instance;
    }
}