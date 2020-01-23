package escudo;

import battlecode.common.*;

/**
 * Class that handles smart pathing for all units.
 * @author ben
 *
 */
class PathingLogic {

	static RobotController rc;
	
	
    // This is important!
    // targetLocation is used by all units to determine where they want to move to next.
    // 
    static MapLocation targetLocation;
    
	// Record the last 100 mapLocations
	static int lastLocationsIndex = 0;
	static MapLocation[] lastLocationsOnPath = new MapLocation[100];
	
	final int stuckTargetMemorySize = 10;
	
    MapLocation[] lastStuckTargetTiles = new MapLocation[stuckTargetMemorySize];
    int lastStuckTargetTilesIndex = 0;
    

	public PathingLogic(RobotController _rc) {
		rc = _rc;
	}
	
	static int closestDistanceOnPath;
	static int stuckCounter;

    

    
    static Direction nextCWDirection(Direction dir) {
    	switch (dir) {
    		case NORTH: 	return Direction.NORTHEAST;
    		case NORTHEAST: return Direction.EAST;
    		case EAST: 		return Direction.SOUTHEAST;
    		case SOUTHEAST: return Direction.SOUTH;
    		case SOUTH:		return Direction.SOUTHWEST;
    		case SOUTHWEST: return Direction.WEST;
    		case WEST:      return Direction.NORTHWEST;
    		case NORTHWEST: return Direction.NORTH;
    		case CENTER:    return Direction.CENTER;
    		default: return Direction.CENTER;
    	}
    }
    
    static Direction prevCWDirection(Direction dir) {
    	switch (dir) {
    		case NORTH: 	return Direction.NORTHWEST;
    		case NORTHEAST: return Direction.NORTH;
    		case EAST: 		return Direction.NORTHEAST;
    		case SOUTHEAST: return Direction.EAST;
    		case SOUTH:		return Direction.SOUTHEAST;
    		case SOUTHWEST: return Direction.SOUTH;
    		case WEST:      return Direction.SOUTHWEST;
    		case NORTHWEST: return Direction.WEST;
    		case CENTER:    return Direction.CENTER;
    		default: return Direction.CENTER;
    	}
    }

    /**
     * Uniform sample on all map locations. Not too bad though probably.
     * @param loc
     * @return A sample point which we want to move towards
     * @throws GameActionException
     */
    static MapLocation sampleTargetLocation(MapLocation loc) throws GameActionException {
    	return new MapLocation((int)(Math.random() * rc.getMapWidth()), (int)(Math.random() * rc.getMapHeight()));
    }
    
    /**
     * Simple check to see if its safe to move in that direction.
     * @param dir
     */
    static boolean isSafeToMoveInDirection(Direction dir) throws GameActionException {
    	MapLocation moveSpot = rc.getLocation().add(dir);
    	if (rc.getType() == RobotType.DELIVERY_DRONE) {
    		if (Unit.enemyHQLocation == null) return true;
    		
    		boolean canMove = true;
    		
    		if (!Drone.yoloMode) {
    			if (moveSpot.distanceSquaredTo(Unit.enemyHQLocation) <= Drone.safeRadius) canMove = false;
    			for (int i = 0; i < Drone.netGunCount; i++) {
    				if (moveSpot.distanceSquaredTo(Drone.netGuns[i]) <= Drone.safeRadius) canMove = false;
        		}
    		}
    		
    		return canMove;
    	}
    	else {
        	return (rc.canSenseLocation(moveSpot) && !rc.senseFlooding(moveSpot));
    	}
    	
    }
    
    static boolean alreadyVisitedOnPath(MapLocation loc) {
    	// System.out.println("Checking if we visited " + loc);
    	// System.out.print("State of the array: " + lastLocationsIndex + " : ");
    	// Debug
		for (int i = 0; i < Math.min(100, lastLocationsIndex); i++) {
			MapLocation vis = lastLocationsOnPath[i];
			// System.out.print(vis + " ");
			if (loc.equals(vis)) {
				// System.out.println("!!Yes, we visited it!!");
				return true;
			}
		}
    	return false; 
    }
    

    /**
     * Moves towards targetLocation.
     * @param new location to move to.
     * @throws GameActionException
     */
    void simpleTargetMovement(MapLocation newLoc, boolean onLattice) throws GameActionException {
    	setTarget(newLoc);
    	simpleTargetMovement(onLattice);
    }
    
    /**
     * Moves towards targetLocation.
     * @param new location to move to.
     * @throws GameActionException
     */
    void simpleTargetMovement(MapLocation newLoc) throws GameActionException {
    	setTarget(newLoc);
    	simpleTargetMovement(false);
    }
  
  
    /**
     * Moves towards targetLocation.
     * @throws GameActionException
     */
    void simpleTargetMovement() throws GameActionException {
    	simpleTargetMovement(false);
    }
    
    /**
     * Move towards targetLocation but only on lattice grid tiles.
     * 
     * @param onLattice
     * @throws GameActionException
     */
    void simpleTargetMovement(boolean onLattice) throws GameActionException {
    	// Sample a point on the grid.
    	MapLocation loc = rc.getLocation();
    	if (targetLocation == null || loc.equals(targetLocation)) {
    		setTarget(sampleTargetLocation(loc));
    	}
    	 
    	// Simple move towards
    	Direction dir = loc.directionTo(targetLocation);
    	boolean success = (!onLattice || Unit.onLatticeTiles(loc.add(dir))) && !alreadyVisitedOnPath(targetLocation) && tryMove(loc.directionTo(targetLocation));
    	int tries = 7;
    	Direction cw = dir;
    	Direction ccw = dir;
    	while (tries > 0 && !success) {
    		cw = nextCWDirection(cw);
    		success = (!onLattice || Unit.onLatticeTiles(loc.add(cw))) && !alreadyVisitedOnPath(loc.add(cw)) && tryMove(cw);
    		tries--;
    		if (tries == 0 || success) break;
    		ccw = prevCWDirection(ccw);
    		success = (!onLattice || Unit.onLatticeTiles(loc.add(ccw))) && !alreadyVisitedOnPath(loc.add(ccw)) && tryMove(ccw);
    		tries--;
    	}
    	
    	// We are stuck... Randomize the destination I guess.
    	// actually, just reset the memory for path
    	if (!success) {
    		// Actually. lets not reset the path memory.
    		resetPathMemory();
    		stuckCounter++;
    	}
    }

	/**
	 * Move towards targetLocation but only on lattice grid tiles.
	 *
	 * @param onLattice
	 * @throws GameActionException
	 */
	void flee(boolean onLattice, MapLocation threatLocation) throws GameActionException {
		// Sample a point on the grid.
		MapLocation loc = rc.getLocation();
		boolean success = false;
		Direction dir = threatLocation.directionTo(loc);
		// Simple move away
		//if (threatLocation.isAdjacentTo(loc)) {
			success = (!onLattice || Unit.onLatticeTiles(loc.add(dir))) && tryMove(dir);
		//}

		int tries = 7;
		Direction cw = dir;
		Direction ccw = dir;
		MapLocation candidateLoc;
		while (tries > 0 && !success) {
			cw = nextCWDirection(cw);
			candidateLoc = loc.add(cw);
			success = (!onLattice || Unit.onLatticeTiles(loc.add(cw))) && !candidateLoc.isAdjacentTo(threatLocation) && tryMove(cw);
			tries--;
			if (tries == 0 || success) break;
			ccw = prevCWDirection(ccw);
			candidateLoc = loc.add(ccw);
			success = (!onLattice || Unit.onLatticeTiles(loc.add(ccw))) && !candidateLoc.isAdjacentTo(threatLocation) && tryMove(ccw);
			tries--;
		}

		// We are stuck... Randomize the destination I guess.
		// actually, just reset the memory for path
		if (!success) {
			// Actually. lets not reset the path memory.
			resetPathMemory();
			stuckCounter++;
		}
	}


    /// ========= Move towards CW ==============
    
    /**
     * Moves towards targetLocation.
     * @param new location to move to.
     * @throws GameActionException
     */
    void simpleTargetMovementCWOnly(MapLocation newLoc, boolean onLattice) throws GameActionException {
    	setTarget(newLoc);
    	simpleTargetMovementCWOnly(onLattice);
    }
    
    /**
     * Moves towards targetLocation.
     * @param new location to move to.
     * @throws GameActionException
     */
    void simpleTargetMovementCWOnly(MapLocation newLoc) throws GameActionException {
    	setTarget(newLoc);
    	simpleTargetMovementCWOnly(false);
    }
  
    
    /**
     * Moves towards targetLocation.
     * @throws GameActionException
     */
    void simpleTargetMovementCWOnly() throws GameActionException {
    	simpleTargetMovementCWOnly(false);
    }
    
    
    /**
     * Move towards targetLocation but only in CW direction (with option for turning on lattice tiles).
     * 
     * @param onLattice
     * @throws GameActionException
     */
    void simpleTargetMovementCWOnly(boolean onLattice) throws GameActionException {
    	// Sample a point on the grid.
    	MapLocation loc = rc.getLocation();
    	if (targetLocation == null || loc.equals(targetLocation)) {
    		setTarget(sampleTargetLocation(loc));
    	}
    	 
    	// Simple move towards
    	Direction dir = loc.directionTo(targetLocation);
    	boolean success = (!onLattice || Unit.onLatticeTiles(targetLocation)) && !alreadyVisitedOnPath(targetLocation) && tryMove(loc.directionTo(targetLocation));
    	int tries = 7;
    	Direction cw = dir;
    	while (tries > 0 && !success) {
    		cw = nextCWDirection(cw);
    		success = (!onLattice || Unit.onLatticeTiles(loc.add(cw))) && !alreadyVisitedOnPath(loc.add(cw)) && tryMove(cw);
    		tries--;
    	}
    	
    	// We are stuck... Randomize the destination I guess.
    	// actually, just reset the memory for path
    	if (rc.isReady() && !success) {
    		// Actually. lets not reset the path memory.
    		resetPathMemory();
    		stuckCounter++;
    	}
    }
    
    boolean isPastStuckTarget(MapLocation loc) {
    	for (int i = 0; i < 5 && i < lastStuckTargetTilesIndex; i++) {
    		if (loc.equals(lastStuckTargetTiles[i])) return true;
    	}
    	return false;
    }

    boolean isStuck() {
    	if (stuckCounter >= 3) {
    		lastStuckTargetTiles[lastStuckTargetTilesIndex % stuckTargetMemorySize] = targetLocation;
    		lastStuckTargetTilesIndex++;
    		return true;
    	}
    	return false;
    }
    
    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && isSafeToMoveInDirection(dir)) {
        	MapLocation oldLoc = rc.getLocation();
            rc.move(dir);
            lastLocationsOnPath[lastLocationsIndex % 100] = oldLoc;
            lastLocationsIndex++;
            return true;
        } else return false;
    }
    
    void resetPathMemory() {
    	lastLocationsIndex = 0;
    }
    void resetTarget() {
    	targetLocation = null;
    	stuckCounter = 0;
    	closestDistanceOnPath = 999999999;
        resetPathMemory();
    }
    
    void setTarget(MapLocation loc) {
    	if (loc == null || !loc.equals(targetLocation)) resetTarget();
    	targetLocation = loc;
    	if (targetLocation != null && loc != null) closestDistanceOnPath = loc.distanceSquaredTo(targetLocation);
    	else closestDistanceOnPath = 999999999;
    }
}