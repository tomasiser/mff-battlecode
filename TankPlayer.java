package KSTTForTheWin;
import KSTTForTheWin.CombatUtils.AttackerCombatStrategy;
import KSTTForTheWin.CombatUtils.BasicCombatStrategy;
import battlecode.common.*;

public strictfp class TankPlayer {

    @SuppressWarnings("unused")

	static void runTank(RobotController rc) throws GameActionException {
        System.out.println("I'm a KSTT tank!");
        Team enemy = rc.getTeam().opponent();
        BasicCombatStrategy strategy = new AttackerCombatStrategy(rc, enemy);

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // The strategy controls the tank
                strategy.update();

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("KSTT Tank Exception");
                e.printStackTrace();
            }
        }
    }
}
