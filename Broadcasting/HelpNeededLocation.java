package KSTTForTheWin.Broadcasting;
import battlecode.common.*;

public strictfp class HelpNeededLocation {
    @SuppressWarnings("unused")

    static final int EXPIRE_LIMIT = 100;

    public MapLocation location = new MapLocation(0, 0);
    public int roundNumber = 0;

    public boolean hasExpired(int currentRoundNumber) {
        return roundNumber + EXPIRE_LIMIT <= currentRoundNumber;
    }
}