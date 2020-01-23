package escudo;

import battlecode.common.*;

public class Vaporator extends Unit {

	public Vaporator(RobotController _rc) throws GameActionException {
		super(_rc);
		if (rc.getTeamSoup() > 2) txn.sendSpawnMessage(RobotType.VAPORATOR, 2);
	}
	
	@Override
	public void run() throws GameActionException {
	}

}
