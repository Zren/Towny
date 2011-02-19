package com.shade.bukkit.towny.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityListener;

import com.shade.bukkit.towny.IConomyException;
import com.shade.bukkit.towny.NotRegisteredException;
import com.shade.bukkit.towny.Towny;
import com.shade.bukkit.towny.TownySettings;
import com.shade.bukkit.towny.object.Nation;
import com.shade.bukkit.towny.object.Resident;
import com.shade.bukkit.towny.object.Town;
import com.shade.bukkit.towny.object.TownyIConomyObject;
import com.shade.bukkit.towny.war.War;
import com.shade.bukkit.towny.war.WarSpoils;

public class TownyEntityMonitorListener extends EntityListener {
	private final Towny plugin;

	public TownyEntityMonitorListener(Towny instance) {
		plugin = instance;
	}
	
	@Override
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity attackerEntity = event.getDamager();
		Entity defenderEntity = event.getEntity();
		
		if (defenderEntity instanceof Player) {
			Player defenderPlayer = (Player) defenderEntity;
			Player attackerPlayer = null;
			if (defenderPlayer.getHealth() > 0)
				return;
			
			Resident attackerResident = null;
			Resident defenderResident = null;
			
			try {
				defenderResident = plugin.getTownyUniverse().getResident(defenderPlayer.getName());
			} catch (NotRegisteredException e) {
				return;
			}
			
			if (attackerEntity instanceof Player) {
				attackerPlayer = (Player) attackerEntity;
				try {
					attackerResident = plugin.getTownyUniverse().getResident(attackerPlayer.getName());
				} catch (NotRegisteredException e) {
				}
			}
			
			deathPayment(attackerPlayer, defenderPlayer, attackerResident, defenderResident);
			
			if (TownySettings.isRemovingOnMonarchDeath())
				monarchDeath(attackerPlayer, defenderPlayer, attackerResident, defenderResident);
		}
	}
	
	private void monarchDeath(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {
		if (plugin.getTownyUniverse().isWarTime()) {
			War warEvent = plugin.getTownyUniverse().getWarEvent();
			try {
				Nation defenderNation = defenderResident.getTown().getNation();
				if (warEvent.isWarringNation(defenderNation))
					if (defenderResident.isMayor())
						if (defenderResident.isKing()) {
							if (attackerResident != null && attackerResident.hasTown())
								warEvent.remove(attackerResident.getTown(), defenderNation);
							else
								warEvent.remove(defenderNation);
							plugin.getTownyUniverse().sendGlobalMessage(defenderNation.getName() + "'s king was killed. Nation removed from war.");
						} else {
							if (attackerResident != null && attackerResident.hasTown())
								warEvent.remove(attackerResident.getTown(), defenderResident.getTown());
							else
								warEvent.remove(defenderResident.getTown());
							plugin.getTownyUniverse().sendGlobalMessage(defenderResident.getTown() + "'s mayor was killed. Town removed from war.");
						}
			} catch (NotRegisteredException e) {
			}
		}
	}

	public void deathPayment(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {
		if (attackerPlayer != null && plugin.getTownyUniverse().isWarTime() && TownySettings.getWartimeDeathPrice() > 0)
			try {
				if (attackerResident == null)
					throw new NotRegisteredException();
				
				int price = TownySettings.getWartimeDeathPrice();
				int townPrice = 0;
				if (!defenderResident.canPay(price)) {
					townPrice = price - defenderResident.getIConomyBalance();
					price = defenderResident.getIConomyBalance();
				}
				
				if (price > 0) {
					defenderResident.pay(price, attackerResident);
					plugin.sendMsg(attackerPlayer, "You robbed " + defenderResident.getName() +" of " + price + " " + TownyIConomyObject.getIConomyCurrency() + ".");
					plugin.sendMsg(defenderPlayer, attackerResident.getName() + " robbed you of " + price + " " + TownyIConomyObject.getIConomyCurrency() + ".");
				}
				
				// Resident doesn't have enough funds.
				if (townPrice > 0) {
					Town town = defenderResident.getTown();
					if (!town.canPay(townPrice)) {
						// Town doesn't have enough funds.
						townPrice = town.getIConomyBalance();
						try {
							plugin.getTownyUniverse().getWarEvent().remove(attackerResident.getTown(), town);
						} catch (NotRegisteredException e) {
							plugin.getTownyUniverse().getWarEvent().remove(town);
						}
					} else
						plugin.getTownyUniverse().sendTownMessage(town, defenderResident.getName() + "'s wallet couldn't satisfy " + attackerResident.getName() + ". "
								+ townPrice + " taken from town bank.");
					town.pay(townPrice, attackerResident);
				}
			} catch (NotRegisteredException e) {
			} catch (IConomyException e) {
				plugin.sendErrorMsg(attackerPlayer, "Could not take wartime death funds.");
				plugin.sendErrorMsg(defenderPlayer, "Could not take wartime death funds.");
			}
		else if (TownySettings.getDeathPrice() > 0)
			try {
				int price = TownySettings.getDeathPrice();
				if (!defenderResident.canPay(price))
					price = defenderResident.getIConomyBalance();
			
				defenderResident.pay(price, new WarSpoils());
				plugin.sendMsg(defenderPlayer, "You lost " + price + " " + TownyIConomyObject.getIConomyCurrency() + ".");
			} catch (IConomyException e) {
				plugin.sendErrorMsg(defenderPlayer, "Could not take death funds.");
			}
	}

	
}
