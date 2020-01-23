package escudo;

import battlecode.common.*;

public class Landscaper extends Unit {

	static int latticeElevation = 6;

    enum LandscaperMode{
    	EARLY_TERRAFORM,
    	FIND_WALL,
    	ON_WALL,
    	TERRAFORM,
    	ATTACK,
    	RUSH_DEFENSE,
    	EARLY_RUSH,
    	WALL_V2,
    };
    
    static LandscaperMode mode = LandscaperMode.FIND_WALL;

    static MapLocation targetWallLocation = null;
    static MapLocation spawnLocation = null;
    static MapLocation terraformTarget = null;
    static MapLocation attackTarget = null;
    
	public Landscaper(RobotController _rc) throws GameActionException {
		super(_rc);
    	spawnLocation = rc.getLocation();
    	if (rc.getTeamSoup() > 0) txn.sendSpawnMessage(RobotType.LANDSCAPER, 1);
	}
	
	public int priorityOfEnemyBuilding(RobotType type) {
		switch(type) {
		case DESIGN_SCHOOL:
			return 10;
		case NET_GUN:
			return 9;
		case HQ:
			return 11;
		default:
			return 1;
		}
	}
	
	public MapLocation senseHighestPriorityNearbyEnemyBuilding(boolean adjacent) {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		
		int distance = 999999999;
		int priority = 0;
		MapLocation bestEnemyLoc = null;
		MapLocation loc = rc.getLocation();

		for (RobotInfo enemy : enemies) {
			MapLocation enemyLoc = enemy.getLocation();
			if (enemy.getType().isBuilding() &&
					(priority < priorityOfEnemyBuilding(enemy.getType()) ||
							(priority == priorityOfEnemyBuilding(enemy.getType()) &&
							 distance > loc.distanceSquaredTo(enemyLoc)))) {
				if (adjacent && loc.isAdjacentTo(enemyLoc)) {
					return enemyLoc;
				}
				priority = priorityOfEnemyBuilding(enemy.getType());
				distance = loc.distanceSquaredTo(enemyLoc);
				bestEnemyLoc = enemyLoc;
			}
		}
		return bestEnemyLoc;
	}

	@Override
	public void run() throws GameActionException {    
    	txn.updateToLatestBlock();
    	
    	while (latticeElevation < 30 && roundFlooded(latticeElevation) < rc.getRoundNum() + 350) {
    		latticeElevation += 3;
    	}

    	// On wall means on a tile adjacent to the wall. No other way around it I think. Greedy approach is the most reliable in decentralized algorithms.
    	MapLocation loc = rc.getLocation();
    	

    	attackTarget = senseHighestPriorityNearbyEnemyBuilding(loc.isAdjacentTo(hqLocation));
    	// Fucking rushed.
    	if (beingRushed) {
			mode = LandscaperMode.EARLY_RUSH;
		}
    	// Early Terraforming
    	else if (rc.getRoundNum() < 300) {
    		if (attackTarget != null) {
    			mode = LandscaperMode.ATTACK;
    		}
    		else {
    			latticeElevation = Math.max(hqElevation + 3, 3);
        		mode = LandscaperMode.EARLY_TERRAFORM;
    		}
    	}
		// On wall
    	else if (loc.isAdjacentTo(hqLocation))
    	{
    		mode = LandscaperMode.ON_WALL;
    	}
    	else{
    		attackTarget = senseHighestPriorityNearbyEnemyBuilding(false);
    		if (attackTarget != null) {
    			mode = LandscaperMode.ATTACK;
        	}
    		else if (!wallComplete && rc.getRoundNum() <= wallCutoffRound) {
	    		mode = LandscaperMode.FIND_WALL;
	    	}
	    	else {
	    		mode = LandscaperMode.TERRAFORM;
	    	}
    	}

    	// Sense nearby enemies.
    	switch (mode) {
    	case EARLY_TERRAFORM:
    		terraform(true);
    		break;
    	case FIND_WALL:
    		findWall();
    		break;
    	case ON_WALL:
    		onWall();
    		break;
		case ATTACK:
			attack(attackTarget);
			break;
		case TERRAFORM:
			terraform(false);
			break;
		case EARLY_RUSH:
			rushDefense(attackTarget);
			break;
		default:
			break;
    		
    	}
    	
	}
	
	static MapLocation getBestSensedWallLocation() throws GameActionException {
		int bestPriority = 0;
		MapLocation bestLocation = null;
		
		int wallPositionFilledCount = 0;

		// Check all locations adjacent to our 
		for (Direction dir : directions) {
			MapLocation wallLocation = hqLocation.add(dir);
			
			// Not on map
			if (!rc.onTheMap(wallLocation)) {
				wallPositionFilledCount++;
				continue;
			}
			// On Map and can Sense
			if (rc.canSenseLocation(wallLocation)) {
				RobotInfo robot = rc.senseRobotAtLocation(wallLocation);
				boolean isOurLandscaper = (robot != null && robot.getType() == RobotType.LANDSCAPER && robot.getTeam() == rc.getTeam());
				if (isOurLandscaper) {
					wallPositionFilledCount++;
				}
				else {
					rc.setIndicatorLine(rc.getLocation(), spawnLocation, 0, 255, 0);
					int priority = wallLocation.distanceSquaredTo(spawnLocation);
					if (priority > bestPriority) {
						bestPriority = priority;
						bestLocation = wallLocation;
					}
				}
			}
		}
		
		if (wallPositionFilledCount == 8) {
			wallComplete = true;
		}
		return bestLocation;
	}
	
	/**
	 * Find one of the 8 spots on the wall to sit on.
	 * 
	 * @throws GameActionException
	 */
	static void findWall() throws GameActionException {
		rc.setIndicatorDot(rc.getLocation(), 128, 128, 128);
		 
		
		// Already have somewhere we want to go.
		if (targetWallLocation != null) {
	    	if (rc.getLocation().equals(targetWallLocation) && targetWallLocation.isAdjacentTo(hqLocation)) {
				mode = LandscaperMode.ON_WALL;
				onWall();
				return;
			}
	    	else if (rc.canSenseLocation(targetWallLocation)) {
				
				// Check if its occupied by one of our own landscapers.
				RobotInfo robot = rc.senseRobotAtLocation(targetWallLocation);
				boolean isOurLandscaper = (robot != null && robot.getType() == RobotType.LANDSCAPER && robot.getTeam() == rc.getTeam());
				
				// Only check if its our landscaper.
				if (isOurLandscaper) {
					targetWallLocation = null;
					path.resetTarget();
				}	
			}
			
		}
		
		// Need to find a place to go.
		if (targetWallLocation == null) {
			
			boolean foundWallTarget = false;
			
			// Can we see any walls? If so, find the best wall
    		MapLocation wallLocation = getBestSensedWallLocation();
    		if (wallLocation != null) {
    			targetWallLocation = wallLocation;
    			path.setTarget(targetWallLocation);
    			foundWallTarget = true;
    		}
    		
    		if (!foundWallTarget) {
    			path.setTarget(hqLocation);
    			targetWallLocation = null;
       			// If all 8 wall spots are confirmed occupied, then become a roamer.
    			if (wallComplete) {
        			mode = LandscaperMode.TERRAFORM;
        		}
    		}
		}
		// try to move towards the target location
		// DO NOT use onLattice move here...
		path.simpleTargetMovement();
	}

	// We are the watchers on the wall.
	static void onWall() throws GameActionException {
		
		MapLocation loc = rc.getLocation();		
		
		// EMERGENCY! Heal the HQ if it's about to die!
		RobotInfo hqInfo = rc.senseRobotAtLocation(hqLocation);
		if (hqInfo != null && rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit && hqInfo.getDirtCarrying() > 40) {
			rc.setIndicatorLine(loc, hqLocation, 255, 0, 0);
			tryDig(loc.directionTo(hqLocation));
		}
					
		// If carrying ANY dirt, deposit it onto ourselves
		if (rc.getDirtCarrying() > 0) {
			
			// Fuck up enemies first.
			RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
			for (RobotInfo robot : robots) {
				if (robot.getTeam() == rc.getTeam().opponent() && robot.getType().isBuilding() && robot.getLocation().isAdjacentTo(loc)) {
					rc.setIndicatorLine(loc, hqLocation, 0, 255, 0);
					tryDeposit(loc.directionTo(robot.getLocation()));
				}
			}
			
			// If it's before round 500 -- wall can still be built in conventional ways (people walking onto it). Only build self.
			if (rc.getRoundNum() < wallCutoffRound) {
				rc.setIndicatorDot(loc, 0, 255, 0);
				tryDeposit(Direction.CENTER);
			}
			// Otherwise, deposit the dirt on the lowest of all adjacent tiles that are still on the wall.
			// Then, if we can move onto 
			else {
				int lowestElevation = rc.senseElevation(loc);
				Direction depositDir = Direction.CENTER;

				for (Direction dir : directions) {
					MapLocation wallSpot = loc.add(dir);
					
					// If its not on the map, GTFO.
					if (!rc.onTheMap(wallSpot)) continue;
					// If it's not adjacent to HQ, gtfo.
					if (!wallSpot.isAdjacentTo(hqLocation)) continue;
					// If it IS HQ, gtfo.
					if (wallSpot.equals(hqLocation)) continue;
					
					RobotInfo robot = rc.senseRobotAtLocation(wallSpot);
					if (robot != null && robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.LANDSCAPER) continue;
					if (!rc.canSenseLocation(wallSpot)) continue;
					int elevation = rc.senseElevation(wallSpot);
					if (elevation < lowestElevation) {
						lowestElevation = elevation;
						depositDir = dir;
					}
				}
				
				// Move towards lower elevation.
				if (depositDir != Direction.CENTER) {
					path.tryMove(depositDir);
				}
				rc.setIndicatorLine(loc, loc.add(depositDir), 0, 255, 0);
				tryDeposit(depositDir);
			}
		}
		// No dirt, need to dig.
		else{
			// Heal the HQ if it's not at full health.
			if (hqInfo != null && hqInfo.getDirtCarrying() > 0) {
				rc.setIndicatorLine(loc, hqLocation, 255, 0, 0);
				tryDig(loc.directionTo(hqLocation));
			}
			
			Direction defaultDir = hqLocation.directionTo(loc);
			
			// Otherwise, first try to dig from the designated spot (as long as no building is there...)
			if (rc.onTheMap(loc.add(defaultDir))) {
				rc.setIndicatorLine(loc, loc.add(defaultDir), 255, 0, 0);
				tryDig(defaultDir);
			}
			// Otherwise, it must be an edge case. Find a non-lattice point near by and dig from it.
			else {
				// try to find a dig point.
				for (Direction dir : directions) {
					MapLocation potentialDigSpot = loc.add(dir);
					// System.out.println("Edge case. Trying spot: " + potentialDigSpot);
					// On the map and not HQ.
					if (rc.onTheMap(potentialDigSpot) && !potentialDigSpot.equals(hqLocation) && !potentialDigSpot.isAdjacentTo(hqLocation)) {
						rc.setIndicatorLine(loc, loc.add(dir), 255, 0, 255);
						if(tryDig(dir)) break;
					}
					// System.out.println("Nope");
				}
			}
		}
	}
	
	static MapLocation findTerraformTarget(boolean aroundHQ) throws GameActionException {
		MapLocation bestSpot = null;
		// minimize priority
		int bestPriority = 9999999;

		// Sense all locations we might want to fill into.
		for (int dx = -4; dx <= 4; dx++) {
			for (int dy = -4; dy <= 4; dy++) {
				MapLocation potentialFillSpot = rc.getLocation().translate(dx, dy);
				// only care about locations we can sense
				if (!rc.onTheMap(potentialFillSpot)) continue;
				if (!rc.canSenseLocation(potentialFillSpot)) continue;
				if (!onLatticeTiles(potentialFillSpot)) continue;
				if (path.isPastStuckTarget(potentialFillSpot)) continue;

				// Check if the elevation is below what we need.
				int elevation = rc.senseElevation(potentialFillSpot);
				if (elevation >= latticeElevation || elevation < -200) continue;
				
				// Check there's no buildings on there.
				RobotInfo robot = rc.senseRobotAtLocation(potentialFillSpot);
				if (robot != null && robot.getType().isBuilding() && robot.getTeam() == rc.getTeam()) continue;
				
				// Fill in the closest square to HQ that needs it.
				int priority = rc.getLocation().distanceSquaredTo(potentialFillSpot);
				
				// If we're filling around the HQ, then we want to find the closest points around HQ that we can sense to fill.
				if (aroundHQ) {
					priority = hqLocation.distanceSquaredTo(potentialFillSpot);
				}

				// Get the best priority.
				if (bestPriority > priority) {
					bestPriority = priority;
					bestSpot = potentialFillSpot;
				}
			}
		}
		return bestSpot;
	}
	
	
	static void terrformTowards() throws GameActionException {
	}

	// Terraform: build lattice while roaming
	static void terraform(boolean aroundHQ) throws GameActionException {

		MapLocation loc = rc.getLocation();
		
		// Not yet max dirt capacity.
		if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
			// Find a non-Lattice tile that is not the HQ and try to dig there.
			for (Direction dir : directions) {
				if (!hqLocation.equals(loc.add(dir)) && !onLatticeTiles(loc.add(dir))) {
					if (tryDig(dir)) break;
				}
			}
			// Else, move.
			path.simpleTargetMovementCWOnly(true);
		}
		// Has max dirt capacity - start depositing dirt.
		else {
			
			// Have to reset terraform target too if its null.
			if (path.isStuck()) {
				rc.setIndicatorDot(loc, 255, 255, 255);
				terraformTarget = null;
				path.resetTarget();
			}
			
			// Make sure we have a terraform target.
			if (terraformTarget == null) {
				terraformTarget = findTerraformTarget(aroundHQ);
			}
			

			if (terraformTarget != null) {
				rc.setIndicatorLine(rc.getLocation(), terraformTarget, 192, 192, 0);
				// Check if the target is done.
				if (rc.canSenseLocation(terraformTarget) &&
						((onLatticeIntersections(terraformTarget) && rc.senseElevation(terraformTarget) >= latticeElevation + 3)
						|| (!onLatticeIntersections(terraformTarget) && rc.senseElevation(terraformTarget) >= latticeElevation))) {
					terraformTarget = findTerraformTarget(aroundHQ);
				}
				navigateToDeposit(terraformTarget, true);
			}
			else {
				if (enemyHQLocation != null) {
					path.simpleTargetMovementCWOnly(enemyHQLocation);
				}
				else {
					path.simpleTargetMovementCWOnly();
				}
			}
		}
	}

	
	static public void rushDefense(MapLocation attackTarget) throws GameActionException {
		MapLocation loc = rc.getLocation();
		rc.setIndicatorDot(loc, 0,0,0);
		if (rc.canSenseLocation(hqLocation)) {
			RobotInfo hqInfo = rc.senseRobotAtLocation(hqLocation);
			if ( loc.isAdjacentTo(hqLocation) ) {
				if ( hqInfo.getDirtCarrying() > 0 && rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit ) {
					tryDig(loc.directionTo(hqLocation));
				} else if ( rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit ) {
					if (attackTarget != null && loc.isAdjacentTo(attackTarget)) {
						tryDeposit(loc.directionTo(attackTarget));
					} else {
						tryDeposit(Direction.CENTER);
					}
				} else { // no threat to hq
					if (attackTarget != null && loc.isAdjacentTo(attackTarget)) {
						attack(attackTarget);
					}
				}
			} else {
				if ( hqInfo != null && hqInfo.getDirtCarrying() > 30 ) {
					rc.setIndicatorLine(loc, hqLocation, 255, 0, 0);
					path.simpleTargetMovement(hqLocation);
				} else if ( attackTarget != null ){
					attack(attackTarget);
				} else {
					path.simpleTargetMovement(hqLocation);
				}
			}
		} else {
			if (attackTarget != null) {
				attack(attackTarget);
			} else {
				path.simpleTargetMovement(hqLocation);
			}
		}
	}
	
	// Move towards and kill that bitch?
	static public void attack(MapLocation attackTarget) throws GameActionException{
		MapLocation loc = rc.getLocation();
		rc.setIndicatorLine(loc, attackTarget, 255, 255, 0);
		
		RobotInfo attackTargetRobot = rc.senseRobotAtLocation(attackTarget);

		// Find the special case: HQ surrounded by wall.
		int distanceToTarget = loc.distanceSquaredTo(attackTarget);
		if (attackTargetRobot != null && attackTargetRobot.getType() == RobotType.HQ && 4 <= distanceToTarget && distanceToTarget <= 16) {
			Direction dirToEnemyHQ = loc.directionTo(attackTarget);
			MapLocation tileNextToEnemyHQ = loc.add(dirToEnemyHQ);
			
			// Brute force our wall through this shit.
			if (rc.canSenseLocation(tileNextToEnemyHQ) && rc.senseElevation(tileNextToEnemyHQ) > rc.senseElevation(loc) + 3) {
				if (rc.getDirtCarrying() == 0) {
					tryDig(dirToEnemyHQ);
				}
				else {
					tryDeposit(Direction.CENTER);
				}
			}
		}

		if (rc.getDirtCarrying() == 0) {
			// Find a non-Lattice tile that is not the HQ and try to dig there.
			for (Direction dir : directions) {
				if (!attackTarget.equals(loc.add(dir))
						&& !hqLocation.equals(loc.add(dir))
						&& !onLatticeTiles(loc.add(dir))) {
					if (tryDig(dir)) {
						rc.setIndicatorLine(loc, loc.add(dir), 0, 128, 0);
						break;
					}
				}
			}
		}
		else {
			navigateToDeposit(attackTarget, false);
		}
	}
	
	static public void rush() {
		
	}
	
	static public void navigateToDeposit(MapLocation dest, boolean latticeOnly) throws GameActionException {
		if (dest != null && rc.getLocation().isAdjacentTo(dest)) {
			// If adjacent, deposit should not fail unless not available for move.
			tryDeposit(rc.getLocation().directionTo(dest));
		}
		else {
			path.simpleTargetMovementCWOnly(dest, latticeOnly);
		}
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
