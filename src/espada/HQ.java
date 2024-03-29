package espada;

import battlecode.common.*;

public class HQ extends Unit {
	
    static int minersProduced = 0;

    static final int initialMinersCount = 3;
    static final int totalMinersNeeded = 20;
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
    	txn.sendLocationMessage(rc.senseRobotAtLocation(hqLocation), hqLocation, 15);
	}
	
	public void senseEnemies() {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		for (RobotInfo robot : robots) {
			if (robot.getType() == RobotType.LANDSCAPER) {
				
			}
		}
	}

	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		// Produce Miners
        // Otherwise, start trying to build miners if we have too much soup.
        if (minersProduced < initialMinersCount || minersProduced < totalMinersNeeded && rc.getTeamSoup() > 300 && Math.random() < 0.1) {
        	for (Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) {
                	minersProduced++;
                	break;
                }
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
    				txn.sendRushDetectedMessage(robot, 15);
    			}
    		}
    	}
	}

}
