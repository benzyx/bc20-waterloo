package espada;

import battlecode.common.*;

public class HQ extends Unit {
	
    static int minersProduced = 0;

    static final int initialMinersCount = 6;
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
	
	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
        // Otherwise, start trying to build miners if we have too much soup.
        if (minersProduced < initialMinersCount || minersProduced >= totalMinersNeeded && rc.getTeamSoup() > 300 && Math.random() < 0.1) {
        	for (Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) {
                	minersProduced++;
                	break;
                }
            }
        }
        
        // Shoot down enemies that appear.
        RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		if (robot.getTeam() != rc.getTeam() && rc.canShootUnit(robot.getID())) {
    			rc.shootUnit(robot.getID());
    		}
    	}
    	

	}

}
