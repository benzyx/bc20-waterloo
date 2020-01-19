package espada;

import battlecode.common.*;

public class DesignSchool extends Unit {

	int landscapersBuilt = 0;
	
	public DesignSchool(RobotController _rc) {
		super(_rc);
	}
	
	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		if (landscapersBuilt < 3 || (rc.getTeamSoup() > 300 && Math.random() < 0.2)) {
    		for (Direction dir : directions) {
    			if (tryBuild(RobotType.LANDSCAPER, dir)) {
    				landscapersBuilt++;
    				break;
    			}
    		}
		}
	}
}
