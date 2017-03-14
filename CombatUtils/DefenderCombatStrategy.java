package KSTTForTheWin.CombatUtils;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

/**
 * This strategy should go for the location where some help is needed.
 */
public class DefenderCombatStrategy extends AttackerCombatStrategy {

    public DefenderCombatStrategy(RobotController rc, Team enemy) {
        super(rc, enemy);
        System.out.println("Defender!");
        goal = null; // start by waiting
    }


    @Override
    public void update() {
        super.update();
    }

    /**
     * Find a someone who needs my help.
     * @return True if a new goal was set.
     */
    @Override
    protected boolean chooseGoal() {
        setGoal(safeLocation); // return where the robot was born to defend the base
        return true;
    }
}
