package espada;

import battlecode.common.*;

/**
 * Class that handles reading and sending transactions to the blockchain.
 * @author ben
 *
 */
class TransactionLogic {
	
	static RobotController rc;
	
	static int lastBlockRead = 0;
	
	public TransactionLogic(RobotController _rc) {
		rc = _rc;
	}

    // -------------------------
    // TRANSACTION MESSAGE SUITE
    // -------------------------
	
	static int secretKey = 0xE57ADA00;
    
	public enum MessageType {
		HQ_LOCATION,
		ENEMY_HQ_LOCATION,
		UNIT_SPAWNED,
		WALL_COMPLETE,
	}

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
    
    static int[] applyXORtoMessage(int[] message) {
    	for (int i = 0; i < message.length; i++) {
    		message[i] = message[i] ^ secretKey;
    	}
    	return message;
    }
    
    static void sendBuildOrderMessage(int buildOrderIndex) throws GameActionException {
    	
    }
    
    /**
     * Announces to the world where the enemy HQ is.
     * 
     * @param enemy
     * @param cost
     * @throws GameActionException
     */
    static void sendEnemyHQLocationMessage(MapLocation loc, int cost) throws GameActionException {
    	int elevation = -1;
    	// Try to get enemy location elevation
    	if (rc.canSenseLocation(loc)) {
    		elevation = rc.senseElevation(loc);
    	}

    	int[] message = {1, MessageType.ENEMY_HQ_LOCATION.ordinal(), 0, 0, elevation, loc.x, loc.y};
    	rc.submitTransaction(applyXORtoMessage(message), cost);
    }
    
    static void readEnemyHQLocationMessage(int[] message) throws GameActionException {
    	Unit.enemyHQElevation = message[4];
		Unit.enemyHQLocation = new MapLocation(message[5], message[6]);
    }
    
    /**
     * Announces to the world where the HQ is.
     * Should only be called by HQ.
     * 
     * @param cost
     * @throws GameActionException 
     */
    void sendHQLocationMessage(MapLocation loc, int cost) throws GameActionException {
    	int[] message = {1, MessageType.HQ_LOCATION.ordinal(), 0, 0, rc.senseElevation(loc), loc.x, loc.y};
    	rc.submitTransaction(applyXORtoMessage(message), cost);
    }
    static void readHQLocationMessage(int[] message) throws GameActionException {
    	Unit.hqElevation = message[4];
		Unit.hqLocation = new MapLocation(message[5], message[6]);
    }
    
    /**
     * Refinery Location Messages have type == 7.
     * @param t
     * @throws GameActionException
     */
    
    static void readTransaction(Transaction t) throws GameActionException {
    	int[] message = applyXORtoMessage(t.getMessage());
    	if (message[0] != 1) {
    		return;
    	}

    	if(message[1] == MessageType.HQ_LOCATION.ordinal()) readHQLocationMessage(message);
    	if(message[1] == MessageType.ENEMY_HQ_LOCATION.ordinal()) readEnemyHQLocationMessage(message);
    }
    
    void readBlock(Transaction[] ts) throws GameActionException {
    	for (Transaction t : ts) {
    		readTransaction(t);
    	}
    }
    
    // Could be expensive as fuck
    static final int blocksPerRound = 20;
    void updateToLatestBlock() throws GameActionException {
    	int latestTransaction = rc.getRoundNum() - 1;
    	int counter = blocksPerRound;
    	while (lastBlockRead < latestTransaction && counter > 0) {
    		counter--;
    		readBlock(rc.getBlock(++lastBlockRead));
    		
    	}
    }
}
