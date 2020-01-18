package espada;

import battlecode.common.*;

public class Landscaper extends Unit {

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
 
	public Landscaper(RobotController _rc) throws GameActionException {
		super(_rc);
    	findHQ();
    	landscaperMode = LandscaperMode.FIND_WALL;
	}
	
	@Override
	public void run () throws GameActionException {
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

}
