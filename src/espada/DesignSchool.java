package espada;

import battlecode.common.*;

public class DesignSchool extends Unit {

	public DesignSchool(RobotController _rc) {
		super(_rc);
	}
	
	@Override
	public void run() throws GameActionException {
    	if (rc.getTeamSoup() > 300 && Math.random() < 0.2) {
    		for (Direction dir : directions) {
    			tryBuild(RobotType.LANDSCAPER, dir);
    		}
    	}
	}
}
