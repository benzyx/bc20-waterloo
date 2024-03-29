package seeding;

import battlecode.common.*;

public class DesignSchool extends Unit {

	static int landscapersBuilt = 0;
	
	public DesignSchool(RobotController _rc) {
		super(_rc);
	}
	
	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		if (beingRushed && rc.getRoundNum() < 500 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost && Math.random() < 0.5) {
			for (Direction dir : directions) {
    			if (tryBuild(RobotType.LANDSCAPER, dir)) {
    				landscapersBuilt++;
    				break;
    			}
    		}
		}

		if ((landscapersBuilt < 8 && rc.getTeamSoup() > 250 && Math.random() < 0.5) ||
			(landscapersBuilt < 15 && rc.getTeamSoup() > 300 && Math.random() < 0.2) ||
			(landscapersBuilt < 30 && rc.getTeamSoup() > 600 && Math.random() < 0.1)) {
    		for (Direction dir : directions) {
    			if (tryBuild(RobotType.LANDSCAPER, dir)) {
    				landscapersBuilt++;
    				break;
    			}
    		}
		}
		
	}
}
