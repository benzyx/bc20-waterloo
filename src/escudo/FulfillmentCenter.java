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

	public void rushDefense() throws GameActionException{
		RobotInfo[] robots = rc.senseNearbyRobots();
		MapLocation loc = rc.getLocation();
		if (rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost && Math.random() < 0.5) {
			for (Direction dir : directions) {
				if (safeToBuild(robots, loc.add(dir)) && tryBuild(RobotType.DELIVERY_DRONE, dir)) {
					dronesBuilt++;
					break;
				}
			}
		}
	}

	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		if (beingRushed) {
			rushDefense();
		}
		else {
			RobotInfo[] robots = rc.senseNearbyRobots();
			MapLocation loc = rc.getLocation();
	        if ((rc.getTeamSoup() > 800) ||
	        		(rc.getRoundNum() > 1000 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost) ||
	        		(rc.getTeamSoup() > 300
	        				&& rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost 
	        				&& landscapersSpawned >= 2 * dronesSpawned
	        				&& 2 * vaporatorsSpawned >= dronesSpawned) ||
	        		(rc.getTeamSoup() > 530
	        				&& rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost
	        				&& landscapersSpawned >= 2 * dronesSpawned)){
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
