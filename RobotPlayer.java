package KSTTForTheWin;
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

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                ArchonPlayer.runArchon(rc);
                break;
            case GARDENER:
                GardenerPlayer.runGardener(rc);
                break;
            case SOLDIER:
                SoldierPlayer.runSoldier(rc);
                break;
            case TANK:
                TankPlayer.runTank(rc);
                break;
            case SCOUT:
                ScoutPlayer.runScout(rc);
                break;
            case LUMBERJACK:
                LumberjackPlayer.runScout(rc);
                break;
        }
	}
}
