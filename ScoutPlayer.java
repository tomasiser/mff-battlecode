package KSTTForTheWin;
import KSTTForTheWin.Broadcasting.Broadcaster;
import battlecode.common.*;
import KSTTForTheWin.CombatUtils.*;

import java.awt.*;

import static KSTTForTheWin.ScoutPlayer.states.EXPLORE_DIR;
import static KSTTForTheWin.ScoutPlayer.states.EXPLORE_LOC;
import static KSTTForTheWin.ScoutPlayer.states.KILL;
import static KSTTForTheWin.SharedUtils.*;

/**
 * This robot is driven by a state machine.
 */

public strictfp class ScoutPlayer {
    enum states {
        EXPLORE_DIR, EXPLORE_LOC, TRACK, KILL
    }

    final static double safeRadius = 10.0;
    final static int patience = 50;
    final static boolean allowTether = false;

    @SuppressWarnings("unused")
    static Direction dir = randomDirection();
    static MapLocation loc;
    static int trackedArchon = 0;
    static states state = EXPLORE_DIR;
    static int dodgeDir = 0;
    static boolean wantShot = false;
    static MapLocation target;
    static int bloodlust = 0;
    static int targetId = 0;
    static float lastTargetHP = 0.0f;
    //// See if there are any nearby enemy robots
    //RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
    //MapLocation myLocation = rc.getLocation();

    static Broadcaster broadcaster;
    static BulletInfo[] bullets;
    static RobotInfo[] robots;

    static void runScout(RobotController rc) throws GameActionException {
        // System.out.println("I'm a KSTT scout!");
        broadcaster = new Broadcaster(rc);
        state = EXPLORE_DIR;
        dir = randomDirection();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
            SharedUtils.tryShake(rc);
            SharedUtils.tryToWin(rc);
            
            Integer round = rc.getRoundNum();
            refreshCache(rc, round);
            
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                observe(rc);
                switch(state)
                {
                    case EXPLORE_LOC:
                        explore(rc);
                        break;
                    case EXPLORE_DIR:
                        explore(rc);
                        break;
                    case TRACK:
                        track(rc);
                        break;
                    case KILL:
                        kill(rc);
                        break;
                }
                
                if (wantShot) {
                    rc.setIndicatorDot(target, 255, 0, 0);
                    rc.setIndicatorLine(rc.getLocation(), target, 200, 0, 0);
                    Direction dir = rc.getLocation().directionTo(target);
                    if (SharedCombatUtils.shoot(rc, dir, rc.senseNearbyRobots(-1f, rc.getTeam().opponent()), rc.senseNearbyRobots(-1f, rc.getTeam()), rc.senseNearbyTrees())) {
                        wantShot = false;
                    }
                }

                Clock.yield();
            } catch (Exception e) {
                System.out.println("KSTT Scout Exception");
                e.printStackTrace();
            }
        }
    }

    static void refreshCache(RobotController rc, Integer roundNum) throws GameActionException
    {
        broadcaster.refresh();
        robots = rc.senseNearbyRobots();
        bullets = rc.senseNearbyBullets();
    }

    static boolean avoidDangers(RobotController rc) throws GameActionException
    {
        MapLocation myLocation = rc.getLocation();

        /* check for bullets */
        double nearestCollisionDistance = 1000;
        BulletInfo nearestCollidingBullet = null;
        for(BulletInfo bullet : bullets)
        {
            double distance = bullet.getLocation().distanceTo(myLocation);
            if(distance < nearestCollisionDistance)
            {
                if(willCollideWithMe(rc, bullet, 10.0))
                {
                    nearestCollisionDistance = distance;
                    nearestCollidingBullet = bullet;

                }
            }
        }
        if(nearestCollidingBullet != null)
        {
            return trySimpleMove(rc, getDodgeDirection(rc, nearestCollidingBullet));
        }

        /*check for robots*/
        double nearestEnemyDistance = 1000;
        RobotInfo nearestEnemy = null;
        for(RobotInfo robot : robots)
        {
            MapLocation robotLocation = robot.getLocation();
            double distance = robotLocation.distanceTo(myLocation);
            if(robot.getTeam() != rc.getTeam()
                    && robotIsDangerous(robot)
                    && distance < safeRadius
                    && distance < nearestEnemyDistance)
            {
                nearestEnemyDistance = distance;
                nearestEnemy = robot;
            }
        }
        if(nearestEnemy != null)
        {
            Direction newDir;
            if(nearestEnemyDistance > safeRadius - 2.0)
                newDir = getDodgeDirection(rc, nearestEnemy, dodgeDir);
            else
                newDir = nearestEnemy.getLocation().directionTo(myLocation);
            return trySimpleMove(rc, newDir);
        }
        return false;
    }

    static void observe(RobotController rc) throws GameActionException
    {
        int own = 0;
        RobotInfo gardener = null;
        MapLocation myLocation = rc.getLocation();
        double minDist = 1000;
        for(RobotInfo robot : robots)
        {
            if(robot.getTeam() != rc.getTeam()) {
                if (robot.getType() == RobotType.ARCHON) {
                    broadcaster.reportEnemyArchon(robot.ID, robot.getLocation());
                    if (!wantShot) {
                        wantShot = true;
                        target = robot.location;
                    }
                } else if (robotIsDangerous(robot)) {
                    broadcaster.reportHelpNeeded();
                    wantShot = true;
                    target = robot.location;
                }
                else if (robot.getType() == RobotType.GARDENER) {
                    if (!wantShot) {
                        wantShot = true;
                        target = robot.location;
                    }
                    if(myLocation.distanceTo(robot.getLocation()) < minDist)
                    {
                       gardener = robot;
                       minDist = myLocation.distanceTo(robot.getLocation());
                    }
                }
            }
            else
                own++;
        }

        if(gardener != null && bloodlust > patience && state != KILL) {
            targetId = gardener.getID();
            state = KILL;
        }

        if(own > 10)
            wantShot = false;
    }

    static void pickMove(RobotController rc) throws GameActionException {
        dodgeDir++;
        if (Math.random() < .5) {
            dir = randomDirection();
            state = EXPLORE_DIR;
        }
        else {
            //loc = broadcaster.findNearestAction();
            loc = broadcaster.randomArchonLocation();
            if(loc != null) {
                state = EXPLORE_LOC;
                dir = rc.getLocation().directionTo(loc);
            }
        }
    }

    // The code you want your robot to perform every round should be in this loop
    static void explore(RobotController rc) throws GameActionException {

        if (Math.random() < .03) {
            pickMove(rc);
        }

        if (state == EXPLORE_LOC && loc != null) {
            dir = rc.getLocation().directionTo(loc);
            //rc.setIndicatorDot(loc, 0, 0, 255);
        }

        if(state == EXPLORE_LOC && rc.getLocation().distanceTo(loc) < safeRadius)
        {
            state = EXPLORE_DIR;
        }

        if (!avoidDangers(rc) && !trySimpleMove(rc, dir)) {
            pickMove(rc);
            tryMove(rc, dir);
        }

        bloodlust++;
    }

    static void kill(RobotController rc) throws GameActionException {
        if(!rc.canSenseRobot(targetId))
        {
            bloodlust = 0;
            pickMove(rc);
            explore(rc);
        }

        try {

        RobotInfo robot = rc.senseRobot(targetId);
        boolean sameHP = robot.getHealth() == lastTargetHP;
        lastTargetHP = robot.getHealth();
       // if (SharedUtils.isNear(rc.getLocation(), robot.getLocation(), robot.getRadius() + 1.5F)) {
            //pick random direction to make it harder for the enemies to shoot the scout while killing the target robot
            //rc.move(SharedUtils.randomDirection());
        /*} else*/ if( rc.canMove(rc.getLocation().directionTo(robot.getLocation())) ) {
            rc.move(rc.getLocation().directionTo(robot.getLocation()));
        } else if (allowTether || sameHP){
            Direction newDir = getDodgeDirection(rc, robot, dodgeDir);
            if(rc.canMove(newDir))
                rc.move(newDir);
            else if(rc.canMove(newDir.opposite()))
                rc.move(newDir.opposite());
        }

        wantShot = true;
        target = robot.getLocation();
        } catch (Exception e) {

        }
    }


    static void track(RobotController rc)  throws GameActionException
    {

    }

}
