package escudo;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class NetGun extends Unit {

	public NetGun(RobotController _rc) {
		super(_rc);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void run() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		if (robot.getTeam() != rc.getTeam() && rc.canShootUnit(robot.getID())) {
    			rc.shootUnit(robot.getID());
    		}
    	}
	}

}
