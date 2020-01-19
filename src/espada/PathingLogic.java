package espada;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

/**
 * Class that handles smart pathing for all units.
 * @author ben
 *
 */
class PathingLogic {

	static RobotController rc;
	
	// Record the last 100 mapLocations
	static int lastLocationsIndex = 0;
	static MapLocation[] lastLocationsOnPath = new MapLocation[100];

	public PathingLogic(RobotController _rc) {
		rc = _rc;
	}
    
    // This is important!
    // targetLocation is used by all units to determine where they want to move to next.
    // 
    static MapLocation targetLocation;
    
    static Direction nextClockwiseDirection(Direction dir) {
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
    		return (Drone.yoloMode || moveSpot.distanceSquaredTo(Unit.enemyHQLocation) > Drone.safeRadiusFromHQ);
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
    	boolean success = (!onLattice || Unit.onLatticeTiles(targetLocation)) && !alreadyVisitedOnPath(targetLocation) && tryMove(loc.directionTo(targetLocation));
    	int tries = 7;
    	while (tries > 0 && !success) {
    		dir = nextClockwiseDirection(dir);
    		success = (!onLattice || Unit.onLatticeTiles(loc.add(dir))) && !alreadyVisitedOnPath(loc.add(dir)) && tryMove(dir);
    		tries--;
    	}
    	
    	// We are stuck... Randomize the destination I guess.
    	// actually, just reset the memory for path
    	if (!success) {
    		resetPathMemory();
    	}
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
        resetPathMemory();
    }
    
    void setTarget(MapLocation loc) {
    	if (!loc.equals(targetLocation)) resetTarget();
    	targetLocation = loc;
    }
}