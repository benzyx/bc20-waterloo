package escudo;

import battlecode.common.*;

public class NetGun extends Unit {

	public NetGun(RobotController _rc) {
		super(_rc);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void run() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    	for (RobotInfo robot : robots) {
    		if (rc.canShootUnit(robot.getID())) {
    			rc.shootUnit(robot.getID());
    		}
    	}
	}

}
