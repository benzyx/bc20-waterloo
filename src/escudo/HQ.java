package escudo;

import battlecode.common.*;

public class HQ extends Unit {
	
    static int minersProduced = 0;

    static final int initialMinersCount = 3;
    static final int totalMinersNeeded = 18;
    /**
     * Hardcoded initial miner count
     * @param rc
     * @throws GameActionException
     */
	public HQ(RobotController rc) throws GameActionException{
		super(rc);
		
    	// try to find soup.
    	hqLocation = rc.getLocation();
    	
    	// Broadcast our current location
    	if (rc.getTeamSoup() > 0) txn.sendLocationMessage(rc.senseRobotAtLocation(hqLocation), hqLocation, Math.min(11, rc.getTeamSoup()));
	}

	public void rushDefense() throws GameActionException{
		RobotInfo[] robots = rc.senseNearbyRobots();
		int enemyRobotCount = 0;
		Team ourTeam = rc.getTeam();
		for (RobotInfo robot : robots) {
			if (robot.getTeam() != ourTeam) {
				++enemyRobotCount;
				// Shoot down enemies that appear.
				if (rc.canShootUnit(robot.getID())) {
					rc.shootUnit(robot.getID());
				}
			}
		}

		// Detect rushes.
		if (enemyRobotCount == 0 || rc.getRoundNum() > 300) {
			beingRushed = false;
			int bid = Math.min(11, rc.getTeamSoup());
			if (bid > 0) {
				txn.sendRushDefeatedMessage(rc.senseRobotAtLocation(rc.getLocation()), bid);
			}
		}
	}

	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		MapLocation loc = rc.getLocation();

		if (rc.getRoundNum() > 350) beingRushed = false;

		if ( beingRushed ) {
			rushDefense();
			return;
		}

		// Produce Miners
        // Otherwise, start trying to build miners if we have too much soup.
        if (minersProduced < initialMinersCount ||
        	!beingRushed && minersProduced < 7 && rc.getTeamSoup() > 70 ||
        	!beingRushed && minersProduced < totalMinersNeeded && rc.getTeamSoup() > 400 && Math.random() < 0.1 ||
        	rc.getTeamSoup() > 630) {
        	for (Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) {
                	minersProduced++;
                	break;
                }
            }
        }
        
        if (rc.getRoundNum() == 1) {
        	// scan nearby for soup to broadcast
        	int bestDistanceSquared = 999999999;
    		MapLocation soupLoc = null;

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
            	if (rc.getSoupCarrying() > 0) txn.sendSoupLocationMessage(soupLoc, 1);
            }
        }
        
        boolean tempWallComplete = true;
        for (Direction dir : directions) {
        	if (!rc.onTheMap(loc.add(dir))) continue;
        	RobotInfo robot = rc.senseRobotAtLocation(loc.add(dir));
        	if (robot == null || robot.getTeam() != rc.getTeam() || robot.getType() != RobotType.LANDSCAPER) tempWallComplete = false;	
        }
        if (!wallComplete && tempWallComplete) {
        	if (rc.getTeamSoup() > 0) {
        		txn.sendWallCompleteMessage(Math.min(rc.getTeamSoup(), 2));
        		wallComplete = true;
        	}
        }

        RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		
    		// Shoot down enemies that appear.
    		if (robot.getTeam() != rc.getTeam() && rc.canShootUnit(robot.getID())) {
    			rc.shootUnit(robot.getID());
    		}
    		
    		// Detect rushes.
    		if (!beingRushed && robot.getTeam() == rc.getTeam().opponent()) {
    			if (rc.getRoundNum() < 300 && (robot.getType() == RobotType.MINER || robot.getType() == RobotType.LANDSCAPER || robot.getType().isBuilding())) {
    				int bid = Math.min(11, rc.getTeamSoup());
    				if (bid > 0) {
    					txn.sendRushDetectedMessage(robot, bid);
    					beingRushed = true;
    				}
    			}
    		}
    	}
	}

}
