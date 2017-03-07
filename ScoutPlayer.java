package KSTTForTheWin;
import KSTTForTheWin.Broadcasting.Broadcaster;
import battlecode.common.*;

import java.awt.*;

import static KSTTForTheWin.ScoutPlayer.states.EXPLORE;
import static KSTTForTheWin.SharedUtils.*;

/**
 * This robot is driven by a state machine.
 */

public strictfp class ScoutPlayer {
    enum states {
        EXPLORE, TRACK
    }

    final static double safeRadius = 10.0;

    @SuppressWarnings("unused")
    static Direction dir = randomDirection();
    static int trackedArchon = 0;
    static states state = EXPLORE;

    //// See if there are any nearby enemy robots
    //RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
    //MapLocation myLocation = rc.getLocation();

    static Broadcaster broadcaster;
    static BulletInfo[] bullets;
    static RobotInfo[] robots;

	static void runScout(RobotController rc) throws GameActionException {
        System.out.println("I'm a KSTT scout!");
        broadcaster = new Broadcaster(rc);
        state = EXPLORE;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            refreshCache(rc);

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                switch(state)
                {
                    case EXPLORE:
                        explore(rc);
                    case TRACK:
                        track(rc);
                }

                Clock.yield();

            } catch (Exception e) {
                System.out.println("KSTT Scout Exception");
                e.printStackTrace();
            }
        }
    }

    static void refreshCache(RobotController rc) throws GameActionException
    {
        broadcaster.refresh(); //not needed (yet)
        bullets = rc.senseNearbyBullets();
        robots = rc.senseNearbyRobots();
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
            if(nearestCollidingBullet != null &&  distance < nearestCollisionDistance)
            {
                if(willCollideWithMe(rc, bullet))
                {
                    nearestCollisionDistance = distance;
                    nearestCollidingBullet = bullet;

                }
            }
        }
        if(nearestCollidingBullet != null)
        {
            tryMove(rc, getDodgeDirection(rc, nearestCollidingBullet));
            return true;
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
            Direction newDir = nearestEnemy.getLocation().directionTo(myLocation);
            tryMove(rc, newDir);
            return true;
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

    // The code you want your robot to perform every round should be in this loop
    static void explore(RobotController rc) throws GameActionException
    {
        observe(rc);

        if (Math.random() < .01)
            dir = randomDirection();


        if(!avoidDangers(rc) && !tryMove(rc, dir))
        {
            dir = randomDirection();
            tryMove(rc, dir);
        }
    }

    static void track(RobotController rc)  throws GameActionException
    {

    }

}
