package KSTTForTheWin;
import KSTTForTheWin.Broadcasting.Broadcaster;
import KSTTForTheWin.Broadcasting.GardenerPlacementInfo;
import battlecode.common.*;

public strictfp class ArchonPlayer {

    @SuppressWarnings("unused")

    static Broadcaster broadcaster;
    static RobotController rc;
    static int built = 0;
    static boolean mainArchon = true;
    //static boolean builtTreeGardener = false;
    //static boolean builtBuilderGardener = false;

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
                SharedUtils.tryShake(rc);
                SharedUtils.tryToWin(rc);

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
                        Direction toEnemy = new Direction(myLocation, enemyArchonLocations[0]);
                        broadcaster.gardenerInfo.initialize(myLocation.add(toEnemy, GardenerPlacementInfo.LINE_DISTANCE / 2f).add(toEnemy.rotateRightDegrees(90), GardenerPlacementInfo.NEIGHBOUR_DISTANCE / 2f), toEnemy);
                    }
                } else {
                    broadcaster.refresh();
                    broadcaster.gardenerInfo.refresh();
                    broadcaster.showDebugCircles();
                    broadcaster.gardenerInfo.showDebugCircles();
                }
                
                
                if (mainArchon && roundNum % 50 == 0) {
                    buyVictoryPointsIfNeeded();
                }
                if (roundNum == 2998) {
                    rc.donate(rc.getTeamBullets());
                }

                
                Direction dir;
                broadcaster.gardenerInfo.refresh();
                dir = SharedUtils.tryFindPlace(rc, broadcaster.gardenerInfo.originDirection.radians);
                /*
                if (builtTreeGardener && builtBuilderGardener) dir = SharedUtils.randomDirection();
                else if (!builtTreeGardener) dir = SharedUtils.randomLeftRightDirection(false);
                else dir = SharedUtils.randomLeftRightDirection(true);
*/

                //every 10 rounds check the number of gardeners
                if (rc.getRoundNum() % 10 == 1)
                	built = rc.readBroadcastInt(Broadcaster.GARDENER_COUNT);
                //reset it for next iteration
                if (rc.getRoundNum() % 10 == 9)
                	rc.broadcastInt(Broadcaster.GARDENER_COUNT, 0);
                // attempt to build a gardener in this direction
                if (dir!= null && rc.canHireGardener(dir) && rc.getTeamBullets() > 125 &&
                		(built - broadcaster.gardenerInfo.getRealGardenerCount()) < 2 + (rc.getRoundNum() / 500)) { 
            		built++;
                	rc.hireGardener(dir);
                    //if (!builtTreeGardener) builtTreeGardener = true;
                    //else if (!builtBuilderGardener) builtBuilderGardener = true;
                }

                // Move away from action
                Direction away = (new Direction(myLocation, broadcaster.findNearestAction())).opposite();
                SharedUtils.tryMove(rc, away);
                // if moved far enough, move the base line.
                //TODO
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("KSTT Archon Exception");
                e.printStackTrace();
            }
        }
    }

    static void buyVictoryPointsIfNeeded() throws GameActionException {
        float bulletsToDonate = 0;

        int myVP = rc.getTeamVictoryPoints();
        int enemyVP = rc.getOpponentVictoryPoints();
        float bullets = rc.getTeamBullets();
        float victoryPointCost = rc.getVictoryPointCost();

        if (!shouldBuyVictoryPoints(myVP, enemyVP, bullets)) return;

        bulletsToDonate = bullets * 0.3f;
        bulletsToDonate = (float)Math.ceil(bulletsToDonate / victoryPointCost) * victoryPointCost;
        
        rc.donate(bulletsToDonate);
    }

    static boolean shouldBuyVictoryPoints(int myVP, int enemyVP, float bullets) {
        if (rc.getRoundNum() == 2950) {
            return true;
        }

        return bullets > 300f && (
            (myVP - 20 < enemyVP) || bullets > 3000f
        );
    }
}
