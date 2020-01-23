package dumbrushbot;

import battlecode.common.*;

/**
 * Class that handles reading and sending transactions to the blockchain.
 * @author ben
 *
 */
class TransactionLogic {
	
	static RobotController rc;
	
	public enum MessageType {
		LOCATION,
		UNIT_SPAWNED,
		WALL_STATUS,
		WALL_COMPLETE,
		RUSH_DETECTED,
		SOUP_LOCATION,
	}

	static int lastBlockRead = 0;
	
	public TransactionLogic(RobotController _rc) {
		rc = _rc;
	}

	static int secretKey = 0x31415121;
    
    static int robotTypeToNum(RobotType type) {
    	switch (type) {
    		case HQ:						return 0;
    		case MINER:						return 1;
    		case REFINERY: 					return 2;
    		case VAPORATOR: 				return 3;
    		case DESIGN_SCHOOL: 			return 4;
    		case FULFILLMENT_CENTER: 		return 5;
            case LANDSCAPER:         		return 6;
            case DELIVERY_DRONE:     		return 7;
            case NET_GUN:            		return 8;
			case COW:                       return 9;
			default:
				break;
    	}
    	return -1;
    }
    
    static RobotType numToRobotType(int num) {
    	switch (num) {
    		case 0:						return RobotType.HQ;
    		case 1:						return RobotType.MINER;
    		case 2: 					return RobotType.REFINERY;
    		case 3: 					return RobotType.VAPORATOR;
    		case 4: 					return RobotType.DESIGN_SCHOOL;
    		case 5: 					return RobotType.FULFILLMENT_CENTER;
            case 6:         			return RobotType.LANDSCAPER;
            case 7:     				return RobotType.DELIVERY_DRONE;
            case 8:            			return RobotType.NET_GUN;
			case 9:                     return RobotType.COW;
			default:
				break;
    	}
    	return RobotType.COW;
    }
    
    static int[] applyXORtoMessage(int[] message, int cost, int roundNum) {
    	int signature = (cost << 16) | roundNum;
    	for (int i = 0; i < message.length; i++) {
    		message[i] = message[i] ^ secretKey ^ signature;
    	}
    	return message;
    }
    
    
    /** 
     * Send wall complete message.
     */
    static void sendWallCompleteMessage() {
    	
    }
    static void readWallCompleteMessage() {
    	
    }
    
    /**
     * 
     */
    void sendRushDetectedMessage(RobotInfo robot, int cost) throws GameActionException {
    	MapLocation loc = robot.getLocation();
    	int[] message = {1, MessageType.RUSH_DETECTED.ordinal(), 0, robotTypeToNum(robot.getType()), 0, loc.x, loc.y};
    	rc.submitTransaction(applyXORtoMessage(message, cost, rc.getRoundNum()), cost);
    }
    
    static void readRushDetectedMessage(int[] message) {
    	// Friendly HQ.
    	MapLocation location = new MapLocation(message[5], message[6]);
    	Unit.beingRushed = true;
		Unit.rushTargetLocation = location;
    }
    
    
    /**
     * Spawn message, used to keep track of macroeconomic stats.
     * @param buildOrderIndex
     * @throws GameActionException
     */
    void sendSpawnMessage(RobotType type, int cost) throws GameActionException {
    	int[] message = {1, MessageType.UNIT_SPAWNED.ordinal(), robotTypeToNum(type), 0, 0, 0, 0};
    	rc.submitTransaction(applyXORtoMessage(message, cost, rc.getRoundNum()), cost);
    }
    
    static void readSpawnMessage(int[] message) throws GameActionException {

    }
    
    /**
     * Announces to the world where the enemy HQ is.
     * 
     * @param enemy
     * @param cost
     * @throws GameActionException
     */
    void sendLocationMessage(RobotInfo robotInfo, MapLocation loc, int cost) throws GameActionException {
    	int elevation = -999;
    	// Try to get enemy location elevation
    	if (rc.canSenseLocation(loc)) {
    		elevation = rc.senseElevation(loc);
    	}
    	
    	int isFriendly = (rc.getTeam() == robotInfo.getTeam()) ? 1 : 0;

    	int[] message = {1, MessageType.LOCATION.ordinal(), isFriendly, robotTypeToNum(robotInfo.getType()), elevation, loc.x, loc.y};
    	rc.submitTransaction(applyXORtoMessage(message, cost, rc.getRoundNum()), cost);
    }
    
    static void readLocationMessage(int[] message) throws GameActionException {
    	
    	// Friendly HQ.
    	
    	int isFriendly = message[2];
    	RobotType type = numToRobotType(message[3]);
    	int elevation = message[4];
    	MapLocation location = new MapLocation(message[5], message[6]);
   
    	// Friendly HQ.
    	if (type == RobotType.HQ && isFriendly == 1) {
    		Unit.hqElevation = elevation;
    		Unit.hqLocation = location;
    		
    	}
    	// Enemy HQ.
    	if (type == RobotType.HQ && isFriendly == 0) {
    		Unit.enemyHQElevation = elevation;
			Unit.enemyHQLocation = location;
    	}
		
    }
    
    
    /**
     * Used to direct miners where to find more soup.
     * 
     * @param robotInfo
     * @param loc
     * @param cost
     * @throws GameActionException
     */
    void sendSoupLocationMessage(RobotInfo robotInfo, MapLocation loc, int cost) throws GameActionException {
    	
    }
    
    static void readSoupLocationMessage(int[] message) throws GameActionException {
    	
    }

    /**
     * 
     * @param t
     * @throws GameActionException
     */
    static void readTransaction(Transaction t, int roundNum) throws GameActionException {
    	int[] message = applyXORtoMessage(t.getMessage(), t.getCost(), roundNum);
    	if (message[0] != 1) {
    		return;
    	}
    	if (message[1] == MessageType.LOCATION.ordinal()) readLocationMessage(message);
    	if (message[1] == MessageType.RUSH_DETECTED.ordinal()) readRushDetectedMessage(message);
    	if (message[1] == MessageType.UNIT_SPAWNED.ordinal()) readSpawnMessage(message);
    	if (message[1] == MessageType.SOUP_LOCATION.ordinal()) readSoupLocationMessage(message);
    }
    
    void readBlock(Transaction[] ts, int roundNum) throws GameActionException {
    	for (Transaction t : ts) {
    		readTransaction(t, roundNum);
    	}
    }
    
    // Could be expensive as fuck
    static final int blocksPerRound = 20;
    void updateToLatestBlock() throws GameActionException {
    	int latestTransaction = rc.getRoundNum() - 1;
    	int counter = blocksPerRound;
    	while (lastBlockRead < latestTransaction && counter > 0) {
    		counter--;
    		lastBlockRead++;
    		readBlock(rc.getBlock(lastBlockRead), lastBlockRead);
    		
    	}
    }
}
