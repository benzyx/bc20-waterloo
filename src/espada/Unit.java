package espada;

import battlecode.common.*;

public class Unit {
	
    static int hqElevation;
    static MapLocation hqLocation;
    
	static RobotController rc;
	
    static Direction[] directions = {
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
	}
	
	public void run() throws GameActionException { System.out.println("Unit never initialized to derived class!"); }
	
	
    // -------------------------
    // TRANSACTION MESSAGE SUITE
    // -------------------------
	
	static int secretKey = 0xE57ADA00;
    
    static void findHQ() throws GameActionException {
    	readBlock(rc.getBlock(1));
    }

    static int robotTypeToNum(RobotType type) {
    	switch (type) {
    		case HQ:						return 0;
    		case MINER:						return 1;
    		case REFINERY: 					return 2;
    		case VAPORATOR: 				return 3;
    		case DESIGN_SCHOOL: 			return 4;
    		case FULFILLMENT_CENTER: 		return 5;
            case LANDSCAPER:         		return 6;
            case DELIVERY_DRONE:     		return 7;
            case NET_GUN:            		return 8;
			case COW:                       return 9;
			default:
				break;
    	}
    	return -1;
    }
    
    static int[] applyXORtoMessage(int[] message) {
    	for (int i = 0; i < message.length; i++) {
    		message[i] = message[i] ^ secretKey;
    	}
    	return message;
    }
    
    static void sendBuildOrderMessage(int buildOrderIndex) throws GameActionException {
    	
    }
    
    /**
     * Enemy Location messages have type == 8.
     * 
     * Announces to the world where an enemy is.
     * 
     * @param enemy
     * @param cost
     * @throws GameActionException
     */
    static void sendEnemyLocationMessage(RobotInfo enemy, int cost) throws GameActionException {
    	
    	MapLocation enemyLoc = enemy.getLocation();
    	int elevation = -1;
    	// Try to get enemy location elevation
    	if (rc.canSenseLocation(enemyLoc)) {
    		elevation = rc.senseElevation(enemyLoc);
    	}

    	int[] message = {1, 8, 0, robotTypeToNum(enemy.getType()), elevation, enemyLoc.x, enemyLoc.y};
    	rc.submitTransaction(applyXORtoMessage(message), cost);
    }
    
    /**
     * HQLocation Messages have type == 9.
     * 
     * Announces to the world where the HQ is.
     * 
     * @param cost
     * @throws GameActionException 
     */
    static void sendHQLocationMessage(int cost) throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	int[] message = {1, 9, 0, 0, rc.senseElevation(loc), loc.x, loc.y};
    	rc.submitTransaction(applyXORtoMessage(message), cost);
    }
    static void readHQLocationMessage(int[] message) throws GameActionException {
    	hqElevation = message[4];
		hqLocation = new MapLocation(message[5], message[6]);
    }
    
    /**
     * Refinery Location Messages have type == 7.
     * @param t
     * @throws GameActionException
     */
    
    static void readTransaction(Transaction t) throws GameActionException {
    	int[] message = applyXORtoMessage(t.getMessage());
    	if (message[0] != 1) {
    		return;
    	}

    	// 9 is HQ Location
    	switch(message[1]) {
    		case 9: 	readHQLocationMessage(message); 	break;
    	}
    }
    
    static void readBlock(Transaction[] ts) throws GameActionException {
    	for (Transaction t : ts) {
    		readTransaction(t);
    	}
    }
    
    // -----------
    // PATHFINDING
    // -----------

    /**
     * Returns true if the location is on the high ground lattice built by landscapers.
     * For now, the lattice is aligned to put the HQ on the low ground (not on lattice).
     * 
     * @param loc
     * @throws GameActionException
     */
    static boolean onLatticeTiles(MapLocation loc) throws GameActionException {
    	if (hqLocation == null) {
    		return true;
    	}
    	return !(loc.x % 2 == hqLocation.x % 2 && loc.y % 2 == hqLocation.y % 2);
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
    	if (rc.getType() == RobotType.DELIVERY_DRONE) return true;
    	MapLocation moveSpot = rc.getLocation().add(dir);
    	return (rc.canSenseLocation(moveSpot) && !rc.senseFlooding(moveSpot));
    }

    /**
     * Moves towards targetLocation.
     * @param new location to move to.
     * @throws GameActionException
     */
    static void simpleTargetMovement(MapLocation newLoc) throws GameActionException {
    	targetLocation = newLoc;
    	simpleTargetMovement();
    }

    /**
     * Moves towards targetLocation.
     * @throws GameActionException
     */
    static void simpleTargetMovement() throws GameActionException {
    	// Sample a point on the grid.
    	MapLocation loc = rc.getLocation();
    	if (targetLocation == null || loc.equals(targetLocation)) {
    		targetLocation = sampleTargetLocation(loc);
    	}
    	 
    	// Simple move towards
    	Direction dir = loc.directionTo(targetLocation);
    	boolean success = tryMove(loc.directionTo(targetLocation));
    	int tries = 7;
    	while (tries > 0 && !success) {
    		dir = nextClockwiseDirection(dir);
    		success = tryMove(dir);
    		tries--;
    	}
    	
    	// rc.setIndicatorLine(loc, targetLocation, 128, 128, 128);
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
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && isSafeToMoveInDirection(dir)) {
            rc.move(dir);
            return true;
        } else return false;
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

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }
    
    /**
     * Attempts to dig dirt in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDig(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        } else return false;
    }
    
    /**
     * Attempts to deposit dirt in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDeposit(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        } else return false;
    }
}
