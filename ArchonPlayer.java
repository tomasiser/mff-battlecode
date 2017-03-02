package KSTTForTheWin;
import battlecode.common.*;

public strictfp class ArchonPlayer {

    @SuppressWarnings("unused")

    static void runArchon(RobotController rc) throws GameActionException {
        System.out.println("I'm a KSTT archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = SharedUtils.randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .01) {
                    rc.hireGardener(dir);
                }

                // Move randomly
                SharedUtils.tryMove(rc, SharedUtils.randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("KSTT Archon Exception");
                e.printStackTrace();
            }
        }
    }

}
