package espada;

import battlecode.common.*;

public class FulfillmentCenter extends Unit {
	
	static int dronesBuilt = 0;

	public FulfillmentCenter(RobotController _rc) {
		super(_rc);
	}
	
	@Override
	public void run() throws GameActionException {
        if ((dronesBuilt < 15 && rc.getTeamSoup() > 300 && Math.random() < 0.1) ||
            (dronesBuilt >= 15 && rc.getTeamSoup() > 600 && Math.random() < 0.3)){
        	for (Direction dir : directions) {
        		dronesBuilt++;
        		tryBuild(RobotType.DELIVERY_DRONE, dir);
        	}
        }
	}

}
