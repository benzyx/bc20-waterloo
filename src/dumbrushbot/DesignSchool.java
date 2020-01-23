package dumbrushbot;

import battlecode.common.*;

public class DesignSchool extends Unit {

	static int landscapersBuilt = 0;
	
	public DesignSchool(RobotController _rc) {
		super(_rc);
	}
	
	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		if (beingRushed && rc.getRoundNum() < 500 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost && Math.random() < 0.5) {
			for (Direction dir : directions) {
    			if (tryBuild(RobotType.LANDSCAPER, dir)) {
    				landscapersBuilt++;
    				break;
    			}
    		}
		}
		RobotInfo[] robots = rc.senseNearbyRobots(-1);
		if (enemyHQLocation != null && rc.canSenseLocation(enemyHQLocation)) {
			int localNetGuns = 0;
			int localNetGunCount = 0;
			int localEnemyDroneCount = 0;
			Team myTeam = rc.getTeam();
			for (RobotInfo robot : robots) {
				// If found enemy HQ...
				if (robot.getTeam() != myTeam) {
					if (robot.getType() == RobotType.NET_GUN) {
						++localEnemyDroneCount;
					}
				} else {
					if (robot.getType() == RobotType.NET_GUN) {
						++localNetGunCount;
					}
				}
			}
			if (localNetGunCount == 0 && localEnemyDroneCount > 0) {
				return;
			}
			if ((landscapersBuilt < 1 && rc.getTeamSoup() > RobotType.LANDSCAPER.cost) ||
					(localNetGunCount > 0 && rc.getTeamSoup() > RobotType.LANDSCAPER.cost)) {
				int minDist = 10000;
				Direction dirToEnemyHQ = Direction.NORTH;
				for (Direction dir : directions) {
					int distToEnemyHQ = rc.getLocation().add(dir).distanceSquaredTo(Unit.enemyHQLocation);
					if (rc.canBuildRobot(RobotType.LANDSCAPER, dir) && distToEnemyHQ <= minDist) {
						minDist = distToEnemyHQ;
						dirToEnemyHQ = dir;
					}
				}
				if (minDist < 10000 && tryBuild(RobotType.LANDSCAPER, dirToEnemyHQ)) {
					landscapersBuilt++;
				}
			}
		} else {
			rc.setIndicatorDot(rc.getLocation(), 0,0,0);
			if ((landscapersBuilt < 8 && rc.getTeamSoup() > 250) ||
					(landscapersBuilt < 15 && rc.getTeamSoup() > 300 && Math.random() < 0.5) ||
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
}
