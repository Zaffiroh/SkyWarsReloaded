package com.walrusone.skywarsreloaded.commands.admin;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.commands.BaseCmd;
import com.walrusone.skywarsreloaded.database.DataStorage;
import com.walrusone.skywarsreloaded.enums.LeaderType;

public class UpdateTopCmd extends BaseCmd { 
	
	public UpdateTopCmd(String t) {
		type = t;
		forcePlayer = false;
		cmdName = "updatetop";
		alias = new String[]{"ut"};
		argLength = 1; //counting cmdName
	}

	@Override
	public boolean run() {
		if (SkyWarsReloaded.getCfg().eloEnabled()) {
			DataStorage.get().updateTop(LeaderType.ELO, SkyWarsReloaded.getCfg().getLeaderSize());
		} 
		if (SkyWarsReloaded.getCfg().winsEnabled()) {
			DataStorage.get().updateTop(LeaderType.WINS, SkyWarsReloaded.getCfg().getLeaderSize());
		}
		if (SkyWarsReloaded.getCfg().lossesEnabled()) {
			DataStorage.get().updateTop(LeaderType.LOSSES, SkyWarsReloaded.getCfg().getLeaderSize());
		}
		if (SkyWarsReloaded.getCfg().killsEnabled()) {
			DataStorage.get().updateTop(LeaderType.KILLS, SkyWarsReloaded.getCfg().getLeaderSize());
		}
		if (SkyWarsReloaded.getCfg().deathsEnabled()) {
			DataStorage.get().updateTop(LeaderType.DEATHS, SkyWarsReloaded.getCfg().getLeaderSize());
		}
		if (SkyWarsReloaded.getCfg().xpEnabled()) {
			DataStorage.get().updateTop(LeaderType.XP, SkyWarsReloaded.getCfg().getLeaderSize());
		}
		return true;
	}
}
