package espada;

import battlecode.common.*;

public class FulfillmentCenter extends Unit {

	public FulfillmentCenter(RobotController _rc) {
		super(_rc);
	}
	
	@Override
	public void run() throws GameActionException {
        if (rc.getTeamSoup() > 800 && Math.random() < 0.1) {
        	for (Direction dir : directions) {
        		tryBuild(RobotType.DELIVERY_DRONE, dir);
        	}
        }
	}

}
