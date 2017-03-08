package KSTTForTheWin;
import KSTTForTheWin.Broadcasting.Broadcaster;
import battlecode.common.*;

import java.awt.*;

import static KSTTForTheWin.ScoutPlayer.states.EXPLORE_DIR;
import static KSTTForTheWin.ScoutPlayer.states.EXPLORE_LOC;
import static KSTTForTheWin.SharedUtils.*;

/**
 * This robot is driven by a state machine.
 */

public strictfp class ScoutPlayer {
    enum states {
        EXPLORE_DIR, EXPLORE_LOC, TRACK
    }

    final static double safeRadius = 10.0;

    @SuppressWarnings("unused")
    static Direction dir = randomDirection();
    static MapLocation loc;
    static int trackedArchon = 0;
    static states state = EXPLORE_DIR;
    static int dodgeDir = 0;

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

            Integer round = rc.getRoundNum();
            refreshCache(rc, round);

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
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
        for(RobotInfo robot : robots)
        {
            if(robot.getTeam() != rc.getTeam()) {
                if (robot.getType() == RobotType.ARCHON) {
                    broadcaster.reportEnemyArchon(robot.ID, robot.getLocation());
                } else if (robotIsDangerous(robot)) {
                    broadcaster.reportHelpNeeded();
                }
            }
        }
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
        observe(rc);

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
    }


    static void track(RobotController rc)  throws GameActionException
    {

    }

}
