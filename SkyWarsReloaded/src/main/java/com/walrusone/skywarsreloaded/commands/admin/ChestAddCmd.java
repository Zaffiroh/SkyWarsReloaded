package com.walrusone.skywarsreloaded.commands.admin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.enums.ChestType;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.Util;

public class ChestAddCmd extends BaseCmd { 
	
	public ChestAddCmd(String t) {
		type = t;
		forcePlayer = true;
		cmdName = "chestadd";
		alias = new String[]{"ca"};
		argLength = 4; //counting cmdName
	}

	@Override
	public boolean run() {

		String type = args[1];
		ChestType ct = null;
		if (type.equalsIgnoreCase("basic")) {
			ct = ChestType.BASIC;
		} else if (type.equalsIgnoreCase("normal")) {
			ct = ChestType.NORMAL;
		} else if (type.equalsIgnoreCase("op")) {
			ct = ChestType.OP;
		} else {
			player.sendMessage(new Messaging.MessageFormatter().format("error.chesttype"));
			return false;
		}
		
		int percent = 0;
		if (Util.get().isInteger(args[3])) {
			percent = Integer.valueOf(args[3]);
		} else {
			player.sendMessage(new Messaging.MessageFormatter().format("error.chestpercent"));
			return false;
		}
		
		if (!(percent > 0 && percent <=100)) {
			player.sendMessage(new Messaging.MessageFormatter().format("error.chestpercent"));
			return false;
		}
		
		
		String method = args[2];
		if (!method.equalsIgnoreCase("hand") && !method.equalsIgnoreCase("inv")) {
			player.sendMessage(new Messaging.MessageFormatter().format("error.chestmethod"));
			return false;
		}
		
		List<ItemStack> items = new ArrayList<ItemStack>();
		if (method.equalsIgnoreCase("hand")) {
			ItemStack item = SkyWarsReloaded.getNMS().getMainHandItem(player).clone();
			items.add(item);
		} else {
			ItemStack[] inventory = player.getInventory().getContents(); 
			for (ItemStack item : inventory) {
				if (item != null && !item.getType().equals(Material.AIR)) {
					items.add(item);
				}
			}
		}
		
		SkyWarsReloaded.getCM().addItems(items, ct, percent);
		String meth = "INVENTORY";
		if (method.equalsIgnoreCase("hand")) {
			meth = "HAND";
		}
			
		player.sendMessage(new Messaging.MessageFormatter().setVariable("method", meth)
				.setVariable("type", type.toUpperCase()).setVariable("percent", "" + percent).format("command.chestadd"));
		return true;
	}

}
