package antirushbot;

import battlecode.common.*;

public class DesignSchool extends Unit {

	static int landscapersBuilt = 0;
	
	public DesignSchool(RobotController _rc) {
		super(_rc);
	}

	public void rushDefense() throws GameActionException {
		int enemyLandscaperCount = 0;
		int allyLandscaperCount = 0;
		RobotInfo[] robots = rc.senseNearbyRobots(-1);
		Team myTeam = rc.getTeam();
		for (RobotInfo robot : robots) {
			// If found enemy HQ...
			if (robot.getType() == RobotType.LANDSCAPER) {
				if (robot.getTeam() != myTeam) {
					++enemyLandscaperCount;
				} else {
					++allyLandscaperCount;
				}
			}
		}
		int minDist = 10000;
		Direction dirToHQ = Direction.NORTH;
		for (Direction dir : directions) {
			int distToEnemyHQ = rc.getLocation().add(dir).distanceSquaredTo(hqLocation);
			if (rc.canBuildRobot(RobotType.LANDSCAPER, dir) && distToEnemyHQ <= minDist) {
				minDist = distToEnemyHQ;
				dirToHQ = dir;
			}
		}
		if (minDist < 10000 && tryBuild(RobotType.LANDSCAPER, dirToHQ)) {
			landscapersBuilt++;
		}
	}

	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		if (beingRushed) {
//			for (Direction dir : directions) {
//				if (tryBuild(RobotType.LANDSCAPER, dir)) {
//					landscapersBuilt++;
//					break;
//				}
//			}
			rushDefense();
		}

		if ((landscapersBuilt < 8 && rc.getTeamSoup() > 250 && Math.random() < 0.5) ||
			(landscapersBuilt < 15 && rc.getTeamSoup() > 300 && Math.random() < 0.2) ||
			(landscapersBuilt < 30 && rc.getTeamSoup() > 600 && Math.random() < 0.1)) {
    		for (Direction dir : directions) {
    			if (tryBuild(RobotType.LANDSCAPER, dir)) {
    				landscapersBuilt++;
    				break;
    			}
    		}
		}
		
	}
}
