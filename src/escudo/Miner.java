package escudo;

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
    	RobotType.FULFILLMENT_CENTER,
    	RobotType.REFINERY,
    	RobotType.VAPORATOR,
    	RobotType.VAPORATOR,
    	RobotType.VAPORATOR,
    };
    
    
    static int designSchoolsBuilt = 0;
    static int fulfillmentCentersBuilt = 0;
    static int vaporatorsBuilt = 0;
    static int refineriesBuilt = 0;
    static int netGunsBuilt = 0;
    

    static int lastMinedRound = 0;
    static MapLocation lastSoupTile = null;
    static MapLocation lastRefineryLocation = null;
    static MapLocation depositLocation = null;
    static MapLocation lastSoupTileFromComms = null;
    
	public Miner(RobotController _rc) throws GameActionException {
		super(_rc);
        if (rc.getRoundNum() <= 2) {
            mode = MinerMode.PRIMARY_BUILDER;
            buildingOrderIndex = 0;
        }
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
    		
    		// Flee to high ground.

			if (roundFlooded(rc.senseElevation(rc.getLocation())) <= rc.getRoundNum() + 5) {
				for (Direction dir : directions) {
					if (!rc.onTheMap(rc.getLocation().add(dir))) continue;
					if (rc.senseFlooding(rc.getLocation().add(dir))) {
						path.tryMove(dir.opposite());
						rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(dir.opposite()), 255, 0, 0);
					}
				}
			}
    		
    		if(!macroBuilding()){
				soupCollecting();
			}
    		break;
    	}
    }
    
	static void incrementBuildCounters(RobotType robotType) throws GameActionException {
    	switch (robotType) {
    	case DESIGN_SCHOOL: 		designSchoolsBuilt++; 		break;
    	case VAPORATOR:				vaporatorsBuilt++;    		break;
    	case FULFILLMENT_CENTER:	fulfillmentCentersBuilt++;	break;
    	case NET_GUN:				netGunsBuilt++;				break;
    	case REFINERY:				refineriesBuilt++;			break;
    	default: break;
    	}
    }

    /**
     * Intelligently builds a building to be on the lattice, and not adjacent to HQ.
     * @param robotType
     * @return true if successfully builds, else false
     * @throws GameActionException
     */
    static MapLocation smartBuild(RobotType robotType) throws GameActionException{
    	return smartBuild(robotType, false);
    }
    
    /**
     * Intelligently builds a building to be on the lattice, and not adjacent to HQ.
     * @param robotType
     * @return true if successfully builds, else false
     * @throws GameActionException
     */
    static MapLocation smartBuild(RobotType robotType, boolean desperate) throws GameActionException{
    	for (Direction dir : directions) {
        	
        	// Do not build anything adjacent to our HQ.
        	MapLocation potentialBuildLocation = rc.getLocation().add(dir);
        	if (potentialBuildLocation.isAdjacentTo(hqLocation) || !onLatticeIntersections(potentialBuildLocation)) {
        		continue;
        	}
            if (tryBuild(robotType, dir)) {
            	incrementBuildCounters(robotType);
            	return potentialBuildLocation;
            }
        }
    	
    	if (desperate) {
    		for (Direction dir : directions) {
    			MapLocation potentialBuildLocation = rc.getLocation().add(dir);
    			if (potentialBuildLocation.isAdjacentTo(hqLocation) || !onLatticeTiles(potentialBuildLocation)) {
            		continue;
            	}
                if (tryBuild(robotType, dir)) {
                	incrementBuildCounters(robotType);
                	return potentialBuildLocation;
                }
    		}
    	}
    	return null;
    }
    

    
    static boolean randomWalkOnLattice(int tries) throws GameActionException {
    	// Randomly move on lattice.
    	Direction dir = Direction.CENTER;
    	while (tries > 0) {
    		dir = randomDirection();
    		if (onLatticeTiles(rc.getLocation().add(dir)) && path.tryMove(dir)) {
    			return true;
    		}
    		tries--;
    	}
    	return false;
    }

    static boolean fleeDronesIfNecessary(RobotInfo[] robots) throws GameActionException {
		MapLocation loc = rc.getLocation();
		boolean enemyDrone = false;
		boolean friendlyNetGun = false;
		MapLocation enemyDroneLoc = new MapLocation(0,0);
		for (RobotInfo robot : robots) {
			// Is enemy drone
			if (robot.getType() == RobotType.DELIVERY_DRONE && robot.getTeam() == rc.getTeam().opponent()) {
				enemyDrone = true;
				enemyDroneLoc = robot.getLocation();
			}
			if (robot.getType() == RobotType.NET_GUN && robot.getTeam() == rc.getTeam()) friendlyNetGun = true;
		}

		if (enemyDrone) {
			rc.setIndicatorDot(loc, 0,0,0);
			// boolean highAlert = enemyDroneLoc.isWithinDistanceSquared(loc, 16);
			boolean highAlert = true;
			boolean built = false;
			if(!friendlyNetGun && !loc.isAdjacentTo(enemyDroneLoc)) {
				built = (smartBuild(RobotType.NET_GUN) != null);
			}
			if (!built && highAlert) {
				path.flee(rc.getRoundNum() >= 700, enemyDroneLoc);
			}
			return true;
		}
		return false;
	}

	static MapLocation antiAggroBuild(RobotType robotType, RobotInfo[] nearbyRobots) throws GameActionException{
		Direction targetDir = rc.getLocation().directionTo(hqLocation);
		if (hqLocation.isAdjacentTo(rc.getLocation().add(targetDir).add(targetDir))) {
			if (tryBuild(robotType, targetDir)) {
				incrementBuildCounters(robotType);
				return rc.getLocation().add(targetDir);
			}
		}

		int friendlyLandscaperCount;
		int enemyLandscaperCount;
		Team ourTeam = rc.getTeam();
		for (Direction dir : directions) {
			// Do not build anything adjacent to our HQ.
			MapLocation potentialBuildLocation = rc.getLocation().add(dir);
			if (potentialBuildLocation.isAdjacentTo(hqLocation)) {
				continue;
			}
			friendlyLandscaperCount = 0;
			enemyLandscaperCount = 0;
			for (RobotInfo robot : nearbyRobots) {
				if ( robot.getType() == RobotType.LANDSCAPER ){
					if ( robot.getTeam() == ourTeam ) {
						++friendlyLandscaperCount;
					} else {
						++enemyLandscaperCount;
					}
				}
			}
			if (enemyLandscaperCount > friendlyLandscaperCount + 1) {
				continue;
			}
			if (tryBuild(robotType, dir)) {
				incrementBuildCounters(robotType);
				return potentialBuildLocation;
			}
		}
		return null;
	}

	static void rushDefense() throws GameActionException {
		MapLocation loc = rc.getLocation();
		boolean inCombatZone = rc.canSenseLocation(hqLocation);
		Team ourTeam = rc.getTeam();
		int friendlyLandscaperCount = 0;
		int enemyLandscaperCount = 0;
		int localDesignSchoolCount = 0;

		RobotInfo[] robots = rc.senseNearbyRobots();
		for (RobotInfo robot : robots) {
			RobotType robotType = robot.getType();
			if ( robot.getTeam() == ourTeam ) {
				if ( robotType == RobotType.LANDSCAPER ) {
					++friendlyLandscaperCount;
				} else if ( robotType == RobotType.DESIGN_SCHOOL ) {
					++localDesignSchoolCount;
				}
			} else {
				if ( robot.getType() == RobotType.LANDSCAPER ) {
					++enemyLandscaperCount;
				}
			}
		}
		if (localDesignSchoolCount < 1 && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
			antiAggroBuild(RobotType.DESIGN_SCHOOL, robots);
		} else if (fulfillmentCentersBuilt < 1 && friendlyLandscaperCount >= 1) {
			antiAggroBuild(RobotType.FULFILLMENT_CENTER, robots);
		}
		path.simpleTargetMovement(hqLocation);
	}

    static int lateFullfillmentCenter = 0;
    static int lateDesignSchool = 0;
    static void primaryBuilder() throws GameActionException {
		rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);
		RobotInfo[] robots = rc.senseNearbyRobots(-1);
		if(fleeDronesIfNecessary(robots)) {
			return;
		}
		
		// Handle Rushing by building a fucking fast 
		if (beingRushed) {
			// Build a design school asap.
			rushDefense();
		}

        if (buildingOrderIndex < buildingOrder.length && buildingOrder[buildingOrderIndex].cost < rc.getTeamSoup()) {
        	MapLocation built = smartBuild(buildingOrder[buildingOrderIndex]);
            if (built != null) buildingOrderIndex++;
        }
        
        macroBuilding();
        
        // Don't stray too far from HQ!
        // Still want to stay on lattice though.
        if (rc.getRoundNum() < 120 && rc.getLocation().distanceSquaredTo(hqLocation) > 4
        		|| rc.getRoundNum() < 500 && rc.getLocation().distanceSquaredTo(hqLocation) > 32
        		|| rc.getRoundNum() < 1200 && rc.getLocation().distanceSquaredTo(hqLocation) > 50) {
        	Direction dir = rc.getLocation().directionTo(hqLocation);
        	if (onLatticeTiles(rc.getLocation().add(dir))) path.tryMove(dir);
        }
        
        // Randomly move but prefer lattice tiles.
        if (!randomWalkOnLattice(50)) {
        	path.tryMove(randomDirection());
        }
    }
    
    static boolean macroBuilding() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(-1);
    	if (fleeDronesIfNecessary(robots)) {
			return true;
		}
    	
    	if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost
        		&& (fulfillmentCentersSpawned+1) * 400 < rc.getRoundNum()) {
        	smartBuild(RobotType.FULFILLMENT_CENTER);
        }
    	else if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost
        		&& (designSchoolsSpawned + 1) * 400 < rc.getRoundNum()) {
        	smartBuild(RobotType.DESIGN_SCHOOL);
        }

    	if (rc.getRoundNum() < 1200 && rc.getTeamSoup() >= 500 + 2) {
    		return smartBuild(RobotType.VAPORATOR, true) != null;
    	}
    	return false;
    }

    static void senseNearbyRefineries() throws GameActionException {
    	
    	// update lastRefineryLocation if dead.
    	if (lastRefineryLocation != null && rc.canSenseLocation(lastRefineryLocation)) {
    		RobotInfo robot = rc.senseRobotAtLocation(lastRefineryLocation);
    		if (robot == null || robot.getType() != RobotType.REFINERY || robot.getTeam() != rc.getTeam()) {
    			lastRefineryLocation = null;
    		}
    	}

    	// Sense Nearby Refineries
    	RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		if (robot.getTeam().equals(rc.getTeam()) && robot.getType() == RobotType.REFINERY) {
    			lastRefineryLocation = robot.getLocation();
    		}
    	}
    }
    
    
	static MapLocation findSoup() throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	
        // If we can see the lastSoupTileOnComms and it no longer has any soup, we can reset it.
        if (lastSoupTileFromComms != null && rc.canSenseLocation(lastSoupTileFromComms)) {
        	if (rc.senseSoup(lastSoupTileFromComms) == 0) lastSoupTileFromComms = null;
        }
    	
        // If we can see the lastMinedSoupTile and it no longer has any soup, we can reset it.
        if (lastSoupTile != null && rc.canSenseLocation(lastSoupTile)) {
        	if (rc.senseSoup(lastSoupTile) == 0) lastSoupTile = null;
        }


        if (lastSoupTile != null && !path.isPastStuckTarget(lastSoupTile)) {
        	return lastSoupTile;
        }

        // By closest, not by amount.
    	int bestDistanceSquared = 999999999;
		MapLocation soupLoc = null;
		
		// Search for soup locally.
        for (int dx = -6; dx <= 6; dx++) {
        	for (int dy = -6; dy <= 6; dy++) {
        		MapLocation potentialSoupLoc = loc.translate(dx, dy);

        		if (!rc.onTheMap(potentialSoupLoc)) continue;
        		if (path.isPastStuckTarget(potentialSoupLoc)) continue;
        		if (!rc.canSenseLocation(potentialSoupLoc)) continue;
    			int soupAmount = rc.senseSoup(potentialSoupLoc);
    			
    			if (soupAmount > 0 && bestDistanceSquared > loc.distanceSquaredTo(potentialSoupLoc)) {
    				bestDistanceSquared = loc.distanceSquaredTo(potentialSoupLoc);
        			soupLoc = loc.translate(dx, dy);
        		}
        	}
        }
        

        if (soupLoc != null) {
        	lastSoupTile = soupLoc;
        	return soupLoc;
        } 
        else if (lastSoupTileFromComms != null && !path.isPastStuckTarget(lastSoupTileFromComms)) {
        	lastSoupTile = lastSoupTileFromComms;
        	return lastSoupTileFromComms;
        }
        return null;
    }

    static void soupCollecting() throws GameActionException {
    	if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {	
    		minerMineSoup();
    	}
    	else if (rc.getSoupCarrying() > 60) {
    		minerDepositSoup();
    	}
    	else {
    		path.simpleTargetMovementCWOnly();
    	}
    }

    static void minerMineSoup() throws GameActionException {

    	MapLocation loc = rc.getLocation();
    	
    	if (path.isStuck()) {
    		path.resetTarget();
    		rc.setIndicatorDot(loc, 255, 255, 255);
    		System.out.println("I am stuck!");
    	}
    	
    	if (lastSoupTileFromComms != null) rc.setIndicatorLine(loc, lastSoupTileFromComms, 255, 0, 255);

    	MapLocation soupLoc = findSoup();
    	
        // found some soup!
        if (soupLoc != null) {
        	rc.setIndicatorLine(loc, soupLoc, 0, 255, 0);
        	// If we are next to the soup, we will mine it.
        	if (soupLoc.isAdjacentTo(loc)) {
        		if (lastMinedRound + 30 < rc.getRoundNum() && lastSoupTileFromComms != null && lastSoupTileFromComms.distanceSquaredTo(soupLoc) > 75) {
        			if (rc.getTeamSoup() >= 1) txn.sendSoupLocationMessage(soupLoc, 1);
        		}
        		lastMinedRound = rc.getRoundNum();
        		tryMine(loc.directionTo(soupLoc));
        	}
        	// If we are not next to the soup, we will move towards it.
        	else {
        		path.simpleTargetMovementCWOnly(soupLoc);
        	}
        }
        else {
        	// Explore the world for more soup.
        	System.out.println("Exploring the world for more soup.");
        	path.simpleTargetMovementCWOnly();
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
	    		path.simpleTargetMovementCWOnly(depositLocation);
	    		rc.setIndicatorLine(rc.getLocation(), depositLocation, 0, 0, 255);
			}
    	}
    	else {
    		path.simpleTargetMovementCWOnly();
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
