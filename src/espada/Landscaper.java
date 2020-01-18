package espada;

import battlecode.common.*;

public class Landscaper extends Unit {

	static final int latticeElevation = 7;

    enum LandscaperMode{
    	FIND_WALL,
    	ON_WALL,
    	ROAM,
    	ATTACK,
    	EARLY_RUSH,
    };
    
    static LandscaperMode mode = LandscaperMode.FIND_WALL;

    static MapLocation targetWallLocation = null;
    static MapLocation spawnLocation = null;
    static boolean allWallFilled = false;
 
    
    static MapLocation terraformTarget = null;
    
	public Landscaper(RobotController _rc) throws GameActionException {
		super(_rc);
    	mode = LandscaperMode.FIND_WALL;
    	spawnLocation = rc.getLocation();

    	
//    	if (rc.getRoundNum() < 100) {
//    		mode = LandscaperMode.EARLY_RUSH;
//    	}
	}
	
	@Override
	public void run() throws GameActionException {    
    	txn.updateToLatestBlock();
    	
    	switch (mode) {
    	case FIND_WALL:
    		findWall();
    		break;
    	case ON_WALL:
    		onWall();
    		break;
		case ATTACK:
			attackNearbyEnemyBuildings();
			break;
		case ROAM:
			terraform();
			break;
		case EARLY_RUSH:
			rush();
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
			}
			// On Map and can Sense
			else if (rc.canSenseLocation(wallLocation)) {
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
			allWallFilled = true;
		}
		return bestLocation;
	}
	
	/**
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
    			if (allWallFilled) {
        			mode = LandscaperMode.ROAM;
        		}
    		}
		}
		// try to move towards the target location
		path.simpleTargetMovement();
	}

	// We are the watchers on the wall.
	static void onWall() throws GameActionException {
		
		MapLocation loc = rc.getLocation();
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
	}
	
	static MapLocation findTerraformTarget() throws GameActionException {
		MapLocation bestSpot = null;
		// minimize priority
		int bestPriority = 9999999;

		// Sense all locations we might want to fill into.
		for (int dx = -5; dx <= 5; dx++) {
			for (int dy = -5; dy <= 5; dy++) {
				MapLocation potentialFillSpot = rc.getLocation().translate(dx, dy);
				// only care about locations we can sense
				if (!rc.canSenseLocation(potentialFillSpot)) continue;
				if (!onLatticeTiles(potentialFillSpot)) continue;

				// Check if the elevation is below what we need.
				int elevation = rc.senseElevation(potentialFillSpot);
				if (elevation >= latticeElevation || elevation < -200) continue;
				
				// Check there's no buildings on there.
				RobotInfo robot = rc.senseRobotAtLocation(potentialFillSpot);
				if (robot != null && robot.getType().isBuilding() && robot.getTeam() == rc.getTeam()) continue;
				
				// Fill in the closest square to HQ that needs it.
				int priority = hqLocation.distanceSquaredTo(potentialFillSpot);
				
				// Actually fill in the closest one to us, tiebroken by distance to HQ.
				if (bestPriority > priority) {
					bestPriority = priority;
					bestSpot = potentialFillSpot;
				}
			}
		}
		return bestSpot;
	}
	
	// Terraform: build lattice while roaming
	static void terraform() throws GameActionException {
		System.out.println("Terraforming!");

		// detect enemies
		Team enemy = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, enemy);
        for (RobotInfo enemyRobot : enemies) {
        	// If it's a building/
        	if (enemyRobot.getType().isBuilding()) {
        		mode = LandscaperMode.ATTACK;
        		path.simpleTargetMovement(enemyRobot.getLocation());
        		return;
        	}
        }
		MapLocation loc = rc.getLocation();
		
		// Not yet max dirt capacity.
		if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
			// Find a non-Lattice tile that is not the HQ and try to dig there.
			for (Direction dir : directions) {
				if (!hqLocation.equals(loc.add(dir)) && !onLatticeTiles(loc.add(dir))) {
					if (tryDig(dir)) break;
				}
			}
		}
		// Has max dirt capacity -- terraform.
		else {
			if (terraformTarget == null) {
				terraformTarget = findTerraformTarget();
			}
			if (terraformTarget != null) {
				// Check if the target is done.
				if (rc.canSenseLocation(terraformTarget) && rc.senseElevation(terraformTarget) >= latticeElevation) {
					terraformTarget = findTerraformTarget();
				}
				rc.setIndicatorLine(rc.getLocation(), terraformTarget, 192, 192, 0);
				navigateToDeposit(terraformTarget);
				
				
			}
		}
	}
	
	static public void attackNearbyEnemyBuildings() {
		
	}
	
	static public void rush() {
		
	}
	
	static public void navigateToDeposit(MapLocation dest) throws GameActionException {
		if (rc.getLocation().isAdjacentTo(dest)) {
			// If adjacent, deposit should not fail unless not available for move.
			tryDeposit(rc.getLocation().directionTo(dest));
		}
		else {
			path.simpleTargetMovement(dest);
		}
	}

}
