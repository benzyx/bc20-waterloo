package dumbrushbot;
import battlecode.common.*;
public strictfp class RobotPlayer {

    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        
        Unit unit = new Unit(rc);
        
        switch (rc.getType()) {
        case HQ:                 unit = new HQ(rc);                	break;
        case MINER:              unit = new Miner(rc);              break;
        case REFINERY:           unit = new Refinery(rc);           break;
        case VAPORATOR:          unit = new Vaporator(rc);        	break;
        case DESIGN_SCHOOL:      unit = new DesignSchool(rc);      	break;
        case FULFILLMENT_CENTER: unit = new FulfillmentCenter(rc); 	break;
        case LANDSCAPER:         unit = new Landscaper(rc);        	break;
        case DELIVERY_DRONE:     unit = new Drone(rc);     			break;
        case NET_GUN:            unit = new NetGun(rc);            	break;
        case COW: 
        	System.out.println("What the fuck? Moo.");
        	break;
        }

        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                unit.run();

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
