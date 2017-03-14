package KSTTForTheWin.Broadcasting;
import battlecode.common.*;
import KSTTForTheWin.Diff;

public strictfp class GardenerPlacementInfo {
    @SuppressWarnings("unused")

    public RobotController rc;

    public MapLocation originPoint;
    public Direction originDirection;

    public enum BuildDirection { RIGHT, LEFT }; // 0, 1
    public BuildDirection buildDirection;
    public int lineNumber = 0;
    public int positionNumber = 0;
    public MapLocation currentTarget;
    public float[] walls = new float[Broadcaster.TOTAL_WALLS];
    public int targets = 0;
    public int removes = 0;
    public int reaquires = 0;
    public boolean[][] usedTargets = new boolean[20][21];
    public int gardenerCount = 0;
    //public int[] pingVals;
    public static final float ACQUIRE_DISTANCE = 5.66f; // jak daleko od cile zahradnik ohlasi, ze to misto je jeho
    public static final float LINE_DISTANCE = 8.04f; // jak daleko jsou od sebe linie, idealne minimum 6.03f + aby prosel robot
    public static final float NEIGHBOUR_DISTANCE = 8.04f; // jak daleko jsou od sebe body v ramci jedne linie, minimum 6.03f, jinak 7.08f i diagonalne + aby prosel robot? (viz geometrie kruhu a ctverce)
    public static final float RADIUS_ON_MAP = 4.2f; // jaky polomer kruhu musi byt na mape v okoli bodu, idealne minimum 7.08f/2f
    // POZNAMKA!! Hodnotu RADIUS_ON_MAP je mozne snizovat, ale pak je potreba NEIGHBOUR_DISTANCE zvysovat,
    //            aby bylo mozne kolem stromku prochazet s jinymi jednotkami!
    //            Funguje i naopak - RADIUS_ON_MAP muzeme zvysit a NEIGHBOUR_DISTANCE snizit.
    //            RADIUS_ON_MAP nesmi byt vetsi nez rozhled zahradnika a je treba brat v uvahu i ACQUIRE_DISTANCE
    // JAKOUKOLI ZMENU VYZKOUSET NA MAPE Alone!!

    public GardenerPlacementInfo(RobotController rc) {
        this.rc = rc;
    }

    public void initialize(MapLocation point, Direction direction) throws GameActionException {
        originPoint = point;
        originDirection = direction;
        currentTarget = originPoint;
        rc.broadcastFloat(Broadcaster.GARDENER_ORIGIN_POINT, point.x);
        rc.broadcastFloat(Broadcaster.GARDENER_ORIGIN_POINT + 1, point.y);
        rc.broadcastFloat(Broadcaster.GARDENER_ORIGIN_DIRECTION, direction.radians);
        rc.broadcastInt(Broadcaster.GARDENER_BUILD_DIRECTION, 0);
        rc.broadcastInt(Broadcaster.GARDENER_LINE_NUMBER, lineNumber);
        rc.broadcastInt(Broadcaster.GARDENER_POSITION_NUMBER, positionNumber);
        rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET, currentTarget.x);
        rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET + 1, currentTarget.y);
    }

    public void refresh() throws GameActionException {
        originPoint = new MapLocation(rc.readBroadcastFloat(Broadcaster.GARDENER_ORIGIN_POINT), rc.readBroadcastFloat(Broadcaster.GARDENER_ORIGIN_POINT + 1));
        originDirection = new Direction(rc.readBroadcastFloat(Broadcaster.GARDENER_ORIGIN_DIRECTION));
        buildDirection = rc.readBroadcastInt(Broadcaster.GARDENER_BUILD_DIRECTION) == 1 ? BuildDirection.LEFT : BuildDirection.RIGHT;
        lineNumber = rc.readBroadcast(Broadcaster.GARDENER_LINE_NUMBER);
        positionNumber = rc.readBroadcast(Broadcaster.GARDENER_POSITION_NUMBER);
        currentTarget = originPoint.add(originDirection.rotateLeftDegrees(90f), NEIGHBOUR_DISTANCE*(positionNumber + 0.5F)).add(originDirection, LINE_DISTANCE*(lineNumber + 0.5F));
        //download new positions
        int newTargets = rc.readBroadcast(Broadcaster.LOCATION_COUNT);
        for (int i = targets; i < newTargets; i++) {
        	int newX = rc.readBroadcast(Broadcaster.GARDENER_LOCATIONS + 2*i);
        	int newY = rc.readBroadcast(Broadcaster.GARDENER_LOCATIONS + 2*i + 1);
        	usedTargets[newX + 2][newY + 10] = true;
        }
        targets = newTargets;
    	
        //download removed positions
    	int newRemoves = rc.readBroadcast(Broadcaster.REMOVES_COUNT);
        for (int i = removes; i < newRemoves; i++) {
        	int newX = rc.readBroadcast(Broadcaster.GARDENER_REMOVES + 2*i);
        	int newY = rc.readBroadcast(Broadcaster.GARDENER_REMOVES + 2*i + 1);
        	usedTargets[newX + 2][newY + 10] = false;
        }
        removes = newRemoves;
        
        //download reaqured positions
    	int newReaquires = rc.readBroadcast(Broadcaster.REAQUIRE_COUNT);
        for (int i = reaquires; i < newReaquires; i++) {
        	int newX = rc.readBroadcast(Broadcaster.GARDENER_REAQUIRES + 2*i);
        	int newY = rc.readBroadcast(Broadcaster.GARDENER_REAQUIRES + 2*i + 1);
        	usedTargets[newX + 2][newY + 10] = true;
        }
        reaquires = newReaquires;
        
        //update walls
    	for (int i = 0; i < Broadcaster.TOTAL_WALLS; i++) {
    		walls[i] = rc.readBroadcastFloat(Broadcaster.WALL_BASE + i);
    	}
    	gardenerCount = rc.readBroadcastInt(Broadcaster.GARDENER_COUNT);
    }

    public void targetAcquired() throws GameActionException {
        //Direction targetMoveDir
    	
        //rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET, currentTarget.x);
        //rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET + 1, currentTarget.y);
        
        //gardener position broadcast
        rc.broadcast(Broadcaster.GARDENER_LOCATIONS + 2*targets, lineNumber);
        rc.broadcast(Broadcaster.GARDENER_LOCATIONS + 2*targets + 1, positionNumber);
        rc.broadcast(Broadcaster.LOCATION_COUNT, ++targets);       
        
        System.out.println(targets);
        if (buildDirection == BuildDirection.RIGHT) positionNumber++;//targetMoveDir = originDirection.rotateRightDegrees(90f);
        else positionNumber--;//targetMoveDir = originDirection.rotateLeftDegrees(90f);
        rc.broadcast(Broadcaster.GARDENER_POSITION_NUMBER, positionNumber);
        
        currentTarget = originPoint.add(originDirection.rotateLeftDegrees(90f), NEIGHBOUR_DISTANCE*(positionNumber + 0.5F)).add(originDirection, LINE_DISTANCE*(lineNumber + 0.5F));
        
        System.out.println(positionNumber + " " + walls[0] + " " + walls[1] + " " + walls[2] + " " + walls[3]);
        if ((walls[0] != 0 && (walls[0] - currentTarget.y < RADIUS_ON_MAP)) ||
        		(walls[1] != 0 && (walls[1] - currentTarget.x < RADIUS_ON_MAP)) ||
        		(walls[2] != 0 && (currentTarget.y - walls[2] < RADIUS_ON_MAP)) ||
        		(walls[3] != 0 && (currentTarget.x - walls[3] < RADIUS_ON_MAP))) {
        	//System.out.println("skip");
        	targetNotFound();
        }
        
        System.out.println("Target : " + currentTarget.x + " " + currentTarget.y);
    }
    public int getRealGardenerCount() {
    	int res = 0;
    	for (int i = 0; i < 20; i++) {
    		for (int j = 0; j < 21; j++) {
    			if (usedTargets[i][j])
    				res++;
    		}
    	}
    	return res;
    }
    public void targetNotFound() throws GameActionException {
        if (buildDirection == BuildDirection.RIGHT) {
            buildDirection = BuildDirection.LEFT;
            positionNumber = -1;
            currentTarget = originPoint.add(originDirection.rotateLeftDegrees(90f), NEIGHBOUR_DISTANCE*0.5F).add(originDirection, LINE_DISTANCE*(lineNumber + 0.5F));
            rc.broadcastInt(Broadcaster.GARDENER_BUILD_DIRECTION, 1);
            rc.broadcast(Broadcaster.GARDENER_POSITION_NUMBER, positionNumber);
        } else {
            buildDirection = BuildDirection.RIGHT;
            lineNumber++;
            positionNumber = 0;
            currentTarget = originPoint.add(originDirection.rotateRightDegrees(90f), NEIGHBOUR_DISTANCE*0.5F).add(originDirection, LINE_DISTANCE*(lineNumber + 0.5F));
            rc.broadcastInt(Broadcaster.GARDENER_BUILD_DIRECTION, 0);
            rc.broadcastInt(Broadcaster.GARDENER_LINE_NUMBER, lineNumber);
            rc.broadcast(Broadcaster.GARDENER_POSITION_NUMBER, positionNumber);
        }
    }
	//removes wrongly added target
    public void removeTarget(MapLocation loc) throws GameActionException {
    	Diff square = getSquareLocation(loc);
		usedTargets[(int)(square.dx + 10) - 8][(int)(square.dy + 10)] = false;
		rc.broadcast(Broadcaster.GARDENER_REMOVES + 2*removes, (int)(square.dx + 10) - 10);
	    rc.broadcast(Broadcaster.GARDENER_REMOVES + 2*removes + 1, (int)(square.dy + 10) - 10);
	    rc.broadcast(Broadcaster.REMOVES_COUNT, ++removes);       
    	System.out.println("Removed " + square.dx + " " + square.dy);
    }
    
    //add previously removed target
    public void addTarget(MapLocation loc) throws GameActionException {
    	Diff square = getSquareLocation(loc);
		usedTargets[(int)(square.dx + 10) - 8][(int)(square.dy + 10)] = true;
		rc.broadcast(Broadcaster.GARDENER_REAQUIRES + 2*reaquires, (int)(square.dx + 10) - 10);
	    rc.broadcast(Broadcaster.GARDENER_REAQUIRES + 2*reaquires + 1, (int)(square.dy + 10) - 10);
	    rc.broadcast(Broadcaster.REAQUIRE_COUNT, ++reaquires);       
    	System.out.println("Reaqured " + square.dx + " " + square.dy);
    }
    
    public Diff getSquareLocation(MapLocation loc) throws GameActionException {		
		refresh();		
		Diff answer = new Diff(originPoint, loc);
		answer.rotate(-originDirection.radians);
		answer.multiply(1/LINE_DISTANCE, 1/NEIGHBOUR_DISTANCE);
		return answer;
		
	}
    //returns whether target is good
    public boolean checkWall(MapLocation target) {    
    	if((Math.abs(walls[0]) > 0.001 && (walls[0] - target.y < RADIUS_ON_MAP)) ||
		(Math.abs(walls[1]) > 0.001 && (walls[1] - target.x < RADIUS_ON_MAP)) ||
		(Math.abs(walls[2]) > 0.001 && (target.y - walls[2] < RADIUS_ON_MAP)) ||
		(Math.abs(walls[3]) > 0.001 && (target.x - walls[3] < RADIUS_ON_MAP))) {
    		return false;
    	}
    	return true;
    }
    public MapLocation getMapLocation(Diff diffLoc) {
    	Diff ret = new Diff(diffLoc.dx, diffLoc.dy);
    	ret.multiply(LINE_DISTANCE, NEIGHBOUR_DISTANCE);
    	ret.rotate(originDirection.radians);
    	return ret.add(originPoint);
    }
    public void showDebugCircles() {
        rc.setIndicatorDot(currentTarget, 255, 255, 255);
        rc.setIndicatorLine(originPoint, originPoint.add(originDirection, 10.0f), 255, 255, 255);
    }
}