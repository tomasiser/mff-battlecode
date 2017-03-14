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

    private final double HELP_PROBABILITY = 0.5;
    private final double DESTROY_ENEMY_FIRST_PROBABILITY = 0.8;
    private MapLocation prevGoal;

    public AttackerCombatStrategy(RobotController rc, Team enemy) {
        super(rc, enemy);
        prevGoal = null;
        goal = broadcaster.findNearestArchon();
    }

    /**
     * Look for a nearest enemy Archon which could be attacked.
     * @return True if a new goal was set.
     */
    @Override
    protected boolean chooseGoal() {
        MapLocation loc = broadcaster.findNearestArchon();
        if (loc == null || loc.equals(prevGoal) || isVeryClose(loc) || rnd.nextDouble() <= HELP_PROBABILITY) {
            // even though attacking is preferred, now there is a friend I must help!
            loc = broadcaster.findNearestHelp();
        }

        if (loc != null && !isVeryClose(loc)) {
            prevGoal = goal;
            setGoal(loc);
            return true;
        }

        return false;
    }

    /**
     * Find a target which should be targeted first.
     * @return The nearest enemy with the highest priority (by its type)
     */
    protected RobotInfo chooseBestShootingTarget() {
        RobotInfo nearest = super.chooseBestShootingTarget();
        if (nearest != null && rnd.nextDouble() < DESTROY_ENEMY_FIRST_PROBABILITY) {
            setGoal(nearest.getLocation()); // kill this enemy first before going for the real target
        }

        return nearest;
    }

}
