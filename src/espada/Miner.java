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
    	RobotType.DESIGN_SCHOOL,
    	RobotType.VAPORATOR,
    	RobotType.VAPORATOR,
    	RobotType.REFINERY,
    	RobotType.FULFILLMENT_CENTER,
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
        
    	int bestSoupAmount = 0;
		MapLocation soupLoc = null;
		
		// Search for soup locally.
        for (int dx = -6; dx <= 6; dx++) {
        	for (int dy = -6; dy <= 6; dy++) {
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
    static MapLocation smartBuild(RobotType robotType) throws GameActionException{
    	for (Direction dir : directions) {
        	
        	// Do not build anything adjacent to our HQ.
        	MapLocation potentialBuildLocation = rc.getLocation().add(dir);
        	if (potentialBuildLocation.isAdjacentTo(hqLocation) || !onLatticeIntersections(potentialBuildLocation)) {
        		continue;
        	}
        	System.out.println("Potential build spot is: " + potentialBuildLocation);
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
    
    static void primaryBuilder() throws GameActionException{
		rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);
        if (buildingOrderIndex < buildingOrder.length && buildingOrder[buildingOrderIndex].cost < rc.getTeamSoup()) {
        	System.out.println("About to smartbuild!");
        	MapLocation built = smartBuild(buildingOrder[buildingOrderIndex]);
            if (built != null) buildingOrderIndex++;
            
        }
        if (buildingOrderIndex == buildingOrder.length && rc.getRoundNum() > 150 && rc.getTeamSoup() >= RobotType.VAPORATOR.cost && Math.random() > 0.1) {
        	smartBuild(RobotType.VAPORATOR);
        }
        
        // Don't stray too far from HQ!
        if (rc.getRoundNum() < 200 && rc.getLocation().distanceSquaredTo(hqLocation) > 25) {
        	path.tryMove(rc.getLocation().directionTo(hqLocation));
        }
        else {
        	path.tryMove(randomDirection());
        }
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
    	// Option 3: DEPOSITING SOUP
    	else if (rc.getSoupCarrying() > 60) {
    		minerDepositSoup();
    	}
    	else {
    		path.simpleTargetMovement();
    	}
    }

    static void minerMineSoup() throws GameActionException {

    	System.out.println("Trying to mine soup!");

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
    	else if (hqLocation != null && (rc.getTeamSoup() < RobotType.REFINERY.cost || loc.distanceSquaredTo(hqLocation) < 45)) {
    		depositLocation = hqLocation;
    	}
    	else if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {
    	
    		MapLocation built = smartBuild(RobotType.REFINERY);
    		if (built != null) {
    			lastRefineryLocation = built;
    			depositLocation = built;
    		
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
}
