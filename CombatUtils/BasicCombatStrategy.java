package KSTTForTheWin.CombatUtils;

import KSTTForTheWin.Broadcasting.ArchonLocation;
import KSTTForTheWin.Broadcasting.Broadcaster;
import KSTTForTheWin.CombatUtils.SharedCombatUtils.ShotType;

import java.util.*;

import KSTTForTheWin.SharedUtils;
import KSTTForTheWin.Diff;
import KSTTForTheWin.Pathfinding;
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
    protected MapLocation safeLocation;
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
    private Diff startSquare;
    private boolean wantGuide;
    private boolean hasPath;
    private Pathfinding myPath;
    protected boolean goalChanged;
    protected int lastGoalChange;
    private int faults = 0;
    // pseudo constant
    protected float VERY_CLOSE_SQ = 20f;

    // communication with the team
    Broadcaster broadcaster;
    float walked;
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
        roundsOnTheSameSpot = 0;
        remainingRandomRounds = 0;
        walked = 0;
        try {
            startSquare = this.broadcaster.gardenerInfo.getSquareLocation(me);
            myPath = new Pathfinding(this.rc, this.broadcaster);
            //wantGuide = myPath.FindPath(rc.getLocation(), broadcaster.findNearestAction());
            hasPath = false;
            System.out.println(startSquare.dx + " " + startSquare.dy);
            
            if (rc.getType() == RobotType.LUMBERJACK) {
                wantGuide = false;
            }
            else {
                wantGuide = true;
            }
        }
        catch (Exception e) {
            System.out.println("OUCH! broadcaster cannot refresh");
        }
        rnd = new Random();
    }

    /**
     * Make one tick/step of the unit's life.
     */
    public void update() {
        //try to get some free improvement
        SharedUtils.tryShake(rc);
        SharedUtils.tryToWin(rc);
        
        // prepare for the new turn
        /*
        try {
            if (wantGuide && walked < 100) {                
                walked = SharedUtils.getOut(rc, walked, broadcaster,1); //startSquare[1]);
                System.out.println(walked);
            }
        }
        catch (Exception e){
              System.out.println("KSTT Soldier Init Exception");
             e.printStackTrace();
        }
        
        */

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
            chooseGoal();
            goalChanged = true;
        }

        // DEBUG: place a debug flag on the goal (if any)
        if (hasGoal()) {
            rc.setIndicatorDot(goal, 0, 255, 0);
        }

        // default behavior is to get closer to the global goal (if there is any)
        MapLocation currentGoal = goal;

        lookAround();

        // fight the enemies if they are nearby
        RobotInfo target = null;
        boolean hasShotAtTarget = false;

        if (shouldFight()) {
            target = chooseBestShootingTarget();
            if (target != null) {
                hasShotAtTarget = shootAt(target);
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
        
        if (target != null) {
            rc.setIndicatorDot(target.getLocation(), 255, 0, 0);
        }

        if (stuckForTooLong()) {
            remainingRandomRounds += 10; // wander around for a while to get from the dead end
        }

        // check if there is a risk of being hit by a bullet
        BulletInfo dangerousBullet = getMostDangerousBullet();
        boolean hasDodged = false;

        if (dangerousBullet != null) {
            Direction myShot = hasShotAtTarget ? me.directionTo(target.getLocation()) : null;
            hasDodged = dodgeBullet(myShot, dangerousBullet);
        }

        if (!hasDodged && !hasShotAtTarget) {
            // I am not in a fight, I can move as I wish
        	if ((goal == null || !goal.isWithinDistance(rc.getLocation(), 5F))) {
            	if (goalChanged && rc.getRoundNum() - lastGoalChange > 10) {
            		goalChanged = false;
            		lastGoalChange = rc.getRoundNum();
            		faults = 0;
            		hasPath = false;
            	}
            	
    	        if(!hasPath) { 
    	        	if (goal != null) //kontola, ze je goal nastaven
    	        		hasPath = myPath.FindPath(rc.getLocation(), goal, wantGuide);
    	        	else
    	        		hasPath = myPath.FindPath(rc.getLocation(), rc.getInitialArchonLocations(rc.getTeam().opponent())[0], wantGuide);
    	        }
    	        if (hasPath) {
    	        	int status = myPath.nextPoint(rc);
    	        	if (status == 2) {
    	        		hasPath = false;
    	        	}
    	        	else if (status == 1) {
    	        		faults++;
    	        		if (faults > 20) {
    	        			faults = 0;
    	        			hasPath = false;
    	        		}
    	        	}
    	        }
            }
            else {
            	hasPath = false;
            }
            
            if (!hasPath) {
                if (remainingRandomRounds > 0) {
                    moveRandomly();
                    remainingRandomRounds--;
                } else if (currentGoal != null) {
                    // move in the direction of the goal of this round
                    moveTowardsAGoal(currentGoal);
                }
            }
        }
    }

    /**
     * No time for reaching goals - just try to survive!
     * @param dangerousBullet The bullet which might hit me
     */
    private boolean dodgeBullet(Direction myShot, BulletInfo dangerousBullet) {
        // DEBUG: mark the bullet and the robot it endangers
        rc.setIndicatorDot(dangerousBullet.getLocation(),0,255,255);
        rc.setIndicatorLine(me, dangerousBullet.getLocation(),0,255,255);
        boolean hasDodged = false;

        try {
            Direction dodgeDirection = SharedUtils.getDodgeDirection(rc, dangerousBullet);
            if (myShot == null || Math.abs(dodgeDirection.degreesBetween(myShot)) > 45) {
                SharedUtils.tryMove(rc, dodgeDirection);
                hasDodged = true;
            }
        } catch (GameActionException e) {
            // whaaaat???!!! I might die!!
        }

        return hasDodged;
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
     * Find a target which should be targeted first.
     * @return The nearest enemy with the highest priority (by its type)
     */
    protected RobotInfo chooseBestShootingTarget() {
        RobotInfo nearest = null;
        for (RobotInfo robot : nearbyEnemyRobots) {
            if ((nearest == null ||
                    SharedCombatUtils.getTargetPriority(robot.getType()) > SharedCombatUtils.getTargetPriority(nearest.getType()) ||
                    me.distanceSquaredTo(robot.getLocation()) < me.distanceSquaredTo(nearest.getLocation())) &&
                    !SharedCombatUtils.isItObviouslyStupidToFireInDirection(me, me.directionTo(robot.getLocation()), nearbyEnemyRobots, nearbyOurRobots, nearbyTrees)) {
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

        double factor = SharedUtils.robotTypeIsDangerous(rc.getType()) ? 2 : 0.9;
        return factor * ourHP < theirHP;
    }

    /**
     * Choosing a new goal is potentially a computationally heavy operation, do not do that unless necessary.
     * @return Choose next goal when there is no goal ready yet.
     */
    protected boolean shouldChooseNewGoal() {
        return !hasGoal() || stuckForTooLong() || hasReachedGoal();
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
    private boolean shootAt(RobotInfo targetRobot) {
        boolean shot = false;

        if (targetRobot != null) {   
            Direction direction = me.directionTo(targetRobot.location);
            if (!SharedCombatUtils.isItObviouslyStupidToFireInDirection(me, direction, nearbyEnemyRobots, nearbyOurRobots, nearbyTrees)) {
                // If I shot at the robot, place a red dot on it
                if (shoot(direction)) {
                    rc.setIndicatorDot(targetRobot.getLocation(), 255, 0, 0);
                    shot = true;
                }

                // only ARCHONS are interesting
                if (targetRobot.getType() == RobotType.ARCHON) {
                    lastTarget = targetRobot;
                    archonTarget = broadcaster.getArchonAtPosition(targetRobot.getLocation());
                }
            }
        }

        return shot;
    }

    /**
     * Shoot in the given direction.
     * @return True if the robot did fire the shot(s) successfully.
     */
    protected boolean shoot(Direction direction) {
        return SharedCombatUtils.shoot(rc, direction, nearbyEnemyRobots, nearbyOurRobots, nearbyTrees);
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