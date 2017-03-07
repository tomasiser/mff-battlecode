package KSTTForTheWin.CombatUtils;

import KSTTForTheWin.SharedUtils;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

import java.awt.*;

/**
 * Created by simon-desktop on 3/7/2017.
 */
public class AttackerCombatStrategy extends BasicCombatStrategy {

    public AttackerCombatStrategy(RobotController rc, Team enemy) {
        super(rc, enemy);
    }

    /**
     * Look for a nearest enemy Archon which could be attacked.
     * @return True if a new goal was set.
     */
    @Override
    protected boolean chooseGoal() {
        MapLocation loc = broadcaster.findNearestArchon();
        if (loc == null) {
            // even though attacking is preferred, now there is a friend I must help!
            loc = broadcaster.findNearestHelp();
        }

        if (loc != null) {
            setGoal(loc);
            return true;
        }

        return false;
    }

}
