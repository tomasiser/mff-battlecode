package KSTTForTheWin.CombatUtils;

import KSTTForTheWin.Broadcasting.Broadcaster;
import KSTTForTheWin.SharedUtils;
import battlecode.common.*;

/**
 * Basic strategy for combat units - this should be the common core for more advanced strategies
 * built on top of this skeleton.
 * @author Simon Rozsival <simon@rozsival.com>
 */
public strictfp abstract class BasicCombatStrategy {

    // the model of the environment
    private RobotController rc;
    private MapLocation goal;
    private boolean hasOnlyTmpGoal;
    private MapLocation safeLocation;
    private Team enemy;
    private RobotInfo[] nearbyEnemyRobots;
    private BulletInfo[] nearbyBullets;
    private RobotInfo lastTarget;
    private MapLocation me;

    // pseudo constants
    protected float GOAL_RADIUS_SQ = 25f; // @todo what is the scale?
    protected float SHOOTING_RANGE_SQ = 0; // @todo what is the scale?

    // communication with the team
    protected Broadcaster broadcaster;

    /**
     * Every combat unit should use this utils for their common behavior.
     * @param rc The controller of the robot.
     */
    public BasicCombatStrategy(RobotController rc, Team enemy) {
        this.broadcaster = new Broadcaster(rc);
        this.rc = rc;
        me = rc.getLocation();
        safeLocation = rc.getLocation(); // remember this location for when the unit should flee
        this.enemy = enemy;
        hasOnlyTmpGoal = true;
    }

    /**
     * Make one tick/step of the unit's life.
     */
    public final void update() {
        // prepare for the new turn
        me = rc.getLocation();
        try {
            broadcaster.refresh();
        } catch (GameActionException e) {}

        if (lastTarget != null) {
            checkIfIKilledArchon();
            lastTarget = null;
        }

        // first make sure the unit knows, where it wants to go
        if (shouldChooseNewGoal()) {
            setGoal(null); // forget the previous goal
            if (!chooseNextGoal()) {
                // there is nothing to do... - no known positions of enemies and nobody needs help
                // so return back home
                setGoal(chooseRandomGoal());
                hasOnlyTmpGoal = true;
            } else {
                System.out.println("Soldier " + rc.getID() + " has NEW GOAL");
                hasOnlyTmpGoal = false;
            }
        }

        // default behavior is to get closer to the global goal (if any)
        MapLocation currentGoal = goal;

        // place a debug flag on the goal (if any)
        if (hasGoal()) {
            rc.setIndicatorDot(goal, 0, 255, 0);
            rc.setIndicatorLine(me, goal, 0, 255, 0);
        } else {
            rc.setIndicatorDot(me, 0, 0, 0);
        }

        lookAround();

        // fight the enemies if they are nearby
        if (shouldFight()) {
            System.out.println("Soldier " + rc.getID() + " should FIGHT!");
            RobotInfo target = chooseBestShootingTarget();
            if (target != null) {
                System.out.println("Soldier " + rc.getID() + " TARGET ACQUIRED!");
                boolean burst = false; // @todo check if there are multiple enemies in that direction
                shootAt(target, burst);
            }
        }

        // even if I am fighting, I might not go directly towards the enemy, but rather flee for life!
        if (shouldFlee()) {
            System.out.println("Soldier " + rc.getID() + " runs away");
            currentGoal = safeLocation;
        }

        if (currentGoal != null) {
            rc.setIndicatorLine(me, currentGoal, 255, 0, 0);
        }

        BulletInfo dangerousBullet = getMostDangerousBullet();
        if (dangerousBullet != null) {
            // no time for reaching goals - just try to survive!
            rc.setIndicatorDot(dangerousBullet.getLocation(),0,255,255);
            rc.setIndicatorLine(me, dangerousBullet.getLocation(),0,255,255);

            try {
                SharedUtils.tryMove(rc, SharedUtils.getDodgeDirection(rc, dangerousBullet));
            } catch (GameActionException e) {
                // whaaaat???!!! I might die!!
            }
        } else if (currentGoal != null) {
            // move in the direction of the goal of this round
            moveTowardsAGoal(currentGoal);
        }
    }

    /**
     * Use the sensors to see what is around the unit.
     */
    private void lookAround() {
        nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemy);
        nearbyBullets = rc.senseNearbyBullets();
    }

    /**
     * There might be some global goal of the unit.
     * @return True if the unit has a goal which it follows, false otherwise.
     */
    protected boolean hasGoal() {
        return goal != null;
    }

    /**
     * Set a new goal location.
     * @param goal The new goal
     */
    protected void setGoal(MapLocation goal) {
        this.goal = goal;
    }

    /**
     * Check if the unit is close enough to its goal.
     * @return Whether the unit reached the goal or not.
     */
    private boolean hasReachedGoal() {
        return hasGoal() && goal.distanceSquaredTo(me) < GOAL_RADIUS_SQ;
    }

    /**
     *
     * @return New goal
     */
    protected MapLocation chooseRandomGoal() {
        // @todo make this somehow smart..
        return safeLocation;
    }

    /**
     * Look at the nearby bullets and figure out, if some is about to hit me soon.
     * @return The bullet which is about to hit me if I don't do anything about it.
     */
    protected BulletInfo getMostDangerousBullet() {
        BulletInfo bullet = null;

        for (BulletInfo nearbyBullet : nearbyBullets) {
            if(SharedUtils.willCollideWithMe(rc, nearbyBullet)) {
                if (bullet == null || nearbyBullet.getLocation().distanceSquaredTo(me) < bullet.getLocation().distanceSquaredTo(me)) {
                    bullet = nearbyBullet;
                }
            }
        }

        return bullet;
    }

    /**
     * GO KILL THEM!!!
     * @return If there is some enemy robot to shoot at, shoot at it!
     */
    protected boolean shouldFight() {
        return nearbyEnemyRobots.length > 0;
    }

    /**
     * If there is a dangerous enemy robot nearby, flee!
     * @return True when there is a danger nearby
     */
    protected boolean shouldFlee() {
        return false;
    }

    /**
     * Find a target which should be targeted first.
     * @return The nearest enemy with the highest priority (by its type)
     */
    protected RobotInfo chooseBestShootingTarget() {
        RobotInfo nearest = null;
        for (RobotInfo robot : nearbyEnemyRobots) {
            if (nearest == null ||
                    getTargetPriority(robot.getType()) > getTargetPriority(nearest.getType()) ||
                    me.distanceSquaredTo(robot.getLocation()) < me.distanceSquaredTo(nearest.getLocation())) {
                // do not be too fast with shooting...
                if (!isItObviouslyStupidToFireAt(robot)) {
                    nearest = robot;
                }
            }
        }

        rc.setIndicatorDot(nearest.getLocation(), 125, 0, 0);
        return nearest;
    }

    /**
     * Get the priority of a targeted enemy by its type.
     * @param type The type of the enemy.
     * @return The number which denotes the
     */
    private int getTargetPriority(RobotType type) {
        switch (type) {
            case TANK:
                return 6; // destroy the defenders first
            case SOLDIER:
                return 5;
            case ARCHON:
                return 4;
            case LUMBERJACK:
                return 3;
            case SCOUT:
                return 2;
            case GARDENER:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * Choosing a new goal is potentially a computationally heavy operation, do not do that unless necessary.
     * @return Choose next goal when there is no goal ready yet.
     */
    protected boolean shouldChooseNewGoal() {
        return !hasGoal() || hasOnlyTmpGoal || hasReachedGoal();
    }

    /**
     * Select a location where the unit should go and be useful for the team.
     * @return True if some goal was selected.
     */
    protected boolean chooseNextGoal() {
        MapLocation nearestAction = broadcaster.findNearestAction();
        if (nearestAction != null && nearestAction != me) {
            goal = nearestAction;
        }

        return hasGoal();
    }

    /**
     * Choose a direction in which the unit should move to reach the goal.
     * @return The next spot where the unit wants to step.
     */
    private void moveTowardsAGoal(MapLocation goal) {
        MapLocation nextStep = goal;

        // @todo: perform local search (DFS) with some reasonable stack of crossroads for backtracking

        // now that I know the next spot I want to end up on, go in that direction
        Direction direction = me.directionTo(nextStep);
        if (direction != null) {
            try {
                SharedUtils.tryMove(rc, direction);
            } catch (GameActionException e) {
            }
        }
    }

    /**
     * Try to shoot in the direction of the target's location.
     * @param targetRobot The enemy to shoot at.
     * @param shootBurst Shoot single shot or
     */
    private void shootAt(RobotInfo targetRobot, boolean shootBurst) {
        if (targetRobot != null && hasEnoughAmmunition(shootBurst) && !isItObviouslyStupidToFireAt(targetRobot)) {
            boolean shot;
            if (shootBurst) {
                shot = shootBurst(targetRobot);
            } else {
                shot = shootSingle(targetRobot);
            }

            if (shot) {
                lastTarget = targetRobot;
            }
        }
    }

    /**
     * Do not waste any ammunition and aim exactelly at the target!
     * @param targetRobot The robot to shoot at.
     */
    protected boolean shootSingle(RobotInfo targetRobot) {
        boolean success = true;
        try {
            rc.fireSingleShot(me.directionTo(targetRobot.location));
        } catch (GameActionException e) {
            success = false;
        }

        return success;
    }

    /**
     * Well the simplest burst is a burst of one bullet.
     * @param targetRobot The robot to shoot at.
     */
    protected boolean shootBurst(RobotInfo targetRobot) {
        return shootSingle(targetRobot);
    }

    /**
     * Check if there is no obvious reason not to shoot at the target.
     * @param targetRobot The robot I want to shoot at.
     * @return True when it should be OK to fire at the target.
     */
    private boolean isItObviouslyStupidToFireAt(RobotInfo targetRobot) {
        // @todo check for friendly fire
        return false;// targetRobot.getLocation().distanceSquaredTo(me) > SHOOTING_RANGE_SQ; // the robot is too far away
    }

    /**
     * Can I shoot at this moment?
     * @return True when it is OK to fire (considering the amount of the team's bullets)
     */
    private boolean hasEnoughAmmunition(boolean shootBurst) {
        return shootBurst ? rc.canFireTriadShot() : rc.canFireSingleShot();
    }

    /**
     * If I shot at some robot last turn, it maybe died. At it could have been enemy ARCHON!!
     */
    private void checkIfIKilledArchon() {
        if (lastTarget != null && lastTarget.getType() == RobotType.ARCHON) {
            if (lastTarget.getHealth() <= 0) { // @todo does 0 mean already dead or should it be negative?
                try {
                    broadcaster.reportEnemyArchonDied(lastTarget.getID());
                } catch (GameActionException e) {
                    // well, I could not inform the mates, but it is not the end of the world... at least I don't explode!
                }
            }

            lastTarget = null;
        }
    }

}