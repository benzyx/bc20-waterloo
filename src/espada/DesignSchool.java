package espada;

import battlecode.common.*;

public class DesignSchool extends Unit {
	
    static Direction nextClockwiseDirection(Direction dir) {
    	switch (dir) {
    		case NORTH: 	return Direction.NORTHEAST;
    		case NORTHEAST: return Direction.EAST;
    		case EAST: 		return Direction.SOUTHEAST;
    		case SOUTHEAST: return Direction.SOUTH;
    		case SOUTH:		return Direction.SOUTHWEST;
    		case SOUTHWEST: return Direction.WEST;
    		case WEST:      return Direction.NORTHWEST;
    		case NORTHWEST: return Direction.NORTH;
    		case CENTER:    return Direction.CENTER;
    		default: return Direction.CENTER;
    	}
    }

	public DesignSchool(RobotController _rc) {
		super(_rc);
	}
	
	@Override
	public void run() throws GameActionException {
		txn.updateToLatestBlock();

    	if (rc.getTeamSoup() > 200 && Math.random() < 0.2) {    		
    		for (Direction dir : directions) {
    			tryBuild(RobotType.LANDSCAPER, dir);
    		}
    	}
	}
}
