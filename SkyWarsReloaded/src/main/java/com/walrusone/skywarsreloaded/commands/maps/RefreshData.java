package com.walrusone.skywarsreloaded.commands.maps;


import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.objects.GameMap;
import com.walrusone.skywarsreloaded.utilities.Messaging;

public class RefreshData extends BaseCmd {

	public RefreshData(String t) {
		type = t;
		forcePlayer = false;
		cmdName = "refresh";
		alias = new String[]{"ref"};
		argLength = 1; //counting cmdName
	}

	@Override
	public boolean run() {
		GameMap.loadMapData();
		sender.sendMessage(new Messaging.MessageFormatter().format("maps.refreshed"));
		return true;
	}
}
