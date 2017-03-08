package KSTTForTheWin.CombatUtils;
import battlecode.common.*;

/**
 * This strategy should go for the location where some help is needed.
 */
public class LumberjackStrategy extends AttackerCombatStrategy {

    public LumberjackStrategy(RobotController rc, Team enemy) {
        super(rc, enemy);
    }

    /**
     * Shooting in fact means hitting with the axe.
     */
    @Override
    public boolean shootSingle(RobotInfo targetRobot) {
        try {
            rc.strike(); // strike anything
            return true;
        } catch (GameActionException e) {
            return false;
        }
    }

    /**
     * Well the simplest burst is a burst of one bullet.
     * @param targetRobot The robot to shoot at.
     */
    @Override
    protected boolean shootBurst(RobotInfo targetRobot) {
        return shootSingle(targetRobot);
    }

    /**
     * The lumberjack cannot shoot, it just hits all the stuff with the axe in his reach.
     * @return Some enemy nearby.
     */
    @Override
    protected RobotInfo chooseBestShootingTarget() {
        RobotInfo[] hittableOurRobots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam());
        RobotInfo[] hittableEnemyRobots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
        if (hittableOurRobots.length > 0) {
            return null; // "friendly-chopping"!!!
        } else if (hittableEnemyRobots.length > 0) {
            return hittableEnemyRobots[0]; // any robot will do
        }

        return null;
    }

    /**
     * Choose a direction in which the unit should move to reach the goal.
     * @return The next spot where the unit wants to step.
     */
    @Override
    protected void moveTowardsAGoal(MapLocation goal) {
        try {
            // shake and chop nearby trees
            TreeInfo[] trees = rc.senseNearbyTrees(1.9f, Team.NEUTRAL);
            if (trees.length > 0) {
                rc.shake(trees[0].ID);
                rc.chop(trees[0].ID);
            } else {
                trees = rc.senseNearbyTrees(1.9f, enemy);
                if (trees.length > 0) {
                    rc.chop(trees[0].ID);
                } else {
                    super.moveTowardsAGoal(goal);
                }
            }
        } catch (GameActionException e) { }
    }
}