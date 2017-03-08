package KSTTForTheWin;
import KSTTForTheWin.Broadcasting.Broadcaster;
import battlecode.common.*;

public strictfp class ArchonPlayer {

    @SuppressWarnings("unused")

    static Broadcaster broadcaster;
    static RobotController rc;

    static boolean mainArchon = true;

    static void runArchon(RobotController rcon) throws GameActionException {
        // System.out.println("I'm a KSTT archon!");

        rc = rcon;
        broadcaster = new Broadcaster(rc);

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                int roundNum = rc.getRoundNum();
                MapLocation myLocation = rc.getLocation();

                if (roundNum <= 1) {
                    MapLocation[] enemyArchonLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
                    broadcaster.reportEnemyInitialArchons(enemyArchonLocations);
                    MapLocation[] archonLocations = rc.getInitialArchonLocations(rc.getTeam());
                    for (MapLocation loc : archonLocations) {
                        if (loc.y > myLocation.y + 0.1f) { mainArchon = false; break; }
                        if (loc.y > myLocation.y - 0.1f && loc.x < myLocation.x - 0.1f) { mainArchon = false; break; }
                    }
                    if (mainArchon) {
                        System.out.println("Archon " + rc.getID() + " is main!");
                    }
                } else {
                    broadcaster.refresh();
                    broadcaster.showDebugCircles();
                }

                if (mainArchon && roundNum % 50 == 0) {
                    buyVictoryPointsIfNeeded();
                }

                // Generate a random direction
                Direction dir = SharedUtils.randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .01) {
                    rc.hireGardener(dir);
                }

                // Move randomly
                SharedUtils.tryMove(rc, SharedUtils.randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("KSTT Archon Exception");
                e.printStackTrace();
            }
        }
    }

    static void buyVictoryPointsIfNeeded() throws GameActionException {
        int myVP = rc.getTeamVictoryPoints();
        int enemyVP = rc.getOpponentVictoryPoints();
        float bullets = rc.getTeamBullets();
        float victoryPointCost = rc.getVictoryPointCost();

        if (!shouldBuyVictoryPoints(myVP, enemyVP, bullets)) return;

        float bulletsToDonate = bullets * 0.3f;
        bulletsToDonate = (float)Math.ceil(bulletsToDonate / victoryPointCost) * victoryPointCost;

        rc.donate(bulletsToDonate);
    }

    static boolean shouldBuyVictoryPoints(int myVP, int enemyVP, float bullets) {
        return bullets > 300f && (
            (myVP - 20 < enemyVP) || bullets > 3000f
        );
    }
}
