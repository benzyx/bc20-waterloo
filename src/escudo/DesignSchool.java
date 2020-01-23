package escudo;

import battlecode.common.*;

public class DesignSchool extends Unit {

	static int landscapersBuilt = 0;
	
	public DesignSchool(RobotController _rc) throws GameActionException {
		super(_rc);
		txn.sendSpawnMessage(RobotType.DESIGN_SCHOOL, 2);
	}
	
	
	public void smartBuild() throws GameActionException {
		MapLocation loc = rc.getLocation();

		for (Direction dir : directions) {
			if (onLatticeTiles(loc.add(dir)) && tryBuild(RobotType.LANDSCAPER, dir)) {
				landscapersBuilt++;
				break;
			}
		}
	}
	
	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		if (rc.getRoundNum() > 350) beingRushed = false;
		
		
		// Being rushed!
		if (beingRushed) {
			if (rc.getRoundNum() < 500 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost && Math.random() < 0.5) {
				smartBuild();
			}
		}
		
		// Not being rushed!
		else {
			if ((landscapersSpawned < 4 && rc.getTeamSoup() > 255 && Math.random() < 0.5) ||
				(rc.getTeamSoup() > 800) ||
			    (rc.getTeamSoup() > 300 && rc.getTeamSoup() > RobotType.LANDSCAPER.cost && landscapersSpawned < dronesSpawned * 2)) {
				smartBuild();
			}
		}	
	}
}
