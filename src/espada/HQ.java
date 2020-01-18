package espada;

import battlecode.common.*;

public class HQ extends Unit {
	
    static int buildOrderIndex;

    /**
     * Hardcoded initial build order.
     *
     **/
    static RobotType[] buildOrderUnits = {
        RobotType.MINER,
        RobotType.MINER,
        RobotType.MINER,
        RobotType.MINER
    };
    
    
	public HQ(RobotController rc) throws GameActionException{
		super(rc);
		
    	// try to find soup.
    	hqLocation = rc.getLocation();
    	
    	// Broadcast our current location
    	txn.sendHQLocationMessage(hqLocation, 15);
	}
	
	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
        // build stuff from order index while it exists
        if (buildOrderIndex < buildOrderUnits.length && buildOrderUnits[buildOrderIndex].cost < rc.getTeamSoup()) {
            for (Direction dir : directions) {
                boolean success = tryBuild(buildOrderUnits[buildOrderIndex], dir);
                if (success) {
                    buildOrderIndex++;
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
    	
        // Otherwise, start trying to build miners if we have too much soup.
        if (rc.getTeamSoup() > 300 && Math.random() < 0.1) {
        	for (Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) break;
            }
        }
	}

}
