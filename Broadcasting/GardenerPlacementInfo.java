package KSTTForTheWin.Broadcasting;
import battlecode.common.*;

public strictfp class GardenerPlacementInfo {
    @SuppressWarnings("unused")

    public RobotController rc;

    public MapLocation originPoint;
    public Direction originDirection;

    public enum BuildDirection { RIGHT, LEFT }; // 0, 1
    public BuildDirection buildDirection;
    public int lineNumber = 1;
    public MapLocation currentTarget;

    public static final float ACQUIRE_DISTANCE = 1f; // jak daleko od cile zahradnik ohlasi, ze to misto je jeho
    public static final float LINE_DISTANCE = 10.05f; // jak daleko jsou od sebe linie, idealne minimum 6.03f + aby prosel robot
    public static final float NEIGHBOUR_DISTANCE = 8.04f; // jak daleko jsou od sebe body v ramci jedne linie, minimum 6.03f, jinak 7.08f i diagonalne + aby prosel robot? (viz geometrie kruhu a ctverce)
    public static final float RADIUS_ON_MAP = 3.8f; // jaky polomer kruhu musi byt na mape v okoli bodu, idealne minimum 7.08f/2f
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
        rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET, currentTarget.x);
        rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET + 1, currentTarget.y);
    }

    public void refresh() throws GameActionException {
        originPoint = new MapLocation(rc.readBroadcastFloat(Broadcaster.GARDENER_ORIGIN_POINT), rc.readBroadcastFloat(Broadcaster.GARDENER_ORIGIN_POINT + 1));
        originDirection = new Direction(rc.readBroadcastFloat(Broadcaster.GARDENER_ORIGIN_DIRECTION));
        buildDirection = rc.readBroadcastInt(Broadcaster.GARDENER_BUILD_DIRECTION) == 1 ? BuildDirection.LEFT : BuildDirection.RIGHT;
        lineNumber = rc.readBroadcast(Broadcaster.GARDENER_LINE_NUMBER);
        currentTarget = new MapLocation(rc.readBroadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET), rc.readBroadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET + 1));
    }

    public void targetAcquired() throws GameActionException {
        Direction targetMoveDir;
        if (buildDirection == BuildDirection.RIGHT) targetMoveDir = originDirection.rotateRightDegrees(90f);
        else targetMoveDir = originDirection.rotateLeftDegrees(90f);

        currentTarget = currentTarget.add(targetMoveDir, NEIGHBOUR_DISTANCE);
        rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET, currentTarget.x);
        rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET + 1, currentTarget.y);

        System.out.println("Target : " + currentTarget.x + " " + currentTarget.y);
    }

    public void targetNotFound() throws GameActionException {
        if (buildDirection == BuildDirection.RIGHT) {
            buildDirection = BuildDirection.LEFT;
            currentTarget = originPoint.add(originDirection.rotateLeftDegrees(90f), NEIGHBOUR_DISTANCE);
            rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET, currentTarget.x);
            rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET + 1, currentTarget.y);
            rc.broadcastInt(Broadcaster.GARDENER_BUILD_DIRECTION, 1);
        } else {
            buildDirection = BuildDirection.RIGHT;
            lineNumber++;
            originPoint = originPoint.add(originDirection, LINE_DISTANCE);
            currentTarget = originPoint;
            rc.broadcastFloat(Broadcaster.GARDENER_ORIGIN_POINT, originPoint.x);
            rc.broadcastFloat(Broadcaster.GARDENER_ORIGIN_POINT + 1, originPoint.y);
            rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET, currentTarget.x);
            rc.broadcastFloat(Broadcaster.GARDENER_CURRENT_TARGET + 1, currentTarget.y);
            rc.broadcastInt(Broadcaster.GARDENER_BUILD_DIRECTION, 0);
            rc.broadcastInt(Broadcaster.GARDENER_LINE_NUMBER, lineNumber);
        }
    }

    public void showDebugCircles() {
        rc.setIndicatorDot(currentTarget, 255, 255, 255);
        rc.setIndicatorLine(originPoint, originPoint.add(originDirection, 10.0f), 255, 255, 255);
    }
}