package escudo;

import battlecode.common.*;

public class HQ extends Unit {
	
    static int minersProduced = 0;

    static final int initialMinersCount = 3;
    static final int totalMinersNeeded = 15;
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

	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		MapLocation loc = rc.getLocation();

		if (rc.getRoundNum() > 400) beingRushed = false;

		// Produce Miners
        // Otherwise, start trying to build miners if we have too much soup.
        if (minersProduced < initialMinersCount ||
        	!beingRushed && minersProduced < 7 && rc.getTeamSoup() > 70 ||
        	!beingRushed && minersProduced < totalMinersNeeded && rc.getTeamSoup() > 400 && Math.random() < 0.1 ||
        	rc.getRoundNum() > 450 && rc.getTeamSoup() > 600) {
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
            	txn.sendSoupLocationMessage(soupLoc, 1);
            }
        }
        
        boolean tempWallComplete = true;
        for (Direction dir : directions) {
        	if (!rc.onTheMap(loc.add(dir))) continue;
        	RobotInfo robot = rc.senseRobotAtLocation(loc.add(dir));
        	if (robot == null || robot.getTeam() != rc.getTeam() || robot.getType() != RobotType.LANDSCAPER) tempWallComplete = false;	
        }
        if (!wallComplete && tempWallComplete) {
        	wallComplete = true;
        	txn.sendWallCompleteMessage(2);
        }

        RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		
    		// Shoot down enemies that appear.
    		if (robot.getTeam() != rc.getTeam() && rc.canShootUnit(robot.getID())) {
    			rc.shootUnit(robot.getID());
    		}
    		
    		// Detect rushes.
    		if (!beingRushed && robot.getTeam() == rc.getTeam().opponent()) {
    			if (rc.getRoundNum() < 300 && robot.getType() == RobotType.MINER || robot.getType() == RobotType.LANDSCAPER || robot.getType().isBuilding()) {
    				if (rc.getSoupCarrying() > 0) {
    					txn.sendRushDetectedMessage(robot, Math.min(11, rc.getSoupCarrying()));
    					beingRushed = true;
    				}
    			}
    		}
    	}
	}

}
