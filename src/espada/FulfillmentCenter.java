package espada;

import battlecode.common.*;

public class FulfillmentCenter extends Unit {

	public FulfillmentCenter(RobotController _rc) {
		super(_rc);
	}
	
	@Override
	public void run() throws GameActionException {
        if ((rc.getRoundNum() < 600 && rc.getTeamSoup() > 300 && Math.random() < 0.1) ||
            (rc.getRoundNum() >= 600 && rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost)){
        	for (Direction dir : directions) {
        		tryBuild(RobotType.DELIVERY_DRONE, dir);
        	}
        }
	}

}
