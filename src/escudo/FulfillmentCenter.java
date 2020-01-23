package escudo;

import battlecode.common.*;

public class FulfillmentCenter extends Unit {
	
	static int dronesBuilt = 0;
	
	public FulfillmentCenter(RobotController _rc) throws GameActionException {
		super(_rc);
		txn.sendSpawnMessage(RobotType.FULFILLMENT_CENTER, 2);
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
		
		if (rc.getRoundNum() > 350) beingRushed = false;

		if (beingRushed) {
			if (rc.getRoundNum() < 500 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost && Math.random() < 0.5) {
				for (Direction dir : directions) {
	    			if (safeToBuild(robots, loc.add(dir)) && tryBuild(RobotType.DELIVERY_DRONE, dir)) {
	    				dronesBuilt++;
	    				break;
	    			}
	    		}
			}
		}
		else {
	        if ((rc.getTeamSoup() > 800) ||
	        		(rc.getRoundNum() > 1000 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost) ||
	        		(rc.getTeamSoup() > 300
	        				&& rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost 
	        				&& landscapersSpawned >= 1.5 * dronesSpawned
	        				&& 2 * vaporatorsSpawned >= dronesSpawned) ||
	        		(rc.getTeamSoup() > 530
	        				&& rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost
	        				&& landscapersSpawned >= 1.5 * dronesSpawned)){
	            	for (Direction dir : directions) {
	            		if (safeToBuild(robots, loc.add(dir)) && tryBuild(RobotType.DELIVERY_DRONE, dir)) {
	            			dronesBuilt++;
	            			break;
	            		}
	            	}
	            }	
		}

	}

}
