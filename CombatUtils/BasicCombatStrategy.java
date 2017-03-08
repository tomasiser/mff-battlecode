package KSTTForTheWin.CombatUtils;

import KSTTForTheWin.Broadcasting.ArchonLocation;
import KSTTForTheWin.Broadcasting.Broadcaster;

import java.util.Random;

import KSTTForTheWin.SharedUtils;
import battlecode.common.*;

/**
 * Basic strategy for combat units - this should be the common core for more advanced strategies
 * built on top of this skeleton.
 * @author Simon Rozsival <simon@rozsival.com>
 */
public strictfp abstract class BasicCombatStrategy {

    // the model of the environment
    protected RobotController rc;
    protected MapLocation goal;
    protected boolean hasOnlyTmpGoal;
    private MapLocation safeLocation;
    protected Team enemy;
    protected RobotInfo[] nearbyEnemyRobots;
    protected RobotInfo[] nearbyOurRobots;
    protected BulletInfo[] nearbyBullets;
    protected TreeInfo[] nearbyTrees;
    private RobotInfo lastTarget;
    private ArchonLocation archonTarget;
    private MapLocation me;
    private int roundsOnTheSameSpot;
    protected int remainingRandomRounds;
    protected Random rnd;

    // pseudo constant
    private float VERY_CLOSE_SQ = 20f;

    // communication with the team
    Broadcaster broadcaster;

    /**
     * Every combat unit should use this utils for their common behavior.
     * @param rc The controller of the robot.
     */
    BasicCombatStrategy(RobotController rc, Team enemy) {
        this.broadcaster = new Broadcaster(rc);
        this.rc = rc;
        me = rc.getLocation();
        safeLocation = rc.getLocation(); // remember this location for when the unit should flee
        this.enemy = enemy;
        hasOnlyTmpGoal = true;
        roundsOnTheSameSpot = 0;
        remainingRandomRounds = 0;
        rnd = new Random();
    }

    /**
     * Make one tick/step of the unit's life.
     */
    public void update() {
        // prepare for the new turn
        if (rc.getLocation().equals(me)) {
            roundsOnTheSameSpot++;
        } else {
            roundsOnTheSameSpot = 0;
            me = rc.getLocation();
        }

        try {
            broadcaster.refresh();
        } catch (GameActionException e) {
            System.out.println("OUCH! broadcaster cannot refresh");
        }

        // I might have killed the ARCHON and that would be awesome and I have to tell it to my friends then!
        if (archonTarget != null) {
            checkIfIKilledArchon();
        }

        // first make sure the unit knows, where it wants to go
        if (shouldChooseNewGoal()) {
            setGoal(null); // forget the previous goal
            if (!chooseGoal()) {
                // there is nothing to do... - no known positions of enemies and nobody needs help
                // so return back home
                setGoal(chooseRandomGoal());
                hasOnlyTmpGoal = true;
            } else {
                hasOnlyTmpGoal = false;
            }
        }

        // default behavior is to get closer to the global goal (if there is any)
        MapLocation currentGoal = goal;

        // DEBUG: place a debug flag on the goal (if any)
        if (hasGoal()) {
            rc.setIndicatorDot(goal, 0, 255, 0);
            rc.setIndicatorLine(me, goal, 0, 255, 0);
        }

        lookAround();

        // fight the enemies if they are nearby
        if (shouldFight()) {
            RobotInfo target = chooseBestShootingTarget();
            if (target != null) {
                boolean burst = false; // @todo check if there are multiple enemies in that direction
                shootAt(target, burst);
            }
        }

        // even if I am fighting, I might not go directly towards the enemy, but rather flee for life!
        if (shouldFlee()) {
            currentGoal = safeLocation;

            try {
                broadcaster.reportHelpNeeded();
            } catch (GameActionException e) {
                System.out.println("I try to call for help, but the radio does not work!");
            }
        }

        // DEBUG: mark the current goal
        // if (currentGoal != null) {
        //     rc.setIndicatorLine(me, currentGoal, 255, 0, 0);
        // }

        if (stuckForTooLong()) {
            remainingRandomRounds += 10; // wander around for a while to get from the dead end
        }

        // check if there is a risk of being hit by a bullet
        BulletInfo dangerousBullet = getMostDangerousBullet();
        if (dangerousBullet != null) {
            dodgeBullet(dangerousBullet);
        } else if (remainingRandomRounds > 0) {
            moveRandomly();
            remainingRandomRounds--;
        } else if (currentGoal != null) {
            // move in the direction of the goal of this round
            moveTowardsAGoal(currentGoal);
        }
    }

    /**
     * No time for reaching goals - just try to survive!
     * @param dangerousBullet The bullet which might hit me
     */
    private void dodgeBullet(BulletInfo dangerousBullet) {
        // DEBUG: mark the bullet and the robot it endangers
        rc.setIndicatorDot(dangerousBullet.getLocation(),0,255,255);
        rc.setIndicatorLine(me, dangerousBullet.getLocation(),0,255,255);

        try {
            SharedUtils.tryMove(rc, SharedUtils.getDodgeDirection(rc, dangerousBullet));
        } catch (GameActionException e) {
            // whaaaat???!!! I might die!!
        }
    }

    /**
     * Use the sensors to see what is around the unit.
     */
    protected void lookAround() {
        nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemy);
        nearbyOurRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        nearbyTrees = rc.senseNearbyTrees();

        for (RobotInfo robot : nearbyEnemyRobots) {
            if (robot.getType() == RobotType.ARCHON) {
                try {
                    broadcaster.reportEnemyArchon(robot.getID(), robot.getLocation());
                } catch (GameActionException e) {
                    // no idea what to do.. so just a silent error
                    System.out.println("Cannot report the position of the archon " + robot.getID());
                }

                setGoal(robot.getLocation()); // just switch the goal to the archon I found
            }
        }

        nearbyBullets = rc.senseNearbyBullets();
    }

    /**
     * There might be some global goal of the unit.
     * @return True if the unit has a goal which it follows, false otherwise.
     */
    private boolean hasGoal() {
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
        if (hasGoal() && isVeryClose(goal)) {
            setGoal(null);
            return true;
        }

        return false;
    }

    /**
     * Check if the location is close to me.
     * @return The location is within reach
     */
    protected boolean isVeryClose(MapLocation loc) {
        return loc.distanceSquaredTo(me) < VERY_CLOSE_SQ;
    }

    /**
     *
     * @return New goal
     */
    private MapLocation chooseRandomGoal() {
        return broadcaster.randomArchonLocation();
    }

    /**
     * Look at the nearby bullets and figure out, if some is about to hit me soon.
     * @return The bullet which is about to hit me if I don't do anything about it.
     */
    private BulletInfo getMostDangerousBullet() {
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
     * If there are dangerous enemy robot nearby, flee!
     * @return True when there is a danger nearby
     */
    protected boolean shouldFlee() {
        // I don't have ammo, just run away
        if (nearbyEnemyRobots.length > 0 && !rc.canFirePentadShot()) {
            return true;
        }

        // if there are more opponents than our robots, flee as well and wait for reinforcements
        float ourHP = rc.getHealth();
        for (RobotInfo robot : nearbyOurRobots) {
            if (SharedUtils.robotIsDangerous(robot)) {
                ourHP += robot.getHealth();
            }
        }

        float theirHP = 0;
        for (RobotInfo robot : nearbyEnemyRobots) {
            if (SharedUtils.robotIsDangerous(robot)) {
                theirHP += robot.getHealth();
            }
        }

        double factor = SharedUtils.robotTypeIsDangerous(rc.getType()) ? 1.5 : 0.9;
        return factor * ourHP < theirHP;
    }

    /**
     * Find a target which should be targeted first.
     * @return The nearest enemy with the highest priority (by its type)
     */
    protected RobotInfo chooseBestShootingTarget() {
        RobotInfo nearest = null;
        for (RobotInfo robot : nearbyEnemyRobots) {
            if ((nearest == null ||
                    getTargetPriority(robot.getType()) > getTargetPriority(nearest.getType()) ||
                    me.distanceSquaredTo(robot.getLocation()) < me.distanceSquaredTo(nearest.getLocation())) &&
                    !isItObviouslyStupidToFireAt(robot)) {
                nearest = robot;
            }
        }

        // DEBUG: mark the target
        if (nearest != null) {
            rc.setIndicatorDot(nearest.getLocation(), 125, 0, 0);
        }

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
        return !hasGoal() || stuckForTooLong() || hasOnlyTmpGoal || hasReachedGoal();
    }

    /**
     * Select a location where the unit should go and be useful for the team.
     * @return True if some goal was selected.
     */
    protected boolean chooseGoal() {
        MapLocation nearestAction = broadcaster.findNearestAction();

        // do not choose a goal which I have already reached
        if (nearestAction != null && !isVeryClose(nearestAction)) {
            System.out.println("Proper goal was chosen.");
            goal = nearestAction;
        }

        return hasGoal();
    }

    /**
     * Check if the goal is not reachable for some reason - there si some obsatacle I cannot overcome.
     * @return True when the the robot stays on the same spot for several rounds.
     */
    protected boolean stuckForTooLong() {
        return roundsOnTheSameSpot > 5;
    }

    /**
     * Choose a direction in which the unit should move to reach the goal.
     * @param goal The spot I want to reach
     */
    protected void moveTowardsAGoal(MapLocation goal) {
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
     * Choose a direction in which the unit should move to reach the goal.
     */
    protected void moveRandomly() {
        // now that I know the next spot I want to end up on, go in that direction
        Direction direction = new Direction((float) (rnd.nextDouble() * Math.PI));
        try {
            SharedUtils.tryMove(rc, direction);
        } catch (GameActionException e) {
        }
    }

    /**
     * Try to shoot in the direction of the target's location.
     * @param targetRobot The enemy to shoot at.
     * @param shootBurst Shoot single shot or
     */
    private void shootAt(RobotInfo targetRobot, boolean shootBurst) {
        if (targetRobot != null && hasEnoughAmmunition(shootBurst) && !isItObviouslyStupidToFireAt(targetRobot)) {
            if (shootBurst) {
                shootBurst(targetRobot);
            } else {
                shootSingle(targetRobot);
            }

            // only ARCHONS are interesting
            if (targetRobot.getType() == RobotType.ARCHON) {
                lastTarget = targetRobot;
                archonTarget = broadcaster.getArchonAtPosition(targetRobot.getLocation());
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
    protected boolean isItObviouslyStupidToFireAt(RobotInfo targetRobot) {
        Direction bulletDirection = me.directionTo(targetRobot.getLocation());

        // look for the teammates and trees which are in the way
        for (RobotInfo ourRobot : nearbyOurRobots) {
            if (me.distanceSquaredTo(ourRobot.getLocation()) < me.distanceSquaredTo(targetRobot.getLocation())
                    && SharedUtils.willCollide(me, ourRobot.getLocation(), ourRobot.getRadius(), bulletDirection)) {
                return true; // friendly fire!!!
            }
        }

        // trees can be destroyed by bullets, but bullets meant for the enemies!!
        for (TreeInfo tree : nearbyTrees) {
            if (me.distanceSquaredTo(tree.getLocation()) < me.distanceSquaredTo(targetRobot.getLocation())
                    && SharedUtils.willCollide(me, tree.getLocation(), tree.getRadius(), bulletDirection)) {
                return true; // waste of ammo
            }
        }

        return false;
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
            if (isDead(lastTarget)) {
                System.out.println("ARCHON is DEAD!!");
                try {
                    broadcaster.reportEnemyArchonDied(lastTarget.getID());
                } catch (GameActionException e) {
                    // well, I could not inform the mates, but it is not the end of the world... at least I don't explode!
                }
            }
        }
    }

    /**
     * Check if the given robot has already died.
     * @param robot The robot
     * @reutrn True if the robot is dead.
     */
    private boolean isDead(RobotInfo robot) {
        // well... 2 does not mean dead, but someone must be shooting at it right at the moment and will probably finish it off!!
        return robot.getHealth() <= 10;
    }

}