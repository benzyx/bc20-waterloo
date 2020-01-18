package espada;

import battlecode.common.*;

public class Drone extends Unit {

	public enum DroneMode {
		ROAM,
		ROAM_HOLDING_ENEMY,
		AWAITING_STRIKE,
		SWARM,
	}

	MapLocation enemyHQLocation;
	
	public Drone(RobotController _rc) {
		super(_rc);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            
            for (RobotInfo robot : robots) {
            	if (robot.getType() == RobotType.HQ) {
            		enemyHQLocation = robot.getLocation();
            	}
            }
            if (robots.length > 0) {
                // Pick up a first robot within range
                if (rc.canPickUpUnit(robots[0].getID())) {
                	rc.pickUpUnit(robots[0].getID());
                }
            }
        }
        else {
        	MapLocation loc = rc.getLocation();
        	for (int dx = -7; dx <= 7; dx++) {
        		for (int dy = -7; dy <= 7; dy++) {
        			
        			MapLocation dropTile = loc.translate(dx, dy);
        			if (!rc.canSenseLocation(dropTile)) continue;

        			if (rc.senseFlooding(dropTile)) {
        				if (loc.isAdjacentTo(dropTile)) {
        					rc.dropUnit(loc.directionTo(dropTile));
        				}
        				else {
        					path.tryMove(loc.directionTo(dropTile));
        				}
        			}
        		}
        	}
        }
        path.simpleTargetMovement();
	}

}
