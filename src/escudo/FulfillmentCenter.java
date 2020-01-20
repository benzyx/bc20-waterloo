package escudo;

import battlecode.common.*;

public class FulfillmentCenter extends Unit {
	
	static int dronesBuilt = 0;

	public FulfillmentCenter(RobotController _rc) {
		super(_rc);
	}
	
	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		if (beingRushed && rc.getRoundNum() < 500 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost && Math.random() < 0.5) {
			for (Direction dir : directions) {
    			if (tryBuild(RobotType.DELIVERY_DRONE, dir)) {
    				dronesBuilt++;
    				break;
    			}
    		}
		}
		
        if ((dronesBuilt < 15 && rc.getTeamSoup() > 300 && Math.random() < 0.1) ||
            (dronesBuilt >= 15 && rc.getTeamSoup() > 600 && Math.random() < 0.3)){
        	for (Direction dir : directions) {
        		dronesBuilt++;
        		tryBuild(RobotType.DELIVERY_DRONE, dir);
        	}
        }
	}

}
