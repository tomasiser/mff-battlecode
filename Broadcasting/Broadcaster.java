package KSTTForTheWin.Broadcasting;
import battlecode.common.*;

import java.util.Random;

import KSTTForTheWin.Diff;

public strictfp class Broadcaster {

    /**
     * channel                         data
     * -------                         ----
     * 
     * ARCHON_COUNT_CHANNEL            enemy archon count
     * 
     * ARCHON_FIRST_CHANNEL + 0        enemy archon robot id
     * ARCHON_FIRST_CHANNEL + 1        enemy archon pos x (negative if dead)
     * ARCHON_FIRST_CHANNEL + 2        enemy archon pos y
     * ARCHON_FIRST_CHANNEL + 3        enemy archon robot id
     * ARCHON_FIRST_CHANNEL + 4        enemy archon pos x (negative if dead)
     * ARCHON_FIRST_CHANNEL + 5        enemy archon pos y
     * ARCHON_FIRST_CHANNEL + 6        enemy archon robot id
     * ARCHON_FIRST_CHANNEL + 7        enemy archon pos x (negative if dead)
     * ARCHON_FIRST_CHANNEL + 8        enemy archon pos y
     * 
     * HELP_NEEDED_FIRST_CHANNEL + 0   help needed round
     * HELP_NEEDED_FIRST_CHANNEL + 1   help needed x
     * HELP_NEEDED_FIRST_CHANNEL + 2   help needed y
     * HELP_NEEDED_FIRST_CHANNEL + 3   help needed round
     * HELP_NEEDED_FIRST_CHANNEL + 4   help needed x
     * HELP_NEEDED_FIRST_CHANNEL + 5   help needed y
     */

    @SuppressWarnings("unused")

    static final int MAX_ARCHONS = 3;
    static final int MAX_HELP_NEEDED = 2;

    static final int ARCHON_LOC_LEN = 3;
    static final int ARCHON_FIRST_CHANNEL = 1;
    static final int HELP_NEEDED_FIRST_CHANNEL = MAX_ARCHONS * ARCHON_LOC_LEN;
    static final int ARCHON_COUNT_CHANNEL = 0;

    static final int GARDENER_ORIGIN_POINT = 50; // 2
    static final int GARDENER_ORIGIN_DIRECTION = 52;
    static final int GARDENER_CURRENT_TARGET = 53; // 2
    static final int GARDENER_BUILD_DIRECTION = 55;
    static final int GARDENER_LINE_NUMBER = 56;
    static final int GARDENER_POSITION_NUMBER = 57;

    static final int LOCATION_COUNT = 1490;
    static final int GARDENER_LOCATIONS = 1500; 
    static final int REMOVES_COUNT = 1990;
    static final int GARDENER_REMOVES = 2000;   
    static final int REAQUIRE_COUNT = 2290;
    static final int GARDENER_REAQUIRES = 2300;
    public static final int GARDENER_COUNT = 2500;
    
    public static final int EMPTY_PLACE_COUNT = 2999;
    public static final int EMPTY_PLACE_REPORTS = 3000;

    static final int TOTAL_WALLS = 4;
    static final int WALL_BASE = 8000; //up->right->down->left
    RobotController rc;

    int enemyArchonCount = 0;

    ArchonLocation[] archonLocations = new ArchonLocation[MAX_ARCHONS];
    HelpNeededLocation[] helpNeededLocations = new HelpNeededLocation[MAX_HELP_NEEDED];
    
    
    public GardenerPlacementInfo gardenerInfo;

    public Broadcaster(RobotController rc) {
        this.rc = rc;
        gardenerInfo = new GardenerPlacementInfo(rc);
        for (int i = 0; i < MAX_ARCHONS; ++i) {
            archonLocations[i] = new ArchonLocation();
        }
        for (int i = 0; i < MAX_HELP_NEEDED; ++i) {
            helpNeededLocations[i] = new HelpNeededLocation();
        }
    }

    public void refresh() throws GameActionException {
        refreshHelpNeededLocations();
        refreshArchonLocations();
    }

    public void refreshArchonLocations() throws GameActionException {
        if (enemyArchonCount < 1) {
            enemyArchonCount = rc.readBroadcastInt(ARCHON_COUNT_CHANNEL);
            if (enemyArchonCount > MAX_ARCHONS) enemyArchonCount = MAX_ARCHONS;
        }
        for (int i = 0; i < enemyArchonCount; ++i) {
            archonLocations[i].robotId = rc.readBroadcastInt(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i);
            archonLocations[i].location = new MapLocation(
                rc.readBroadcastFloat(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i + 1),
                rc.readBroadcastFloat(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i + 2)
            );
        }
    }
    
    
    public void reportWall(int dir) throws GameActionException {
    	//uz byla reportovana
    	if (gardenerInfo.walls[dir] != 0)
    		return;
    	float dx = 0;
    	float dy = 0;
    	switch(dir) {
    		case 0:
    			dy = 1;
    			break;
    		case 1:
    			dx = 1;
    			break;
    		case 2:
    			dy = -1;
    			break;
    		case 3:
    			dx = -1;
    			break;
    	}
    	float dist = rc.getType().sensorRadius - 0.1F;
    	//pokud konec nevidim, nic nebudu delat
    	if (rc.onTheMap(rc.getLocation().translate(dx*dist, dy*dist)))
    		return;
    	
    	//hledam kraj
    	while (!rc.onTheMap(rc.getLocation().translate(dx*dist, dy*dist))) {
    		dx *= 0.9F;
    		dy *= 0.9F;
    	}
    	while (rc.onTheMap(rc.getLocation().translate(dx*dist, dy*dist))) {
    		dx *= 1.005F;
    		dy *= 1.005F;
    	}
    	//broadcast result
    	if (dir == 0 || dir == 2)
    		rc.broadcastFloat(WALL_BASE + dir, rc.getLocation().y + dy*dist);
    	else
    		rc.broadcastFloat(WALL_BASE + dir, rc.getLocation().x + dx*dist);
    	
    	return;
    }
    
    public void tryReportEmptyPlace() throws GameActionException {
    	Diff myLoc = gardenerInfo.getSquareLocation(rc.getLocation());
    	for (int id = 0; id < 4; id++) {
	    	Diff squareLoc = myLoc;
	    	int X = 0;
	    	int Y = 0;
	    	switch(id) {
	    		case 0:
	    			X = (int)(squareLoc.dx + 10) - 11;
	    			Y = (int)(squareLoc.dy + 10) - 11;
	    			break;
	    		case 1:
	    			X = (int)(squareLoc.dx + 10) - 10;
	    			Y = (int)(squareLoc.dy + 10) - 11;
	    		case 2:
	    			X = (int)(squareLoc.dx + 10) - 11;
	    			Y = (int)(squareLoc.dy + 10) - 10;
	    		case 3:
	    			X = (int)(squareLoc.dx + 10) - 10;
	    			Y = (int)(squareLoc.dy + 10) - 10;
	    	}
	    	MapLocation endLocation = gardenerInfo.getMapLocation(new Diff(X + 0.5F, Y + 0.5F));
	    	if (!gardenerInfo.usedTargets[X + 2][Y + 10]) {
	    		continue;
	    	}
	    	if (rc.canSenseAllOfCircle(endLocation, 2F)) {
	    		if (rc.getType() == RobotType.GARDENER && rc.getLocation().distanceTo(endLocation) < 2.5F) //dont skip myself
	    			continue;
	    		RobotInfo[] robots = rc.senseNearbyRobots(endLocation, 2F, rc.getTeam());
	    		boolean found = false;
	    		for (RobotInfo robot : robots) {
	    			if (robot.type == RobotType.GARDENER) {
	    				found = true;
	    				break;
	    			}
	    		}
	    		if (found) {
	    			continue;
	    		}
	    		//gardener not found, report the place
		    	int total = rc.readBroadcastInt(EMPTY_PLACE_COUNT);
		    	for (int i = 0; i < total; i++) {
		    		if (rc.readBroadcastInt(EMPTY_PLACE_REPORTS + 2*i) == X && rc.readBroadcastInt(EMPTY_PLACE_REPORTS + 2*i + 1) == Y) //reported already
		    			return;
		    	}
		    	//not yet reported, therefore report!
		    	rc.broadcastInt(EMPTY_PLACE_REPORTS + 2*total, X);
		    	rc.broadcastInt(EMPTY_PLACE_REPORTS + 2*total + 1, Y);
		    	rc.broadcastInt(EMPTY_PLACE_COUNT, total + 1);
		    	gardenerInfo.removeTarget(endLocation);
		    	System.out.println("Reported" + endLocation.toString() + ", " + X + " " + Y);
	    	}
    	}
    }
    public void removeEmptyPlace(MapLocation loc) throws GameActionException {
    	Diff squareLoc = gardenerInfo.getSquareLocation(loc);
    	int X = (int)(squareLoc.dx + 10) - 10;
    	int Y = (int)(squareLoc.dy + 10) - 10;
    	int total = rc.readBroadcastInt(EMPTY_PLACE_COUNT);
    	int i = 0;
    	while (i < total && (rc.readBroadcastInt(EMPTY_PLACE_REPORTS + 2*i) != X || rc.readBroadcastInt(EMPTY_PLACE_REPORTS + 2*i + 1) != Y)) {
    		i++;
    	}
    	if (i == total) { //was removed already
    		return; 
    	}
    	//move last report to this place
    	if (i < total - 1) {
    		int newX = rc.readBroadcastInt(EMPTY_PLACE_REPORTS + 2*total);
    		int newY = rc.readBroadcastInt(EMPTY_PLACE_REPORTS + 2*total + 1);
    		rc.broadcast(EMPTY_PLACE_REPORTS + 2*i, newX);
    		rc.broadcast(EMPTY_PLACE_REPORTS + 2*i + 1, newY);
    	}
    
    	//remove
    	rc.broadcastInt(EMPTY_PLACE_COUNT, total - 1);
    	gardenerInfo.addTarget(loc);
    }
    public void reportEnemyArchonCount(int howManyArchons) throws GameActionException {
        rc.broadcastInt(ARCHON_COUNT_CHANNEL, howManyArchons);
        this.enemyArchonCount = howManyArchons;
    }

    public void reportEnemyInitialArchons(MapLocation[] locations) throws GameActionException {
        reportEnemyArchonCount(locations.length);
        for (int i = 0; i < enemyArchonCount; ++i) {
            rc.broadcastInt(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i, -1);
            rc.broadcastFloat(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i + 1, locations[i].x);
            rc.broadcastFloat(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i + 2, locations[i].y);
        }
    }

    public void reportEnemyArchon(int archonRobotId, MapLocation location) throws GameActionException {
        for (int i = 0; i < enemyArchonCount; ++i) {
            if (archonLocations[i].robotId == -1 || archonLocations[i].robotId == archonRobotId) {
                if (archonLocations[i].robotId == archonRobotId && location.isWithinDistance(archonLocations[i].location, 2.f)) return;
                if (archonLocations[i].robotId == -1) rc.broadcastInt(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i, archonRobotId);
                rc.broadcastFloat(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i + 1, location.x);
                rc.broadcastFloat(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i + 2, location.y);
                return;
            }
        }
    }

    public void reportEnemyArchonDied(int archonRobotId) throws GameActionException {
        for (int i = 0; i < enemyArchonCount; ++i) {
            if (archonLocations[i].robotId == archonRobotId) {
                rc.broadcastFloat(ARCHON_FIRST_CHANNEL + ARCHON_LOC_LEN * i + 1, -500f);
            }
        }
    }

    public void refreshHelpNeededLocations() throws GameActionException {
        for (int i = 0; i < MAX_HELP_NEEDED; ++i) {
            int roundNumber = rc.readBroadcastInt(HELP_NEEDED_FIRST_CHANNEL + ARCHON_LOC_LEN * i);
            if (helpNeededLocations[i].roundNumber == roundNumber) return;
            helpNeededLocations[i].roundNumber = roundNumber;
            helpNeededLocations[i].location = new MapLocation(
                rc.readBroadcastFloat(HELP_NEEDED_FIRST_CHANNEL + ARCHON_LOC_LEN * i + 1),
                rc.readBroadcastFloat(HELP_NEEDED_FIRST_CHANNEL + ARCHON_LOC_LEN * i + 2)
            );
        }
    }

    public void reportHelpNeeded() throws GameActionException {
        int roundNumber = rc.getRoundNum();
        MapLocation location = rc.getLocation();

        int oldestIndex = 0;
        int lowestRoundNumber = 999999;
        for (int i = 0; i < MAX_HELP_NEEDED; ++i) {
            if (helpNeededLocations[i].roundNumber < lowestRoundNumber) {
                oldestIndex = i;
                lowestRoundNumber = helpNeededLocations[i].roundNumber;
            }
        }
        rc.broadcastInt(HELP_NEEDED_FIRST_CHANNEL + ARCHON_LOC_LEN * oldestIndex, roundNumber);
        rc.broadcastFloat(HELP_NEEDED_FIRST_CHANNEL + ARCHON_LOC_LEN * oldestIndex + 1, location.x);
        rc.broadcastFloat(HELP_NEEDED_FIRST_CHANNEL + ARCHON_LOC_LEN * oldestIndex + 2, location.y);
    }

    public MapLocation randomArchonLocation() {
        Double r =  Math.random() / (1.0/enemyArchonCount);
        Integer idx = r.intValue();
        return archonLocations[idx].location;
    }

    /**
     * Get the archon on the given position.
     * @param loc The location of the aleged archon.
     * @return The archon
     */
    public ArchonLocation getArchonAtPosition(MapLocation loc) {
        for (ArchonLocation archon : archonLocations) {
            if (archon.location.equals(loc)) {
                return archon;
            }
        }

        return null;
    }

    /**
     * Find the nearest position where some action is needed.
     * @return The position of the nearest action.
     */
    public MapLocation findNearestAction() {
        MapLocation origin = rc.getLocation();
        MapLocation nearestHelp = findNearestHelp();
        MapLocation nearestArchon = findNearestArchon();

        if (nearestArchon == null && nearestHelp == null) {
            return rc.getInitialArchonLocations(rc.getTeam().opponent())[0]; // when no target is known, go to enemy archon initial position
        } else if (nearestArchon == null) { // when some of the targets is not present, just return the other one
            return nearestHelp; // is not null
        } else if (nearestHelp == null) {
            return nearestArchon; // is not null
        }

        if (origin.distanceSquaredTo(nearestHelp) < origin.distanceSquaredTo(nearestArchon)) {
            return nearestHelp;
        } else {
            return nearestArchon;
        }
    }

    /**
     * Find the nearest point the unit can defend.
     * @return The nearest position where some mate needs help or null.
     */
    public MapLocation findNearestHelp() {
        MapLocation origin = rc.getLocation();
        MapLocation nearestAction = null;
        float nearestDistance = 9999f;
        int roundNumber = rc.getRoundNum();

        for (int i = 0; i < MAX_HELP_NEEDED; ++i) {
            HelpNeededLocation loc = helpNeededLocations[i];
            if (loc.hasExpired(roundNumber)) continue;
            float distance = loc.location.distanceSquaredTo(origin);
            if (nearestAction == null || distance < nearestDistance) {
                nearestAction = loc.location;
                nearestDistance = distance;
            }
        }

        return nearestAction;
    }

    /**
     * Find the nearest known position the unit can attack.
     * @return The position of the archon or null.
     */
    public MapLocation findNearestArchon() {
        MapLocation origin = rc.getLocation();
        MapLocation nearestAction = null;
        float nearestDistance = 9999f;

        for (int i = 0; i < enemyArchonCount; ++i) {
            ArchonLocation loc = archonLocations[i];
            if (loc.isDead()) continue;
            float distance = loc.location.distanceSquaredTo(origin);
            if (nearestAction == null || distance < nearestDistance) {
                nearestAction = loc.location;
                nearestDistance = distance;
            }
        }

        return nearestAction;
    }

    public void showDebugCircles() {/*
        int roundNumber = rc.getRoundNum();

        for (int i = 0; i < MAX_HELP_NEEDED; ++i) {
            HelpNeededLocation loc = helpNeededLocations[i];
            if (loc.hasExpired(roundNumber)) continue;
            rc.setIndicatorDot(loc.location, 0, 255, 0);
        }
        for (int i = 0; i < enemyArchonCount; ++i) {
            ArchonLocation loc = archonLocations[i];
            if (loc.isDead()) continue;
            rc.setIndicatorDot(loc.location, 255, 0, 0);
        }*/
    }
    
    public void addGardener() throws GameActionException {
    	int total = rc.readBroadcastInt(GARDENER_COUNT);
    	rc.broadcast(GARDENER_COUNT, total + 1);
    }
}
