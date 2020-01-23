package dumbrushbot;

import battlecode.common.*;

public class Drone extends Unit {

	public enum DroneMode {
		ROAM,
		HOLDING_ENEMY,
		AWAITING_STRIKE,
		SWARM,
		RUSH_DEFENSE,
	}
	
	static final int safeRadius = 16;
	static final int swarmRound = 1200;
	
	RobotInfo robotCarrying = null;

	// Turn this to true to let path.simpleTargetMovement() work.
	static boolean yoloMode = false;

	DroneMode mode = DroneMode.ROAM;
	MapLocation pickUpLocation;
	MapLocation dropOffLocation;
	MapLocation lastFloodedTile;

	static public MapLocation[] netGuns = new MapLocation[50];
	static public int netGunCount = 0;

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
				robotCarrying = robot;
				path.resetTarget();
			}
			else {
				path.simpleTargetMovement(robot.getLocation());
			}
		}
		else {
			if (loc == null) {
				path.simpleTargetMovement();
			}
			else {
				path.simpleTargetMovement(loc);
			}
		}
	}

	/**
	 * One caveat : cannot return the cell that's directly underneath for stupid reasons.
	 * Drones can't drop units onto Direction.CENTER
	 * @return
	 * @throws GameActionException
	 */
	public MapLocation getClosestKnownWaterTile() throws GameActionException {
		MapLocation loc = rc.getLocation();
		
		int lowestDistance = 99999;
		MapLocation bestDropTile = null;

    	for (int dx = -4; dx <= 4; dx++) {
    		for (int dy = -4; dy <= 4; dy++) {
    			
    			if (dx == 0 && dy == 0) continue;

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
	 * Move towards the dropOffLocation, and drop off the unit we are holding when close enough.
	 * @throws GameActionException
	 */
	public void navigateToDropOff() throws GameActionException {
    	MapLocation loc = rc.getLocation();
		if (loc.isAdjacentTo(dropOffLocation)) {
			System.out.println("Adjacent to dropoff!");
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
			System.out.println("I am at: " + dropOffLocation);
			System.out.println("dropOffLocation is: " + dropOffLocation);
			rc.setIndicatorLine(rc.getLocation(), dropOffLocation, 255, 255, 255);
			navigateToDropOff();
		}
		else {
			path.simpleTargetMovement();
		}
	}
	
	public void awaitingStrike() throws GameActionException {
		rc.setIndicatorLine(rc.getLocation(), enemyHQLocation, 255, 0, 0);
		path.simpleTargetMovement(enemyHQLocation);
	}
	
	
	/**
	 * Lets drones dodge net guns.
	 * @throws GameActionException
	 */
	public void senseNetGuns() throws GameActionException {
		netGunCount = 0;
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		for (RobotInfo robot : robots) {
			if (robot.getType() == RobotType.NET_GUN) {
				netGuns[netGunCount++] = robot.getLocation();
			}
		}
	}

	@Override
	public void run() throws GameActionException {
		
		// Catch up on messages.
		txn.updateToLatestBlock();
		
		// Make sure we don't get shot down.
		senseNetGuns();

		// Check if any of the adjacent blocks are water.
		lastFloodedTile = getClosestKnownWaterTile();
		
		if (beingRushed && rc.getRoundNum() > 500) {
			beingRushed = false;
			path.resetTarget();
		}
		
		if (rc.isCurrentlyHoldingUnit() && robotCarrying.getTeam() == rc.getTeam().opponent()) {
			mode = DroneMode.HOLDING_ENEMY;
		}
		// Rush defense in first 500 rounds.
		else if (beingRushed) {
			rc.setIndicatorLine(rc.getLocation(), hqLocation, 0, 255, 0);
			mode = DroneMode.RUSH_DEFENSE;
		}
		else if (enemyHQLocation != null) {
			if ((rc.getRoundNum() >= swarmRound && rc.getRoundNum() % 400 < 30) ||
				(rc.getRoundNum() >= 2400 && rc.getRoundNum() % 100 < 30)) mode = DroneMode.SWARM;
			else if (rc.getRoundNum() > swarmRound - 120) mode = DroneMode.AWAITING_STRIKE;
			else mode = DroneMode.ROAM;
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
			
			// Only do this if your ID is even. This way we don't get a drop with all drones carrying landscapers (which would be sad as fuck).
			if (!rc.isCurrentlyHoldingUnit() && rc.getID() % 2 == 0) {
				RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
				// Friendly Landscaper which is not adjacent to either HQ.
				for (RobotInfo robot : robots) {
					if (robot.getType() == RobotType.LANDSCAPER &&
							!robot.getLocation().isAdjacentTo(hqLocation) &&
							!robot.getLocation().isAdjacentTo(enemyHQLocation)) {
						
						// Try to pick up or Move towards it.
						if (tryPickUp(robot.getID())) {
							robotCarrying = robot;
							path.resetTarget();
						}
						else {
							path.simpleTargetMovement(robot.getLocation());
						}
						break;
					}
				}
			}
			roam(enemyHQLocation);
			break;
		case SWARM:
			yoloMode = true;
			// Holding a unit which is the same team as us and is a Landscaper.
			if (rc.isCurrentlyHoldingUnit() && robotCarrying.getType() == RobotType.LANDSCAPER && robotCarrying.getTeam() == rc.getTeam()) {
				for (Direction dir : directions) {
					MapLocation potentialDropSpot = rc.getLocation().add(dir);
					
					// Can sense this spot, it's not flooding, it's empty, and it's next to the enemy HQ.
					if (!rc.canSenseLocation(potentialDropSpot)) continue;
					if (rc.senseFlooding(potentialDropSpot)) continue;
					if (rc.senseRobotAtLocation(potentialDropSpot) != null) continue;
					if (!potentialDropSpot.isAdjacentTo(enemyHQLocation)) continue;

					// So if it's all clear to drop our landscaper near the enemy base, let's do it.
					tryDrop(dir);
					break;
				}
			}
			roam(enemyHQLocation);
			break;
		case RUSH_DEFENSE:
			System.out.println("In rush defense mode!");
			roam(hqLocation);
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
