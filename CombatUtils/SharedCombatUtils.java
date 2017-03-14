package KSTTForTheWin.CombatUtils;

import KSTTForTheWin.*;
import battlecode.common.*;
import java.util.*;

public final class SharedCombatUtils {
    public enum ShotType {
        NONE,
        SINGLE,
        TRIAD,
        PENTAD
    }

    /**
     * Do not waste any ammunition and aim exactelly at the target!
     * @param targetRobot The robot to shoot at.
     */
    public static boolean shoot(RobotController rc, Direction direction, RobotInfo[] nearbyEnemyRobots, RobotInfo[] nearbyOurRobots, TreeInfo[] nearbyTrees) {
        boolean success = true;
        ShotType type = chooseBestShotType(rc, direction, nearbyEnemyRobots, nearbyOurRobots, nearbyTrees);

        try {
            switch (type) {
                case SINGLE:
                    rc.fireSingleShot(direction);
                    break;
                case TRIAD:
                    rc.fireTriadShot(direction);
                    break;
                case PENTAD:
                    rc.firePentadShot(direction);
                    break;
            }
        } catch (GameActionException e) {
            success = false;
        }

        return success;
    }

    private static ShotType chooseBestShotType(RobotController rc, Direction direction, RobotInfo[] nearbyEnemyRobots, RobotInfo[] nearbyOurRobots, TreeInfo[] nearbyTrees) {
        MapLocation me = rc.getLocation();
        if (!rc.canFireSingleShot() || !shouldFireSingle(me, direction, nearbyEnemyRobots, nearbyOurRobots, nearbyTrees)) {
            return ShotType.NONE;
        } else if (!rc.canFireTriadShot() || !shouldFireTriad(me, direction, nearbyEnemyRobots, nearbyOurRobots, nearbyTrees)) {
            return ShotType.SINGLE;
        } else if (!rc.canFirePentadShot() || !shouldFirePentad(me, direction, nearbyEnemyRobots, nearbyOurRobots, nearbyTrees)) {
            return ShotType.TRIAD;
        } else {
            return ShotType.PENTAD;
        }
    }

    /**
     * Check if there is no obvious reason not to shoot at the target.
     * @param targetRobot The robot I want to shoot at.
     * @return True when it should be OK to fire at the target.
     */
    private static boolean shouldNotFireSingle(MapLocation me, Direction direction, RobotInfo[] nearbyEnemyRobots, RobotInfo[] nearbyOurRobots, TreeInfo[] nearbyTrees) {
        boolean willHitSomeNearbyEnemyRobot = false;
        for (RobotInfo enemyRobot : nearbyEnemyRobots) {
            MapLocation target = enemyRobot.getLocation();
            if (SharedUtils.willCollide(me, target, enemyRobot.getRadius(), direction)) {
                boolean isGoodTarget = true;            
                // look for the teammates and trees which are in the way
                for (RobotInfo ourRobot : nearbyOurRobots) {
                    if (me.distanceSquaredTo(ourRobot.getLocation()) < me.distanceSquaredTo(target)
                            && SharedUtils.willCollide(me, ourRobot.getLocation(), ourRobot.getRadius(), direction)) {
                        isGoodTarget = false;
                        break;
                    }
                }

                // trees can be destroyed by bullets, but bullets meant for the enemies!!
                for (TreeInfo tree : nearbyTrees) {
                    if (me.distanceSquaredTo(tree.getLocation()) < me.distanceSquaredTo(target)
                            && SharedUtils.willCollide(me, tree.getLocation(), tree.getRadius(), direction)) {
                        isGoodTarget = false;
                        break;
                    }
                }

                if (isGoodTarget) {
                    willHitSomeNearbyEnemyRobot = true;
                    break;
                }
            }
        }

        return !willHitSomeNearbyEnemyRobot;
    }


    /**
     * Check if there is no obvious reason not to shoot at the target.
     * @param direction The direction of the shot.
     * @return True when it should be OK to fire at the target.
     */
    private static boolean shouldFireSingle(MapLocation me, Direction direction, RobotInfo[] nearbyEnemyRobots, RobotInfo[] nearbyOurRobots, TreeInfo[] nearbyTrees) {
        return !shouldNotFireSingle(me, direction, nearbyEnemyRobots, nearbyOurRobots, nearbyTrees);
    }

    /**
     * Check if the direction is suitable for a triad shot.
     * @param The middle direction in which we could fire.
     * @return True when the triad shot is good option.
     */
    private static boolean shouldFireTriad(MapLocation me, Direction direction, RobotInfo[] nearbyEnemyRobots, RobotInfo[] nearbyOurRobots, TreeInfo[] nearbyTrees) {
        ArrayList<Direction> triadDirections = new ArrayList(3);
        triadDirections.add(direction);
        triadDirections.add(direction.rotateLeftDegrees(20f));
        triadDirections.add(direction.rotateRightDegrees(20f));

        return shouldFireMultiple(me, triadDirections.toArray(new Direction[0]), nearbyEnemyRobots, nearbyOurRobots, nearbyTrees);
    }

    /**
     * Check if the direction is suitable for a pentad shot.
     * @param The middle direction in which we could fire.
     * @return True when the triad shot is good option.
     */
    private static boolean shouldFirePentad(MapLocation me, Direction direction, RobotInfo[] nearbyEnemyRobots, RobotInfo[] nearbyOurRobots, TreeInfo[] nearbyTrees) {
        ArrayList<Direction> pentadDirections = new ArrayList(5);
        pentadDirections.add(direction);
        pentadDirections.add(direction.rotateLeftDegrees(15f));
        pentadDirections.add(direction.rotateLeftDegrees(30f));
        pentadDirections.add(direction.rotateRightDegrees(15f));
        pentadDirections.add(direction.rotateRightDegrees(30f));

        return shouldFireMultiple(me, pentadDirections.toArray(new Direction[0]), nearbyEnemyRobots, nearbyOurRobots, nearbyTrees);
    }

    /**
     * Check if the direction is suitable for a multiple shot (pentad, triad).
     * @param directions The directions in which we could fire.
     * @return True when the triad shot is good option.
     */
    private static boolean shouldFireMultiple(MapLocation me, Direction[] directions, RobotInfo[] nearbyEnemyRobots, RobotInfo[] nearbyOurRobots, TreeInfo[] nearbyTrees) {
        float score = 0;
        System.out.println(directions.length);
        for (Direction direction : directions) {
            score += shouldFireSingle(me, direction, nearbyEnemyRobots, nearbyOurRobots, nearbyTrees) ? 1f : 0f;
        }

        return (score / directions.length) > 0.5f; // most of the shots are successful
    }

    /**
     * Check if there is no obvious reason not to shoot at the target.
     * @param targetRobot The robot I want to shoot at.
     * @return True when it should be OK to fire at the target.
     */
    public static boolean isItObviouslyStupidToFireInDirection(MapLocation me, Direction direction, RobotInfo[] nearbyEnemyRobots, RobotInfo[] nearbyOurRobots, TreeInfo[] nearbyTrees) {
        return shouldNotFireSingle(me, direction, nearbyEnemyRobots, nearbyOurRobots, nearbyTrees);
    }

    /**
     * Get the priority of a targeted enemy by its type.
     * @param type The type of the enemy.
     * @return The number which denotes the
     */
    public static int getTargetPriority(RobotType type) {
        switch (type) {
            case TANK:
                return 6; // destroy the attackers/defenders first
            case SOLDIER:
                return 5; // destroy the attackers/defenders first
            case SCOUT:
                return 4; // destroy the attackers/defenders first
            case LUMBERJACK:
                return 3;
            case ARCHON:
                return 2;
            case GARDENER:
                return 1;
            default:
                return 0;
        }
    }

}