package espada;

import battlecode.common.*;

public class Drone extends Unit {

	public enum DroneMode {
		ROAM,
		HOLDING_ENEMY,
		AWAITING_STRIKE,
		SWARM,
	}
	
	static final int safeRadiusFromHQ = 15;
	static final int swarmRound = 1200;

	// Turn this to true to let path.simpleTargetMovement() work.
	static boolean yoloMode = false;

	DroneMode mode = DroneMode.ROAM;
	MapLocation pickUpLocation;
	MapLocation dropOffLocation;
	MapLocation lastFloodedTile;

	public Drone(RobotController _rc) {
		super(_rc);
		// TODO Auto-generated constructor stub
	}
	
	
	/**
	 * Senses nearby enemies
	 * 
	 * @return Closest enemy landscaper
	 * @throws GameActionException
	 */
	public RobotInfo senseEnemies() throws GameActionException {
		MapLocation loc = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        
        
        // Look at all the enemy robots.
        RobotInfo closestEnemyRobot = null;
        int closestDistance = 999999999;
        for (RobotInfo robot : robots) {
        	// If found enemy HQ...
        	if (robot.getType() == RobotType.HQ && enemyHQLocation == null) {
        		enemyHQLocation = robot.getLocation();
        		txn.sendLocationMessage(robot, enemyHQLocation, Math.min(10, rc.getTeamSoup()));
        	}
        	
        	// Get those landscapers (and maybe Miners).
        	if (robot.getType() == RobotType.LANDSCAPER && robot.getLocation().distanceSquaredTo(loc) < closestDistance) {
        		closestDistance = robot.getLocation().distanceSquaredTo(loc);
        		closestEnemyRobot = robot;
        	}
        	else if (robot.getType() == RobotType.MINER && robot.getLocation().distanceSquaredTo(loc) * 3 < closestDistance) {
        		closestDistance = robot.getLocation().distanceSquaredTo(loc);
        		closestEnemyRobot = robot;
        	}
        }
        
        return closestEnemyRobot;
	}
	
	/**
	 * Roaming: trying to find the opponent base.
	 * @throws GameActionException 
	 */
	public void roam(MapLocation loc) throws GameActionException {
		RobotInfo robot = senseEnemies();
		if (robot != null) {
			if (tryPickUp(robot.getID())) {
				mode = DroneMode.HOLDING_ENEMY;
			}
			else {
				path.setTarget(robot.getLocation());
			}
		}
		
		if (loc == null) {
			path.simpleTargetMovement();
		}
		else {
			path.simpleTargetMovement(loc);
		}
		
	}

	public MapLocation getClosestKnownWaterTile() throws GameActionException {
		MapLocation loc = rc.getLocation();
		
		int lowestDistance = 99999;
		MapLocation bestDropTile = null;

    	for (int dx = -4; dx <= 4; dx++) {
    		for (int dy = -4; dy <= 4; dy++) {
    			
    			MapLocation dropTile = loc.translate(dx, dy);
    			if (!rc.canSenseLocation(dropTile)) continue;

    			if (rc.senseFlooding(dropTile) && loc.distanceSquaredTo(dropTile) < lowestDistance) {
    				bestDropTile = dropTile;
    				lowestDistance = loc.distanceSquaredTo(dropTile);
    			}
    		}
    	}
    	
    	if (bestDropTile == null) return lastFloodedTile;
    	lastFloodedTile = bestDropTile;
    	return bestDropTile;
	}
	
	/**
	 * Move towards the dropOffLocation, and drop off when close enough.
	 * @throws GameActionException
	 */
	public void navigateToDropOff() throws GameActionException {
    	MapLocation loc = rc.getLocation();
		if (loc.isAdjacentTo(dropOffLocation)) {
			tryDrop(loc.directionTo(dropOffLocation));
		}
		else {
			path.simpleTargetMovement(dropOffLocation);
		}
	}

	/**
	 * Holding enemy unit: find nearest water to dump to.
	 * @throws GameActionException 
	 * 
	 */
	public void holdingEnemyUnit() throws GameActionException {
		dropOffLocation = getClosestKnownWaterTile();
		if (dropOffLocation != null) {
			rc.setIndicatorLine(rc.getLocation(), dropOffLocation, 255, 255, 255);
			navigateToDropOff();
		}
		else {
			System.out.println("dropOffLocation is none!");
			path.simpleTargetMovement();
		}
	}
	
	public void awaitingStrike() throws GameActionException {
		rc.setIndicatorLine(rc.getLocation(), enemyHQLocation, 255, 0, 0);
		path.simpleTargetMovement(enemyHQLocation);
	}
	
	@Override
	public void run() throws GameActionException {
		
		// Catch up on messages.
		txn.updateToLatestBlock();
		
		// Check if any of the adjacent blocks are water.
		lastFloodedTile = getClosestKnownWaterTile();
		
		if (rc.isCurrentlyHoldingUnit()) {
			mode = DroneMode.HOLDING_ENEMY;
		}
		else if (enemyHQLocation != null) {
			if (rc.getRoundNum() >= swarmRound && (rc.getRoundNum() % swarmRound < 100)) mode = DroneMode.SWARM;
			else mode = DroneMode.AWAITING_STRIKE;
		}
		else {
			mode = DroneMode.ROAM;
		}

        switch(mode) {
        case ROAM:
        	roam(null);
        	break;
        case HOLDING_ENEMY:
        	holdingEnemyUnit();
        	break;
		case AWAITING_STRIKE:
			yoloMode = false;
			rc.setIndicatorLine(rc.getLocation(), enemyHQLocation, 255, 0, 0);
			roam(enemyHQLocation);
			break;
		case SWARM:
			yoloMode = true;
			rc.setIndicatorLine(rc.getLocation(), enemyHQLocation, 0, 255, 0);
			roam(enemyHQLocation);
			break;
		default:
			break;
        	
        }
        path.simpleTargetMovement();
	}
	
	
    /**
     * Attempts to pick up a unit with given ID.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryPickUp(int id) throws GameActionException {
        if (rc.isReady() && rc.canPickUpUnit(id)) {
            rc.pickUpUnit(id);;
            return true;
        } else return false;
    }
    
    /**
     * Attempts drop unit in given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDrop(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDropUnit(dir)) {
            rc.dropUnit(dir);
            return true;
        } else return false;
    }
}
