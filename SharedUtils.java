package KSTTForTheWin;
import battlecode.common.*;

public strictfp class SharedUtils {

    @SuppressWarnings("unused")

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    public static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    public static Direction randomLeftRightDirection(boolean right) {
        float angle = 0f;
        if (right) angle = (float)Math.random() * (float)Math.PI / 2f;
        else angle = (float)Math.PI / 2f + (float)Math.random() * (float)Math.PI / 2f;
        if (Math.random() < .5) angle *= -1f;
        return new Direction(angle);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryMove(RobotController rc, Direction dir) throws GameActionException {
        return tryMove(rc, dir, 20, 3);
    }

    static boolean trySimpleMove(RobotController rc, Direction dir) throws GameActionException {
      if(rc.canMove(dir))
      {
          try {
              rc.move(dir);
          }
       catch (Exception e) {
            System.out.println("Move exception");
            e.printStackTrace();
        }
        return true;
      }
      else
        return false;
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryMove(RobotController rc, Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    public static boolean willCollideWithMe(RobotController rc, BulletInfo bullet) {
        return willCollideWithMe(rc, bullet, 1.0);
    }

    public static boolean willCollideWithMe(RobotController rc, BulletInfo bullet, double cf) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        return willCollide(bulletLocation, myLocation, (float) cf * rc.getType().bodyRadius, propagationDirection);
    }

    /**
     * Calculate if the aims at the target.
     * @param bulletLocation
     * @param targetLocation
     * @param targetRadius
     * @param propagationDirection
     * @return
     */
    public static boolean willCollide(MapLocation bulletLocation, MapLocation targetLocation, float targetRadius, Direction propagationDirection) {
        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(targetLocation);
        float distToRobot = bulletLocation.distanceTo(targetLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)
      
        return (perpendicularDist <= targetRadius);
    }

    static Direction getDodgeDirection(RobotController rc, BodyInfo bullet, int preferredDir)
    {
        MapLocation myLocation = rc.getLocation();
        Direction relativeDirection = bullet.getLocation().directionTo(myLocation);
        Direction candidate;
        if(preferredDir % 2 == 0)
            candidate = relativeDirection.rotateRightDegrees(90);
        else
            candidate = relativeDirection.rotateLeftDegrees(90);
        return candidate;
    }

    public static Direction getDodgeDirection(RobotController rc, BulletInfo bullet)
    {
        MapLocation myLocation = rc.getLocation();
        Direction relativeDirection = bullet.getLocation().directionTo(myLocation);
        Direction bulletDirection = bullet.getDir();
        Direction candidate;
        if(relativeDirection.radiansBetween(bulletDirection) > 0)
            candidate = relativeDirection.rotateRightDegrees(70);
        else
            candidate = relativeDirection.rotateLeftDegrees(70);
        return candidate;
    }

    public static boolean robotIsDangerous(RobotInfo info)
    {
        return robotTypeIsDangerous(info.getType());
    }

    public static boolean robotTypeIsDangerous(RobotType type)
    {
        switch (type) {
            case ARCHON:
            case GARDENER:
            case SCOUT:
                return false;
            case SOLDIER:
            case TANK:
            case LUMBERJACK:
                return true;
        }
        return false;
    }
    
    
    /**
	 * Finds first hole with radius 1 clockwise starting by baseAngle
	 * @param baseAngle Angle from which hole is searched
	 * @return found direction, or null, if not found
	 * */
    public static Direction tryFindPlace(RobotController rc, float baseAngle) throws GameActionException {
    	return tryFindPlace(rc, baseAngle, 1F);
    }
    
    /**
	 * Finds first hole with given radius clockwise starting by baseAngle
	 * @param baseAngle Angle from which hole is searched
	 * @param radius hole radius
	 * @return found direction, or null, if not found
	 * */
	public static Direction tryFindPlace(RobotController rc, float baseAngle, float radius) throws GameActionException {
		float angle = baseAngle;
		float distance = rc.getType().bodyRadius + radius + 0.01F;
		MapLocation myLocation = rc.getLocation();
		
		//look around for empty place 
		while (angle < 2*Math.PI + baseAngle) {
			if (!rc.isCircleOccupiedExceptByThisRobot(myLocation.add(angle, distance), radius) &&
					rc.onTheMap(myLocation.add(angle, distance), radius)) {
				break;
			}
			angle += 0.2;
		}
		//put it as nearby as possible
		while (!rc.isCircleOccupiedExceptByThisRobot(myLocation.add(angle, distance), radius) &&
				rc.onTheMap(myLocation.add(angle, distance), radius) && angle > baseAngle) {
			angle -= 0.01;
		}
		angle += 0.01;
		
		//check it!
		if (angle < 2*Math.PI + baseAngle) {
			return new Direction(angle);
		}
		else
			return null;
	}
	
	/**
	 * Looks around for shakeable tree (with some bullets)
	 * @param rc controller
	 * @return true, if some tree was shaken
	 * */
	public static boolean tryShake(RobotController rc) throws GameActionException {
		TreeInfo[] availbleTrees = rc.senseNearbyTrees(rc.getType().bodyRadius + 1f, Team.NEUTRAL);
		for (TreeInfo tree : availbleTrees) {
			if (tree.containedBullets > 0) {
				if (rc.canShake(tree.ID)) {
					rc.shake(tree.ID);
					return true;
				}
			}
		}
		return false;
	}
}
