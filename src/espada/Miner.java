package espada;

import battlecode.common.*;

public class Miner extends Unit {
	
	enum MinerMode {
		PRIMARY_BUILDER,
		SOUP_COLLECTING,
	};

	static MinerMode mode = MinerMode.SOUP_COLLECTING;
    
    static int buildingOrderIndex;

    static RobotType[] buildingOrder = {
        RobotType.VAPORATOR,
    	RobotType.DESIGN_SCHOOL,
    	RobotType.REFINERY,
    	RobotType.FULFILLMENT_CENTER,
    	RobotType.VAPORATOR,
    };
    
    static MapLocation lastSoupTile;
    static MapLocation lastRefineryLocation;
    static MapLocation depositLocation;
    
	public Miner(RobotController _rc) throws GameActionException {
		super(_rc);
        if (rc.getRoundNum() <= 2) {
            mode = MinerMode.PRIMARY_BUILDER;
            buildingOrderIndex = 0;
        }
    }
	
	static MapLocation findSoup() throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	
    	
        // If we can see the lastMinedSoupTile and it no longer has any soup, we can reset it.
        if (lastSoupTile != null && rc.canSenseLocation(lastSoupTile)) {
        	if (rc.senseSoup(lastSoupTile) == 0) lastSoupTile = null;
        	else {
        		return lastSoupTile;
        	}
        }
        
        // By closest, not by amount.
    	int bestDistanceSquared = 999999999;
		MapLocation soupLoc = null;
		
		// Search for soup locally.
        for (int dx = -6; dx <= 6; dx++) {
        	for (int dy = -6; dy <= 6; dy++) {
        		if (!rc.canSenseLocation(loc.translate(dx, dy))) continue;
        		
    			int soupAmount = rc.senseSoup(loc.translate(dx, dy));
    			if (soupAmount > 0 && bestDistanceSquared > loc.distanceSquaredTo(loc.translate(dx, dy))) {
    				bestDistanceSquared = loc.distanceSquaredTo(loc.translate(dx, dy));
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
    static MapLocation smartBuild(RobotType robotType) throws GameActionException{
    	for (Direction dir : directions) {
        	
        	// Do not build anything adjacent to our HQ.
        	MapLocation potentialBuildLocation = rc.getLocation().add(dir);
        	if (potentialBuildLocation.isAdjacentTo(hqLocation) || !onLatticeIntersections(potentialBuildLocation)) {
        		continue;
        	}
            if (tryBuild(robotType, dir)) return potentialBuildLocation;
        }
    	return null;
    }
    
    @Override
    public void run() throws GameActionException {
    	txn.updateToLatestBlock();
    	senseNearbyRefineries();
    	switch (mode) {
    	case PRIMARY_BUILDER:
    		primaryBuilder();
    		break;
    	case SOUP_COLLECTING:
    		soupCollecting();
    		break;
    	}
    }
    
    static void randomWalkOnLattice() throws GameActionException {
    	// Randomly move on lattice.
    	Direction dir = Direction.CENTER;
    	int tries = 100;
    	while (tries > 0) {
    		dir = randomDirection();
    		if (onLatticeTiles(rc.getLocation().add(dir)) && path.tryMove(dir)) break;
    		tries--;
    	}	
    }
    
    static int lateFullfillmentCenter = 0;
    static void primaryBuilder() throws GameActionException {
		rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);
        if (buildingOrderIndex < buildingOrder.length && buildingOrder[buildingOrderIndex].cost < rc.getTeamSoup()) {
        	MapLocation built = smartBuild(buildingOrder[buildingOrderIndex]);
            if (built != null) buildingOrderIndex++;
            
        }
        
        if (buildingOrderIndex == buildingOrder.length && rc.getRoundNum() > 600 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost && lateFullfillmentCenter < 2) {
        	MapLocation built = smartBuild(RobotType.FULFILLMENT_CENTER);
        	if (built != null) lateFullfillmentCenter++;
        }
        if (buildingOrderIndex == buildingOrder.length && rc.getRoundNum() > 150 && rc.getTeamSoup() >= RobotType.VAPORATOR.cost && Math.random() > 0.1) {
        	smartBuild(RobotType.VAPORATOR);
        }
        
        // Don't stray too far from HQ!
        // Still want to stay on lattice though.
        if (rc.getRoundNum() < 300 && rc.getLocation().distanceSquaredTo(hqLocation) > 25) {
        	Direction dir = rc.getLocation().directionTo(hqLocation);
        	if (onLatticeTiles(rc.getLocation().add(dir))) path.tryMove(dir);
        }

        path.tryMove(randomDirection());
    }
    
    static void senseNearbyRefineries() throws GameActionException {
    	// Sense Nearby Refineries
    	RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		if (robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.REFINERY) {
    			lastRefineryLocation = robot.getLocation();
    		}
    	}
    }

    static void soupCollecting() throws GameActionException {
    	if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {	
    		minerMineSoup();
    	}
    	else if (rc.getSoupCarrying() > 60) {
    		minerDepositSoup();
    	}
    	else {
    		path.simpleTargetMovement();
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
        		tryMine(loc.directionTo(soupLoc));
        	}
        	// If we are not next to the soup, we will move towards it.
        	else {
        		path.simpleTargetMovement(soupLoc);
        	}
        }
        else {
        	// Explore the world for more soup.
        	System.out.println("Exploring the world for more soup.");
        	path.simpleTargetMovement();
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
    	else if (hqLocation != null && (rc.getTeamSoup() < RobotType.REFINERY.cost || loc.distanceSquaredTo(hqLocation) < 30)) {
    		depositLocation = hqLocation;
    	}
    	else if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {
    		
    		System.out.println("Trying to build refinery!");
    		MapLocation built = smartBuild(RobotType.REFINERY);
    		if (built != null) {
    			lastRefineryLocation = built;
    			depositLocation = built;
    		}
    		else {
    			System.out.println("Failed???");
    		}
    	}
    	
    	if (depositLocation != null) {
			// If we are next to the deposit location, deposit (obviously).
			if (loc.isAdjacentTo(depositLocation)) {
	    		tryRefine(loc.directionTo(depositLocation));
	    		path.resetTarget();
	    	}
	    	else {
	    		path.simpleTargetMovement(depositLocation);
	    		rc.setIndicatorLine(rc.getLocation(), depositLocation, 0, 0, 255);
			}
    	}
    	else {
    		path.simpleTargetMovement();
    	}
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
}