package dumbrushbot;

import battlecode.common.*;


public class Miner extends Unit {

	enum MinerMode {
		PRIMARY_BUILDER,
		SOUP_COLLECTING,
	};

	static MinerMode mode = MinerMode.SOUP_COLLECTING;

	static int buildingOrderIndex;

	static RobotType[] buildingOrder = {
			RobotType.VAPORATOR,
			RobotType.DESIGN_SCHOOL,
			RobotType.REFINERY,
			RobotType.FULFILLMENT_CENTER,
			RobotType.VAPORATOR,
	};


	static int designSchoolsBuilt = 0;
	static int fulfillmentCentersBuilt = 0;
	static int vaporatorsBuilt = 0;
	static int refineriesBuilt = 0;
	static int netGunsBuilt = 0;
	static int turnsInCurrentTile = 0;
	static int distFromTarget = 0;
	static int turnsSinceLastProgress = 0;
	static int hqSeachPhase = -1;

	static MapLocation lastSoupTile;
	static MapLocation lastRefineryLocation;
	static MapLocation depositLocation;
	static MapLocation currentTile;
	static MapLocation targetLoc;

	public Miner(RobotController _rc) throws GameActionException {
		super(_rc);
		if (rc.getRoundNum() <= 2) {
			mode = MinerMode.PRIMARY_BUILDER;
			buildingOrderIndex = 0;
		}
	}

	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();
		senseNearbyRefineries();
		switch (mode) {
			case PRIMARY_BUILDER:
				primaryBuilder();
				break;
			case SOUP_COLLECTING:
				macroBuilding();
				soupCollecting();
				break;
		}
	}

	static MapLocation findSoup() throws GameActionException {
		MapLocation loc = rc.getLocation();

		// If we can see the lastMinedSoupTile and it no longer has any soup, we can reset it.
		if (lastSoupTile != null && rc.canSenseLocation(lastSoupTile)) {
			if (rc.senseSoup(lastSoupTile) == 0) lastSoupTile = null;
			else {
				return lastSoupTile;
			}
		}

		// By closest, not by amount.
		int bestDistanceSquared = 999999999;
		MapLocation soupLoc = null;

		// Search for soup locally.
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
			lastSoupTile = soupLoc;
			return soupLoc;
		}

		return lastSoupTile;
	}

	static void incrementBuildCounters(RobotType robotType) throws GameActionException {
		switch (robotType) {
			case DESIGN_SCHOOL: 		designSchoolsBuilt++; 		break;
			case VAPORATOR:				vaporatorsBuilt++;    		break;
			case FULFILLMENT_CENTER:	fulfillmentCentersBuilt++;	break;
			case NET_GUN:				netGunsBuilt++;				break;
			case REFINERY:				refineriesBuilt++;			break;
			default: break;
		}
	}

	/**
	 * Intelligently builds a building to be on the lattice, and not adjacent to HQ.
	 * @param robotType
	 * @return true if successfully builds, else false
	 * @throws GameActionException
	 */
	static MapLocation smartBuild(RobotType robotType) throws GameActionException{
		return smartBuild(robotType, false);
	}


	/**
	 * Intelligently builds a building to be on the lattice, and not adjacent to HQ.
	 * @param robotType
	 * @return true if successfully builds, else false
	 * @throws GameActionException
	 */
	static MapLocation smartBuild(RobotType robotType, boolean desperate) throws GameActionException{
		for (Direction dir : directions) {

			// Do not build anything adjacent to our HQ.
			MapLocation potentialBuildLocation = rc.getLocation().add(dir);
			if (potentialBuildLocation.isAdjacentTo(hqLocation) || !onLatticeIntersections(potentialBuildLocation)) {
				continue;
			}
			if (tryBuild(robotType, dir)) {
				incrementBuildCounters(robotType);
				return potentialBuildLocation;
			}
		}

		if (desperate) {
			for (Direction dir : directions) {
				MapLocation potentialBuildLocation = rc.getLocation().add(dir);
				if (potentialBuildLocation.isAdjacentTo(hqLocation) || !onLatticeTiles(potentialBuildLocation)) {
					continue;
				}
				if (tryBuild(robotType, dir)) {
					incrementBuildCounters(robotType);
					return potentialBuildLocation;
				}
			}
		}
		return null;
	}


	/**
	 * Intelligently builds a building to be on the lattice, and not adjacent to HQ.
	 * @param robotType
	 * @return true if successfully builds, else false
	 * @throws GameActionException
	 */
	static MapLocation aggroBuild(RobotType robotType) throws GameActionException{
		for (Direction dir : directions) {
			// Do not build anything adjacent to our HQ.
			MapLocation potentialBuildLocation = rc.getLocation().add(dir);
			if (!potentialBuildLocation.isAdjacentTo(enemyHQLocation)) {
				continue;
			}
			if (tryBuild(robotType, dir)) {
				incrementBuildCounters(robotType);
				return potentialBuildLocation;
			}
		}
		return null;
	}

	static boolean randomWalkOnLattice(int tries) throws GameActionException {
		// Randomly move on lattice.
		Direction dir = Direction.CENTER;
		while (tries > 0) {
			dir = randomDirection();
			if (onLatticeTiles(rc.getLocation().add(dir)) && path.tryMove(dir)) {
				return true;
			}
			tries--;
		}
		return false;
	}


	static boolean searchForHQ() throws GameActionException {
		if (hqSeachPhase == 0) {
			targetLoc = new MapLocation(rc.getMapWidth() - Unit.hqLocation.x, Unit.hqLocation.y);
		} else if (hqSeachPhase == 1) {
			targetLoc = new MapLocation(rc.getMapWidth() - Unit.hqLocation.x, rc.getMapHeight() - Unit.hqLocation.y);
		} else {
			targetLoc = new MapLocation( Unit.hqLocation.x , rc.getMapHeight() - Unit.hqLocation.y);
		}

		return false;
	}

	static int lateFullfillmentCenter = 0;
	static int lateDesignSchool = 0;
	static void primaryBuilder() throws GameActionException {
		rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);
		MapLocation loc = rc.getLocation();

		if (hqSeachPhase < 0) {
			hqSeachPhase = 0;
			searchForHQ();
		}

		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		// Look at all the enemy robots.
		RobotInfo closestEnemyRobot = null;
		int closestDistance = 999999999;
		for (RobotInfo robot : robots) {
			// If found enemy HQ...
			if (robot.getType() == RobotType.HQ && Unit.enemyHQLocation == null) {
				Unit.enemyHQLocation = robot.getLocation();
				Unit.txn.sendLocationMessage(robot, Unit.enemyHQLocation, Math.min(10, rc.getTeamSoup()));
			}

			// Get those landscapers (and maybe Miners).
			if (robot.getLocation().distanceSquaredTo(loc) < closestDistance) {
				closestDistance = robot.getLocation().distanceSquaredTo(loc);
				closestEnemyRobot = robot;
			}
		}

		// Handle Rushing by building a fucking fast
		if (Unit.enemyHQLocation != null) {
			// Build a design school asap.
			if (designSchoolsBuilt < 1 && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
				aggroBuild(RobotType.DESIGN_SCHOOL);
			}
			if(netGunsBuilt < 1 && rc.getTeamSoup() >= RobotType.NET_GUN.cost) {
				aggroBuild(RobotType.NET_GUN);
			}

			path.simpleTargetMovement(enemyHQLocation);
		} else {
			if (rc.canSenseLocation(targetLoc)) {
				hqSeachPhase += 1;
				searchForHQ();
			}

			path.simpleTargetMovement(targetLoc);
		}
		// Search for soup locally.
//		for (int dx = -6; dx <= 6; dx++) {
//			for (int dy = -6; dy <= 6; dy++) {
//				if (!rc.canSenseLocation(loc.translate(dx, dy))) continue;
//				int soupAmount = rc.senseSoup(loc.translate(dx, dy));
//			}
//		}

//        if (rc.getRoundNum() > 500 && rc.getTeamSoup() >= RobotType.VAPORATOR.cost && Math.random() > 0.5) {
//			smartBuild(RobotType.VAPORATOR);
//		}

		// Randomly move but prefer lattice tiles.
//        if (!randomWalkOnLattice(50)) {
//        	path.tryMove(randomDirection());
//        }

	}

	static void macroBuilding() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(-1);
		boolean enemyDrone = false;
		boolean friendlyNetGun = false;
		for (RobotInfo robot : robots) {
			// Is enemy drone
			if (robot.getType() == RobotType.DELIVERY_DRONE && robot.getTeam() == rc.getTeam().opponent()) enemyDrone = true;
			if (robot.getType() == RobotType.NET_GUN && robot.getTeam() == rc.getTeam()) friendlyNetGun = true;
		}

		if (enemyDrone && !friendlyNetGun) {
			smartBuild(RobotType.NET_GUN, true);
		}
		if (rc.getRoundNum() > 600 && rc.getTeamSoup() > 1000 && Math.random() < 0.3) {
			smartBuild(RobotType.VAPORATOR, true);
		}
	}

	static void senseNearbyRefineries() throws GameActionException {
		// Sense Nearby Refineries
		RobotInfo[] robots = rc.senseNearbyRobots();
		for (RobotInfo robot : robots) {
			if (robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.REFINERY) {
				lastRefineryLocation = robot.getLocation();
			}
		}
	}

	static void soupCollecting() throws GameActionException {
		if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
			minerMineSoup();
		}
		else if (rc.getSoupCarrying() > 60) {
			minerDepositSoup();
		}
		else {
			path.simpleTargetMovement();
		}
	}

	static void minerMineSoup() throws GameActionException {

		MapLocation loc = rc.getLocation();
		MapLocation soupLoc = findSoup();
		// found some soup!
		if (soupLoc != null) {
			rc.setIndicatorDot(soupLoc, 0, 255, 0);
			// If we are next to the soup, we will mine it.
			if (soupLoc.isAdjacentTo(loc)) {
				tryMine(loc.directionTo(soupLoc));
			}
			// If we are not next to the soup, we will move towards it.
			else {
				path.simpleTargetMovement(soupLoc);
			}
		}
		else {
			// Explore the world for more soup.
			System.out.println("Exploring the world for more soup.");
			path.simpleTargetMovement();
		}
	}

	static void minerDepositSoup() throws GameActionException {
		MapLocation loc = rc.getLocation();

		// Update target deposit location

		// If a refinery was seen and isn't too far away.
		if (lastRefineryLocation != null && (rc.getTeamSoup() < RobotType.REFINERY.cost || loc.distanceSquaredTo(lastRefineryLocation) < 70)) {
			depositLocation = lastRefineryLocation;
		}
		// If HQ isn't too far away.
		else if (hqLocation != null && (rc.getTeamSoup() < RobotType.REFINERY.cost || loc.distanceSquaredTo(hqLocation) < 30)) {
			depositLocation = hqLocation;
		}
		else if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {

			System.out.println("Trying to build refinery!");
			MapLocation built = smartBuild(RobotType.REFINERY);
			if (built != null) {
				lastRefineryLocation = built;
				depositLocation = built;
			}
			else {
				System.out.println("Failed???");
			}
		}

		if (depositLocation != null) {
			// If we are next to the deposit location, deposit (obviously).
			if (loc.isAdjacentTo(depositLocation)) {
				tryRefine(loc.directionTo(depositLocation));
				path.resetTarget();
			}
			else {
				path.simpleTargetMovement(depositLocation);
				rc.setIndicatorLine(rc.getLocation(), depositLocation, 0, 0, 255);
			}
		}
		else {
			path.simpleTargetMovement();
		}
	}

	/**
	 * Attempts to mine soup in a given direction.
	 *
	 * @param dir The intended direction of mining
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMine(Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canMineSoup(dir)) {
			rc.mineSoup(dir);
			return true;
		} else return false;
	}

	/**
	 * Attempts to refine soup in a given direction.
	 *
	 * @param dir The intended direction of refining
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryRefine(Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canDepositSoup(dir)) {
			rc.depositSoup(dir, rc.getSoupCarrying());
			return true;
		} else return false;
	}
}
