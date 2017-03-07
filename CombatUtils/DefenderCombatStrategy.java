package KSTTForTheWin.CombatUtils;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

/**
 * This strategy should go for the location where some help is needed.
 */
public class DefenderCombatStrategy extends BasicCombatStrategy {

    public DefenderCombatStrategy(RobotController rc, Team enemy) {
        super(rc, enemy);
    }

    /**
     * Find a someone who needs my help.
     * @return True if a new goal was set.
     */
    @Override
    protected boolean chooseNextGoal() {
        MapLocation loc = broadcaster.findNearestHelp();
        if (loc != null) {
            setGoal(loc);
            return true;
        }

        return false;
    }
}
