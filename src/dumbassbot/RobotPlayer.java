package dumbassbot;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    
    static int secretKey = 0x77778888;

    static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST};

    static RobotType[] spawnedByMiner = {
        RobotType.REFINERY,
        RobotType.VAPORATOR,
        RobotType.DESIGN_SCHOOL,
        RobotType.FULFILLMENT_CENTER,
        RobotType.NET_GUN};
    
    static boolean isPrimaryBuilder;
    static int buildOrderIndex;

    /**
     * Hardcoded initial build order.
     *
     **/
    static RobotType[] buildOrderUnits = {
        RobotType.MINER,
        RobotType.MINER,
        RobotType.MINER,
        RobotType.MINER
    };
    
    static int buildingOrderIndex;
    static RobotType[] buildingOrder = {
    	RobotType.DESIGN_SCHOOL,
    	RobotType.VAPORATOR,
    	RobotType.VAPORATOR,
    	RobotType.REFINERY,
    	RobotType.FULFILLMENT_CENTER,
    };

    static int hqElevation;
    static MapLocation hqLocation;
    static boolean movedThisTurn;
    
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        isPrimaryBuilder = false;

        switch (rc.getType()) {
        	case HQ:                 initHQ();                break;
            case MINER:              initMiner();             break;
            case LANDSCAPER:         initLandscaper();        break;
        }

        while (true) {

        	movedThisTurn = false;
        	
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
    
    // -------------------------
    // TRANSACTION MESSAGE SUITE
    // -------------------------
    
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
    
    // -----
    // HQ
    // -----
    static void initHQ() throws GameActionException {
    	// try to find soup.
    	hqLocation = rc.getLocation();
    	
    	// Broadcast our current location
    	sendHQLocationMessage(15);
    }
 
    static void runHQ() throws GameActionException {
        // build stuff from order index while it exists
        if (buildOrderIndex < buildOrderUnits.length && buildOrderUnits[buildOrderIndex].cost < rc.getTeamSoup()) {
            for (Direction dir : directions) {
                boolean success = tryBuild(buildOrderUnits[buildOrderIndex], dir);
                if (success) {
                    buildOrderIndex++;
                    break;
                }
            }
        }
        
        // Shoot down enemies that appear.
        RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		if (robot.getTeam() != rc.getTeam() && rc.canShootUnit(robot.getID())) {
    			rc.shootUnit(robot.getID());
    		}
    	}
    	
        // Otherwise, start trying to build miners if we have too much soup.
        if (rc.getTeamSoup() > 300 && Math.random() < 0.1) {
        	for (Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) break;
            }
        }
    }
    
    static void findHQ() throws GameActionException {
    	readBlock(rc.getBlock(1));
    }
    
    // -----
    // MINER
    // -----
    

    static void initMiner() throws GameActionException {

    	findHQ();

        if (rc.getRoundNum() <= 2) {
            isPrimaryBuilder = true;
            buildingOrderIndex = 0;
        }

    }
    
    static MapLocation lastSoupTile;
    static MapLocation lastRefineryLocation;
    static MapLocation depositLocation;

    static MapLocation findSoup() throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	
    	
        // If we can see the lastMinedSoupTile and it no longer has any soup, we can reset it.
        if (lastSoupTile != null && rc.canSenseLocation(lastSoupTile)) {
        	if (rc.senseSoup(lastSoupTile) == 0) lastSoupTile = null;
        	else {
        		return lastSoupTile;
        	}
        }
        
        
    	int bestSoupAmount = 0;
		MapLocation soupLoc = null;
		
		// Search for soup locally.
        for (int dx = -7; dx <= 7; dx++) {
        	for (int dy = -7; dy <= 7; dy++) {
        		if (!rc.canSenseLocation(loc.translate(dx, dy))) continue;
        		
    			int soupAmount = rc.senseSoup(loc.translate(dx, dy));
    			if (bestSoupAmount < soupAmount) {
        			bestSoupAmount = soupAmount;
        			soupLoc = loc.translate(dx, dy);
        		}
        	}
        }
        
        if (soupLoc != null) {
        	lastSoupTile = soupLoc;
        	return soupLoc;
        }
        
        return lastSoupTile;
    }
    
    /**
     * Intelligently builds a building to be on the lattice, and not adjacent to HQ.
     * @param robotType
     * @return true if successfully builds, else false
     * @throws GameActionException
     */
    static boolean smartBuild(RobotType robotType) throws GameActionException{
    	for (Direction dir : directions) {
        	
        	// Do not build anything adjacent to our HQ.
        	MapLocation potentialBuildLocation = rc.getLocation().add(dir);
        	if (potentialBuildLocation.isAdjacentTo(hqLocation) || !onLatticeTiles(potentialBuildLocation)) {
        		continue;
        	}
        	
        	System.out.println("Potential build spot is: " + potentialBuildLocation);
        	System.out.println("hqLocation is: " + hqLocation);
            if (tryBuild(robotType, dir)) return true;
        }
    	return false;
    }
    
    static void runMiner() throws GameActionException {
    	
    	// Sense Nearby Refineries
    	RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		if (robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.REFINERY) {
    			lastRefineryLocation = robot.getLocation();
    		}
    	}

    	// Option 1: PRIMARY BUILDER
    	if (isPrimaryBuilder) {
    		rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);
            if (buildingOrderIndex < buildingOrder.length && buildingOrder[buildingOrderIndex].cost < rc.getTeamSoup()) {
            	System.out.println("About to smartbuild!");
            	boolean success = smartBuild(buildingOrder[buildingOrderIndex]);
                if (success) buildingOrderIndex++;
                
            }
            if (buildingOrderIndex == buildingOrder.length && rc.getRoundNum() > 150 && rc.getTeamSoup() >= RobotType.VAPORATOR.cost && Math.random() > 0.1) {
            	smartBuild(RobotType.VAPORATOR);
            }
            
            // Don't stray too far from HQ!
            if (rc.getLocation().distanceSquaredTo(hqLocation) > 25) {
            	tryMove(rc.getLocation().directionTo(hqLocation));
            }
            else {
            	tryMove(randomDirection());
            }
    	}
    	// Option 2: FINDING / MINING SOUP
    	else if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {	
    		minerMineSoup();
    	}
    	// Option 3: DEPOSITING SOUP
    	else if (rc.getSoupCarrying() > 60) {
    		minerDepositSoup();
    	}
    	else {
    		simpleTargetMovement();
    	}
    }
    
    static void minerMineSoup() throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	MapLocation soupLoc = findSoup();
        // found some soup!
        if (soupLoc != null) {
        	rc.setIndicatorDot(soupLoc, 0, 255, 0);
        	// If we are next to the soup, we will mine it.
        	if (soupLoc.isAdjacentTo(loc)) {
        		targetLocation = null;
        		if (!tryMine(loc.directionTo(soupLoc))) {
        			tryMove(randomDirection());
        		}
        	}
        	
        	// If we are not next to the soup, we will move towards it.
        	else {
        		targetLocation = soupLoc;
        		simpleTargetMovement();
        	}
        }
        else {
        	// Explore the world for more soup.
        	simpleTargetMovement();
        }
    }

    static void minerDepositSoup() throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	
    	// Update target deposit location
    	
    	// If a refinery was seen and isn't too far away.
    	if (lastRefineryLocation != null && (rc.getTeamSoup() < RobotType.REFINERY.cost || loc.distanceSquaredTo(lastRefineryLocation) < 70)) {
    		depositLocation = lastRefineryLocation;
    	}
    	// If HQ isn't too far away.
    	else if (hqLocation != null && (rc.getTeamSoup() < RobotType.REFINERY.cost || loc.distanceSquaredTo(hqLocation) < 45)) {
    		depositLocation = hqLocation;
    	}
    	else if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {
    		for (Direction dir : directions) {
    			if (tryBuild(RobotType.REFINERY, dir)) {
    				lastRefineryLocation = loc.add(dir);
    				depositLocation = lastRefineryLocation;
    				break;
    			}
    		}
    	}
    	
    	if (depositLocation != null) {
			// If we are next to HQ, drop our shit.
			if (loc.isAdjacentTo(depositLocation)) {
	    		tryRefine(loc.directionTo(depositLocation));
	    		targetLocation = null;
	    	}
	    	else {
	    		targetLocation = depositLocation;
	    		rc.setIndicatorLine(loc, depositLocation, 0, 0, 255);
	    		simpleTargetMovement();
			}
    	}
    	else {
    		simpleTargetMovement();
    	}
    }
    
    // ---
    // REFINERY
    // ---
    static void runRefinery() throws GameActionException {
    }
    
    // ---
    // VAPORATOR
    // ---
    static void runVaporator() throws GameActionException {

    }
    
    // ---
    // DESIGN SCHOL
    // ---
    static void runDesignSchool() throws GameActionException {
    	if (rc.getTeamSoup() > 300 && Math.random() < 0.2) {
    		for (Direction dir : directions) {
    			tryBuild(RobotType.LANDSCAPER, dir);
    		}
    	}
    }
    
    // ---
    // FULFILLMENT CENTER
    // ---
    static void runFulfillmentCenter() throws GameActionException {
        if (rc.getTeamSoup() > 800 && Math.random() < 0.1) {
        	for (Direction dir : directions) {
        		tryBuild(RobotType.DELIVERY_DRONE, dir);
        	}
        }
    }
    
    // ----------
    // LANDSCAPER
    // ----------
    
    enum LandscaperMode{
    	FIND_WALL,
    	ON_WALL,
    	ROAM,
    	ATTACK
    };
    
    static LandscaperMode landscaperMode = LandscaperMode.FIND_WALL;

    static MapLocation targetWallLocation = null;
    static int targetDumpPriority;
    static int lastKnownElevationOfWall[] = new int[8];
    static int lastObservedRoundNumberOfWall[] = new int[8];
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
    
    static void initLandscaper() throws GameActionException {
    	findHQ();
    	landscaperMode = LandscaperMode.FIND_WALL;
    }
    static void runLandscaper() throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	
    	if (rc.getLocation().isAdjacentTo(hqLocation)) {
			landscaperMode = LandscaperMode.ON_WALL;
		}
    	
    	switch (landscaperMode) {
    	case FIND_WALL:
    		rc.setIndicatorDot(rc.getLocation(), 128, 128, 128);
    		// Already have somewhere we want to go.
    		if (targetWallLocation != null) {
    			
    			if (rc.canSenseLocation(targetWallLocation)) {
    				
    				// Check if its occupied by one of our own landscapers.
    				RobotInfo robot = rc.senseRobotAtLocation(targetWallLocation);
    				boolean isOurLandscaper = (robot != null && robot.getType() == RobotType.LANDSCAPER && robot.getTeam() == rc.getTeam());
    				
    				// Only check if its our landscaper.
    				if (isOurLandscaper) {
    					targetWallLocation = null;
    				}	
    			}
    			
    		}
    		
    		// Need to find a place to go.
    		if (targetWallLocation == null) {
    			
    			boolean foundWallTarget = false;
    			int senseDirectionCount = 0;
    			// Can we see any walls?
        		for (Direction dir : directions) {
        			MapLocation wallLocation = hqLocation.add(dir);
        			if (rc.onTheMap(wallLocation) && rc.canSenseLocation(wallLocation)) {
        				senseDirectionCount++;
        				RobotInfo robot = rc.senseRobotAtLocation(wallLocation);
        				boolean isOurLandscaper = (robot != null && robot.getType() == RobotType.LANDSCAPER && robot.getTeam() == rc.getTeam());
        				if (!isOurLandscaper) {
        					targetWallLocation = wallLocation;
        					foundWallTarget = true;
        					break;
        				}
        			}
        		}
        		if (!foundWallTarget) {
        			targetWallLocation = hqLocation;
        			if (senseDirectionCount == 8) {
            			landscaperMode = LandscaperMode.ROAM;
            		}
        		}
    		}
    		simpleTargetMovement(targetWallLocation);
    		rc.setIndicatorLine(rc.getLocation(), targetWallLocation, 255, 255, 255);
    		break;
    	// We are the watchers on the wall.
    	case ON_WALL:
    		
    		rc.setIndicatorDot(rc.getLocation(), 255, 255, 255);
    		// If carrying ANY dirt, deposit it onto ourselves. Build a wall.
    		if (rc.getDirtCarrying() > 0) {
    			tryDeposit(Direction.CENTER);
    		}
    		// Otherwise, first try to dig from the designated spot.
    		else if (rc.onTheMap(loc.add(hqLocation.directionTo(loc)))) {
    			tryDig(hqLocation.directionTo(loc));
    		}
    		// Otherwise, it must be an edge case. Find a non-lattice point near by and dig from it.
    		else {
    			for (Direction dir : directions) {
    				MapLocation potentialDigSpot = loc.add(dir);
    				// On the map and not HQ.
    				if (rc.onTheMap(potentialDigSpot) && !potentialDigSpot.equals(hqLocation)) {
    					if(tryDig(dir)) break;
    				}
    			}
    		}
    		break;
		case ATTACK:
			break;
		case ROAM:
			targetLocation = null;
			simpleTargetMovement();
			break;
		default:
			break;
    		
    	}
    	
    }
    		
    // -----
    // DRONE
    // -----
    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
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
        					tryMove(loc.directionTo(dropTile));
        				}
        			}
        		}
        	}
        }
        simpleTargetMovement();
    }

    static void runNetGun() throws GameActionException {
    	RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		if (robot.getTeam() != rc.getTeam() && rc.canShootUnit(robot.getID())) {
    			rc.shootUnit(robot.getID());
    		}
    	}
    }
    
    // -----------
    // PATHFINDING
    // -----------
    
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
