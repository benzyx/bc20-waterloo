package espada;

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
}
