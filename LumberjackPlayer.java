package KSTTForTheWin;
import KSTTForTheWin.CombatUtils.BasicCombatStrategy;
import KSTTForTheWin.CombatUtils.LumberjackStrategy;
import battlecode.common.*;

public strictfp class LumberjackPlayer {

    @SuppressWarnings("unused")

	static void runScout(RobotController rc) throws GameActionException {
        // System.out.println("I'm a KSTT lumberjack!");
        Team enemy = rc.getTeam().opponent();
        BasicCombatStrategy strategy = new LumberjackStrategy(rc, enemy);

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // the strategy handles everything
                strategy.update();

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            } catch (Exception e) {
                System.out.println("KSTT Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }
}
