package espada;

import battlecode.common.*;

public class Miner extends Unit {
	
    static boolean isPrimaryBuilder = false;

    
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
    	findHQ();

        if (rc.getRoundNum() <= 2) {
            isPrimaryBuilder = true;
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
    
    @Override
    public void run() throws GameActionException {
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
}
