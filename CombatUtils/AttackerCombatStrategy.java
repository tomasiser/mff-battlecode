package KSTTForTheWin.CombatUtils;

import KSTTForTheWin.SharedUtils;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

import java.awt.*;
import java.util.Random;

/**
 * Created by simon-desktop on 3/7/2017.
 */
public class AttackerCombatStrategy extends BasicCombatStrategy {

    Random rnd;
    private final double HELP_PROBABILITY = 0.35;

    public AttackerCombatStrategy(RobotController rc, Team enemy) {
        super(rc, enemy);
        rnd = new Random();
    }

    /**
     * Look for a nearest enemy Archon which could be attacked.
     * @return True if a new goal was set.
     */
    @Override
    protected boolean chooseGoal() {
        MapLocation loc = broadcaster.findNearestArchon();
        if (loc == null || isVeryClose(loc) || rnd.nextDouble() <= HELP_PROBABILITY) {
            // even though attacking is preferred, now there is a friend I must help!
            loc = broadcaster.findNearestHelp();
        }

        if (loc != null && !isVeryClose(loc)) {
            setGoal(loc);
            return true;
        }

        return false;
    }

}
