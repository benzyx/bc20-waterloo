package escudo;

import battlecode.common.*;

public class DesignSchool extends Unit {

	static int landscapersBuilt = 0;
	
	public DesignSchool(RobotController _rc) throws GameActionException {
		super(_rc);
		if (rc.getTeamSoup() > 0) txn.sendSpawnMessage(RobotType.DESIGN_SCHOOL, Math.min(rc.getTeamSoup(), 2));
	}
	
	
	public void smartBuild() throws GameActionException {
		MapLocation loc = rc.getLocation();

		for (Direction dir : directions) {
			if (onLatticeTiles(loc.add(dir)) && tryBuild(RobotType.LANDSCAPER, dir)) {
				landscapersBuilt++;
				break;
			}
		}
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
		if (minDist < 10000 && allyLandscaperCount < 4 && tryBuild(RobotType.LANDSCAPER, dirToHQ)) {
			landscapersBuilt++;
		} else if (minDist < 10000 && Math.random() < 0.2 && tryBuild(RobotType.LANDSCAPER, dirToHQ)) {
			landscapersBuilt++;
		}
	}
	
	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		
		if (rc.getRoundNum() > 500) beingRushed = false;
		
		
		// Being rushed!
		if (beingRushed) {
//			if (rc.getRoundNum() < 500 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost && Math.random() < 0.5) {
//				smartBuild();
//			}
			rushDefense();
		}
		
		// Not being rushed!
		else {
			if ((landscapersSpawned < 4 && rc.getTeamSoup() > 255 && Math.random() < 0.5) ||
				(rc.getTeamSoup() > 800) ||
			    (rc.getTeamSoup() > 300 && rc.getTeamSoup() > RobotType.LANDSCAPER.cost && landscapersSpawned < dronesSpawned * 2)) {
				smartBuild();
			}
		}	
	}
}
