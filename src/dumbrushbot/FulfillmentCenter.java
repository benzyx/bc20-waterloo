package escudo;

import battlecode.common.*;

public class FulfillmentCenter extends Unit {
	
	static int dronesBuilt = 0;
	
	public FulfillmentCenter(RobotController _rc) {
		super(_rc);
	}
	
	
	static boolean safeToBuild(RobotInfo[] robots, MapLocation loc) {
		for (RobotInfo robot : robots) {
			if (robot.getType() == RobotType.NET_GUN &&
					robot.getTeam() == rc.getTeam().opponent() &&
					robot.getLocation().distanceSquaredTo(loc) <= 14) {
				// Don't build if there's an enemy net gun nearby that can shoot us down.
				return false;
			}
		}
		return true;
	}
	@Override
	public void run() throws GameActionException {
		
		txn.updateToLatestBlock();
		RobotInfo[] robots = rc.senseNearbyRobots();
		MapLocation loc = rc.getLocation();
		
		if (beingRushed && rc.getRoundNum() < 500 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost && Math.random() < 0.5) {
			for (Direction dir : directions) {
    			if (safeToBuild(robots, loc.add(dir)) && tryBuild(RobotType.DELIVERY_DRONE, dir)) {
    				dronesBuilt++;
    				break;
    			}
    		}
		}
		
        if ((dronesBuilt < 15 && rc.getTeamSoup() > 300 && Math.random() < 0.5) ||
            (dronesBuilt >= 15 && rc.getTeamSoup() > 600 && Math.random() < 0.3) ||
            (rc.getRoundNum() > 1250 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost)){
        	for (Direction dir : directions) {
        		if (safeToBuild(robots, loc.add(dir)) && tryBuild(RobotType.DELIVERY_DRONE, dir)) {
        			dronesBuilt++;
        			break;
        		}
        	}
        }
	}

}
