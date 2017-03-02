package KSTTForTheWin.Broadcasting;
import battlecode.common.*;

public strictfp class ArchonLocation {
    @SuppressWarnings("unused")

    public MapLocation location = new MapLocation(0, 0);
    public int robotId = -1; // use id -1 for undetermined (initial locations)

    public boolean isDead() {
        return location.x < -250f;
    }
}