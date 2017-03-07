package KSTTForTheWin;
import KSTTForTheWin.Broadcasting.Broadcaster;
import KSTTForTheWin.CombatUtils.AttackerCombatStrategy;
import KSTTForTheWin.CombatUtils.BasicCombatStrategy;
import KSTTForTheWin.CombatUtils.DefenderCombatStrategy;
import battlecode.common.*;

public strictfp class SoldierPlayer {

    @SuppressWarnings("unused")

    static Broadcaster broadcaster;
    static RobotController rc;

	static void runSoldier(RobotController rcon) throws GameActionException {
        System.out.println("I'm a KSTT soldier!");
        rc = rcon;
        Team enemy = rc.getTeam().opponent();
        BasicCombatStrategy strategy = new AttackerCombatStrategy(rc, enemy);

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // The strategy controls the soldier
                strategy.update();

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            } catch (Exception e) {
                System.out.println("KSTT Soldier Exception");
                e.printStackTrace();
                rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
            }
        }
    }
}
