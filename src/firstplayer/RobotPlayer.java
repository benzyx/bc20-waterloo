package firstplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    
    static int secretKey = 0xF3F35D5D;

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

    static int turnCount;
    static int minersBuilt;
    static int dronesBuilt;

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
        RobotType.MINER,
        RobotType.MINER
    };

    /**
     * Hardcoded initial build order costs.
     **/
    static int[] buildOrderCosts = {
        70,
        70,
        70,
        70,
        70
    };
    
    static int buildingOrderIndex;
    static RobotType[] buildingOrder = {
    	RobotType.DESIGN_SCHOOL,
    	RobotType.FULFILLMENT_CENTER,
    };
    
    static int[] buildingCost = {
        200,
        200
    };
    
    static MapLocation hqLocation;
    static MapLocation targetLocation;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        turnCount = 0;
        minersBuilt = 0;
        dronesBuilt = 0;
        isPrimaryBuilder = false;
        
        // Find initial HQ building.
        findHQ();
        
        System.out.println("I'm a " + rc.getType() + " and I just got created!");

        switch (rc.getType()) {
        	case HQ:                 initHQ();                break;
            case MINER:              initMiner();             break;
        }

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
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
    
    
    static int[] applyXORtoMessage(int[] message) {
    	for (int i = 0; i < message.length; i++) {
    		message[i] = message[i] ^ secretKey;
    	}
    	return message;
    }
    
    static void sendHQLocationMessage() throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	int[] message = {1, 9, 0, 0, rc.senseElevation(loc), loc.x, loc.y};
    	rc.submitTransaction(applyXORtoMessage(message), 3);
    }

    static void initHQ() throws GameActionException {
    	// try to find soup.
    	
    	
    	// Broadcast our current location
    	sendHQLocationMessage();
    }
 
    static void runHQ() throws GameActionException {
        // build stuff from order index while it exists
        if (buildOrderIndex < buildOrderUnits.length && buildOrderCosts[buildOrderIndex] < rc.getTeamSoup()) {
            for (Direction dir : directions) {
                boolean success = tryBuild(buildOrderUnits[buildOrderIndex], dir);
                if (success) {
                    buildOrderIndex++;
                    break;
                }
            }
        }
        
        // Otherwise, start trying to build miners if we have too much soup.
        
        if (rc.getTeamSoup() > 1200) {
        	for (Direction dir : directions) {
                boolean success = tryBuild(RobotType.MINER, dir);
                if (success) {
                	break;
                }
            }
        }
    }
    
    static void findHQ() {
    	RobotInfo[] robots = rc.senseNearbyRobots();
        // Look for the HQ location
        for (RobotInfo robot : robots) {
            // If this is the HQ of our team
            if (robot.getType() == RobotType.HQ && robot.getTeam() == rc.getTeam()) {
                hqLocation = robot.getLocation();
                System.out.println("hqLocation is: " + hqLocation);
            }
        }
    }

    static void initMiner() {
        
        if (rc.getRoundNum() <= 2) {
            isPrimaryBuilder = true;
            buildingOrderIndex = 0;
        }

    }

    static void runMiner() throws GameActionException {
    	
    	if (isPrimaryBuilder) {
    		rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
            if (buildingOrderIndex < buildingOrder.length && buildingCost[buildingOrderIndex] < rc.getTeamSoup()) {
                for (Direction dir : directions) {
                    boolean success = tryBuild(buildingOrder[buildingOrderIndex], dir);
                    if (success) {
                        buildingOrderIndex++;
                        break;
                    }
                }
            }
            
            // Don't stray too far from HQ!
            if (rc.getLocation().distanceSquaredTo(hqLocation) > 12) {
            	tryMove(rc.getLocation().directionTo(hqLocation));
            }
            else {
            	minerDefaultMovement();
            }
    	}
    	else {
    		
    		// Look at nearby locations for soup
    		MapLocation loc = rc.getLocation();
    		if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {	
	    		int bestSoupAmount = 0;
	    		MapLocation soupLoc = new MapLocation(0, 0);
	            for (int dx = -3; dx <= 3; dx++) {
	            	for (int dy = -3; dy <= 3; dy++) {
	            		int soupAmount = rc.senseSoup(loc.translate(dx, dy));
	            		if (bestSoupAmount < soupAmount) {
	            			bestSoupAmount = soupAmount;
	            			soupLoc = loc.translate(dx, dy);
	            		}
	            	}
	            }
	            // found some soup!
	            if (soupLoc != new MapLocation(0, 0)) {
	            	rc.setIndicatorDot(soupLoc, 0, 255, 0);
	            	// move towards there if too far away.
	            	if (soupLoc.isAdjacentTo(loc)) {
	            		if (!tryMine(loc.directionTo(soupLoc))) {
	            			tryMove(randomDirection());
	            		}
	            	}
	            	else {
	            		if (!tryMove(loc.directionTo(soupLoc))) {
	            			tryMove(randomDirection());
	            		}
	            	}
	            }
	            // no soup found
	            else {
	            	minerDefaultMovement();
	            }
    		}
    		else {
    			minerDefaultMovement();
    		}
    	}
    }
    
    static void minerDefaultMovement() throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	
    	// If carrying soup, return home
    	if (rc.getSoupCarrying() > 20) {
    		rc.setIndicatorDot(hqLocation, 0, 0, 255);
        	if (hqLocation.isAdjacentTo(loc)) {
        		tryRefine(loc.directionTo(hqLocation));
        	}
        	else {
        		tryMove(loc.directionTo(hqLocation));
        	}
        }
        else {
        	simpleTargetMovement();
        }
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
    	if (rc.getTeamSoup() > 1100) {
    		for (Direction dir : directions) {
    			tryBuild(RobotType.LANDSCAPER, dir);
    		}
    	}
    }

    static void runFulfillmentCenter() throws GameActionException {
        if (rc.getTeamSoup() > 400) {
        	for (Direction dir : directions) {
        		tryBuild(RobotType.DELIVERY_DRONE, dir);
        	}
        }
    }
    
    static void landscaperMovement() throws GameActionException {
    	
    }

    static void runLandscaper() throws GameActionException {
    	// build lattice
    	
    }
    
    /**
     * Shitty Uniform sample on all map locations lmao. Not too bad though probably.
     * @param loc
     * @return A sample point which we want to move towards
     * @throws GameActionException
     */
    static MapLocation sampleTargetLocation(MapLocation loc) throws GameActionException {
    	return new MapLocation((int)(Math.random() * rc.getMapHeight()), (int)(Math.random() * rc.getMapWidth()));
    }

    static void simpleTargetMovement() throws GameActionException {
    	// Sample a point on the grid.
    	MapLocation loc = rc.getLocation();
    	if (loc == targetLocation) {
    		targetLocation = sampleTargetLocation(loc);
    	}
    	
    	// Simple move towards (since this is a flying unit
    	boolean success = tryMove(loc.directionTo(targetLocation));
    	for (Direction dir : directions) {
    		if (success) break;
    		tryMove(dir);
    	}
    }
 
    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        }
        else {
        	MapLocation loc = rc.getLocation();
        	for (int dx = -3; dx <= 3; dx++) {
        		for (int dy = -3; dy <= 3; dy++) {
        			MapLocation dropTile = loc.translate(dx, dy);
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

    
    
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
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


    static void tryBlockchain() throws GameActionException {

    }
}
