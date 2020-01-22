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
    	txn.sendLocationMessage(rc.senseRobotAtLocation(hqLocation), hqLocation, 11);
	}

	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		MapLocation loc = rc.getLocation();

		// Produce Miners
        // Otherwise, start trying to build miners if we have too much soup.
        if (minersProduced < initialMinersCount ||
        	!beingRushed && minersProduced < 7 && rc.getTeamSoup() > 100 ||
        	!beingRushed && minersProduced < totalMinersNeeded && rc.getTeamSoup() > 300 && Math.random() < 0.5 ||
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

        RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		
    		// Shoot down enemies that appear.
    		if (robot.getTeam() != rc.getTeam() && rc.canShootUnit(robot.getID())) {
    			rc.shootUnit(robot.getID());
    		}
    		
    		// Detect rushes.
    		if (!beingRushed && robot.getTeam() == rc.getTeam().opponent()) {
    			if (rc.getRoundNum() < 300 && robot.getType() == RobotType.MINER || robot.getType() == RobotType.LANDSCAPER || robot.getType().isBuilding()) {
    				beingRushed = true;
    				txn.sendRushDetectedMessage(robot, 11);
    			}
    		}
    	}
	}

}
