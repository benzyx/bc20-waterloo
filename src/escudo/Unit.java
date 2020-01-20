package escudo;

import battlecode.common.*;

public class Unit {
	
	static RobotController rc;
	static PathingLogic path;
	static TransactionLogic txn;

	
	// Things that we read off transactions;
    public static int hqElevation;
    public static MapLocation hqLocation;
    public static int enemyHQElevation;
    public static MapLocation enemyHQLocation;
    
    public static boolean wallComplete = false;
    public static boolean beingRushed = false;
    public static MapLocation rushTargetLocation;

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST};
    
	public Unit(RobotController _rc) {
		rc = _rc;
		path = new PathingLogic(rc);
		txn = new TransactionLogic(rc);
	}
	
	public void run() throws GameActionException { System.out.println("Unit never initialized to derived class!"); }
	

    /**
     * Returns true if the location is on the high ground lattice built by landscapers.
     * For now, the lattice is aligned to put the HQ on the low ground (not on lattice).
     * 
     * @param loc
     * @throws GameActionException
     */
    public static boolean onLatticeTiles(MapLocation loc) {
    	if (hqLocation == null) {
    		return true;
    	}
    	return !(loc.x % 2 == hqLocation.x % 2 && loc.y % 2 == hqLocation.y % 2);
    }
    
    /**
     * Returns points on the lattice intersections.
     * Since units can move diagonally, if we only build buildings here, they will never block movement completely.
     *
     * @param loc
     * @return
     */
    public static boolean onLatticeIntersections(MapLocation loc) {
    	return (loc.x % 2 != hqLocation.x % 2 && loc.y % 2 != hqLocation.y % 2);
    }
    
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }
    
    int roundFlooded (int elevation) {
        //returns the round it gets flooded the given elevation. Last one is for 30
        switch(elevation) {
            case 0: return 0;
            case 1: return 256;
            case 2: return 464;
            case 3: return 677;
            case 4: return 931;
            case 5: return 1210;
            case 6: return 1413;
            case 7: return 1546;
            case 8: return 1640;
            case 9: return 1713;
            case 10: return 1771;
            case 11: return 1819;
            case 12: return 1861;
            case 13: return 1893;
            case 14: return 1929;
            case 15: return 1957;
            case 16: return 1983;
            case 17: return 2007;
            case 18: return 2028;
            case 19: return 2048;
            case 20: return 2067;
            case 21: return 2084;
            case 22: return 2100;
            case 23: return 2115;
            case 24: return 5459;
            case 25: return 2143;
            case 26: return 2155;
            case 27: return 2168;
            case 28: return 2179;
            case 29: return 2190;
            default: return 2201;
        }
    }
}
