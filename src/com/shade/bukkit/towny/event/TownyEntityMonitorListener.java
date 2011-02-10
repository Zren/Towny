package com.shade.bukkit.towny.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityListener;

import com.shade.bukkit.towny.IConomyException;
import com.shade.bukkit.towny.NotRegisteredException;
import com.shade.bukkit.towny.Towny;
import com.shade.bukkit.towny.TownySettings;
import com.shade.bukkit.towny.TownyUniverse;
import com.shade.bukkit.towny.object.Coord;
import com.shade.bukkit.towny.object.Resident;
import com.shade.bukkit.towny.object.Town;
import com.shade.bukkit.towny.object.TownBlock;
import com.shade.bukkit.towny.object.TownyIConomyObject;
import com.shade.bukkit.towny.object.TownyWorld;
import com.shade.bukkit.towny.war.WarSpoils;

public class TownyEntityMonitorListener extends EntityListener {
	private final Towny plugin;

	public TownyEntityMonitorListener(Towny instance) {
		plugin = instance;
	}
	
	@Override
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		Entity defender = event.getEntity();

		if (defender instanceof Player) {
			Player b = (Player) defender;
			if (b.getHealth() > 0)
				return;
			
			Resident ra = null;
			Resident rb = null;
			
			try {
				rb = plugin.getTownyUniverse().getResident(b.getName());
			} catch (NotRegisteredException e) {
				return;
			}
			if (attacker instanceof Player && plugin.getTownyUniverse().isWarTime() && TownySettings.getWartimeDeathPrice() > 0) {
				Player a = (Player) attacker;
				try {
					ra = plugin.getTownyUniverse().getResident(a.getName());
					
					int price = TownySettings.getWartimeDeathPrice();
					int townPrice = 0;
					if (!rb.canPay(price)) {
						townPrice = price - rb.getIConomyBalance();
						price = rb.getIConomyBalance();
					}
					
					if (price > 0) {
						rb.pay(price, ra);
						plugin.sendMsg(a, "You robbed " + rb.getName() +" of " + price + " " + TownyIConomyObject.getIConomyCurrency() + ".");
						plugin.sendMsg(b, ra.getName() + " robbed you of " + price + " " + TownyIConomyObject.getIConomyCurrency() + ".");
					}
					
					// Resident doesn't have enough funds.
					if (townPrice > 0) {
						Town town = rb.getTown();
						if (!town.canPay(townPrice)) {
							// Town doesn't have enough funds.
							townPrice = town.getIConomyBalance();
							try {
								plugin.getTownyUniverse().getWarEvent().remove(ra.getTown(), town);
							} catch (NotRegisteredException e) {
								plugin.getTownyUniverse().getWarEvent().remove(town);
							}
						} else
							plugin.getTownyUniverse().sendTownMessage(town, rb.getName() + "'s wallet couldn't satisfy " + ra.getName() + ". "
									+ townPrice + " taken from town bank.");
						town.pay(townPrice, ra);
					}
				} catch (NotRegisteredException e) {
				} catch (IConomyException e) {
					plugin.sendErrorMsg(a, "Could not take wartime death funds.");
					plugin.sendErrorMsg(b, "Could not take wartime death funds.");
				}
			} else if (TownySettings.getDeathPrice() > 0)
				try {
					int price = TownySettings.getDeathPrice();
					if (!rb.canPay(price))
						price = rb.getIConomyBalance();
				
					rb.pay(price, new WarSpoils());
					plugin.sendMsg(b, "You lost " + price + " " + TownyIConomyObject.getIConomyCurrency() + ".");
				} catch (IConomyException e) {
					plugin.sendErrorMsg(b, "Could not take death funds.");
				}
		}
	}

	public boolean preventDamageCall(Player a, Player b) {
		TownyUniverse universe = plugin.getTownyUniverse();

		// Check Town PvP status
		try {
			if (universe.isWarTime())
				throw new Exception();
			
			TownyWorld world = universe.getWorld(a.getWorld().getName());
			Coord key = Coord.parseCoord(a);
			TownBlock townblock = world.getTownBlock(key);

			if (!townblock.getTown().isPVP())
				return true;
		} catch (Exception e) {
		}

		// Check Allies
		if (!TownySettings.getFriendlyFire() && universe.isAlly(a.getName(), b.getName()))
			return true;

		return false;
	}
}
