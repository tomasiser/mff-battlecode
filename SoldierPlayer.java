package KSTTForTheWin;
import KSTTForTheWin.Broadcasting.Broadcaster;
import KSTTForTheWin.CombatUtils.AttackerCombatStrategy;
import KSTTForTheWin.CombatUtils.BasicCombatStrategy;
import KSTTForTheWin.CombatUtils.DefenderCombatStrategy;
import battlecode.common.*;

import java.util.Random;

public strictfp class SoldierPlayer {

    @SuppressWarnings("unused")

    static Broadcaster broadcaster;
    static RobotController rc;
    static Random rnd = new Random();
    static double attackerChance = 0.6;

	static void runSoldier(RobotController rcon) throws GameActionException {
        System.out.println("I'm a KSTT soldier!");
        rc = rcon;
        Team enemy = rc.getTeam().opponent();

        // some are attackers, some are defenders
        BasicCombatStrategy strategy = rnd.nextDouble() <= attackerChance
                ? new AttackerCombatStrategy(rc, enemy)
                : new DefenderCombatStrategy(rc, enemy);

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
