package KSTTForTheWin;
import battlecode.common.*;
import battlecode.world.GameStats;

import java.util.ArrayList;
import java.util.List;

import KSTTForTheWin.Broadcasting.Broadcaster;

enum MY_TYPE {
	GARDENER,
	BUILDER,
	UNKNOWN;
}
/* Class for holding relative position of some object */
strictfp class diff {	
	float dx;
	float dy;
	Direction dir;
	diff(float dx, float dy){
		this.dx = dx; 
		this.dy = dy;
		dir = new Direction(dx, dy);
	}
	
	/**
	 * Gets coordinates difference end - start.
	 * */
	diff(MapLocation start, MapLocation end) {
		dx = end.x - start.x;
		dy = end.y - start.y;
	}
	
	/**
	 * will add relative position to center
	 * @param center Point to which diff is added
	 * @return transformed position 
	 * */
	MapLocation add(MapLocation center) {
		return center.translate(dx, dy);
	}
	MapLocation addChanged(MapLocation center, float reduction) {
		float length = (float)Math.sqrt((double)(dx*dx + dy*dy)) - reduction;
		return center.translate(dir.getDeltaX(length), dir.getDeltaY(length));
	}
}

public strictfp class GardenerPlayer {

    @SuppressWarnings("unused")

    static private Broadcaster broadcaster;
    static private MY_TYPE myType = MY_TYPE.UNKNOWN;
    static int myLife = 0;	
    static Direction dir = SharedUtils.randomDirection();
    static RobotController rc;
    /*//to nefunguje, vzdalenost rohovych je moc velka
    static diff[] treeLocations = new diff[]{
    		new diff(3.015F,1.7407F),
	    	new diff(-3.015F,-1.7407F),
			new diff(2.01F,0F),
		    new diff(1.005F,1.7407F),
		    new diff(-1.005F,1.7407F),
		    new diff(-2.01F,0F),
		    new diff(-1.005F,-1.7407F),
		    new diff(1.005F,-1.7407F)
		};
    */
    static diff[] treeLocations = new diff[]{
        	new diff(2.01F,2.01F),
        	new diff(-2.01F,2.01F),
        	new diff(-2.01F,-2.01F),
        	new diff(2.01F,-2.01F),
        	new diff(2.01F,0F),
        	new diff(0F,2.01F),
        	new diff(-2.01F,0F),
        	new diff(0F,-2.01F)
    	};
    
    static float rotation = 0;
	static void runGardener(RobotController controller) throws GameActionException {
		try {
			//initialization of the gardener
	        System.out.println("I'm a KSTT gardener!");
	        broadcaster = new Broadcaster(rc);
	        rc = controller;
	        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2F);
	        //System.out.println("Location:" + rc.getLocation().toString());
	        for(RobotInfo robot : nearbyRobots) {
	        	//System.out.println("robot_dist: " + rc.getLocation().distanceTo(robot.getLocation()) + "Type:" + robot.type.toString());
	        	
	        	if (robot.type == RobotType.ARCHON && robot.team == rc.getTeam()) {
	        		//go away from Archon
	        		dir = new Direction(robot.getLocation(), rc.getLocation());
	        		//take role
	        		if (robot.getLocation().x < rc.getLocation().x) {
	        			myType = MY_TYPE.BUILDER;
	        			System.out.println("Type = BUILDER");
	        			
	        			break;
	        		}
	        		else {
	        			myType = MY_TYPE.GARDENER;
	        			System.out.println("Type = GARDENER");
	        			break;
	        		}
	        	}
	        }
	        //if made from tree
	        if (myType == MY_TYPE.UNKNOWN) {
	        	//System.out.println("type = UNKNOWN");
	        	myType = MY_TYPE.GARDENER;
	        }
		}
		catch (Exception e) {
            System.out.println("KSTT Gardener Init Exception");
            e.printStackTrace();
        }
		
		
        // The code you want your robot to perform every round should be in this loop  
		while (true) {
	        //GARDENER:
			if (myType == MY_TYPE.GARDENER) {
		        	executeGardener();
		        }
			
	        //BUILDER
	        else {
	        	executeBuilder();
	        }
		}
    }
	
	/* Gardener execution code */
	static void executeGardener() {		
		boolean foundPlace = false;
		boolean onPlace = false;
		boolean lumberjackBuilt = false;
		MapLocation finalLocation = null;
		float baseFloat = (float)Math.random() * 2F * (float)Math.PI;
		//nema smysl si pamatovat TreeInfo, protoze se jen okopiruje
		//List<MapLocation> myTrees = new ArrayList<>();
		boolean[] builtTrees = new boolean[treeLocations.length];
		
        while (true) {    	
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {         	
            	//broadcaster.refresh(); //TODO
            	myLife++;
                // Listen for home archon's location - unused
                //int xPos = rc.readBroadcast(0);
                //int yPos = rc.readBroadcast(1);
                //MapLocation archonLoc = new MapLocation(xPos,yPos);
            	SharedUtils.tryShake(rc);
            	
            	if (!foundPlace) {
            		finalLocation = tryFindHole();
            		if (finalLocation != null) {
            			foundPlace = true;
            		}
            		else if (!lumberjackBuilt && myLife > 10 &&  rc.getTeamBullets() >= RobotType.LUMBERJACK.bulletCost) {
            			Direction lumberLoc = SharedUtils.tryFindPlace(rc, baseFloat);
        				if (lumberLoc != null && rc.canBuildRobot(RobotType.LUMBERJACK, lumberLoc)) {
        					rc.buildRobot(RobotType.LUMBERJACK, lumberLoc);
        					lumberjackBuilt = true;
        				}
            		}
        			SharedUtils.tryMove(rc, SharedUtils.randomDirection());
            		
            	}
            	else {
            		if (!onPlace) {
                		if (rc.getLocation().distanceTo(finalLocation) >= RobotType.GARDENER.strideRadius) {
                			SharedUtils.tryMove(rc, rc.getLocation().directionTo(finalLocation));
                		}
                		else if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation, 1F)) {
                			if (rc.canMove(finalLocation)) {
                				rc.move(finalLocation);
                				onPlace = true;
                			}
                			else 
                				SharedUtils.tryMove(rc, rc.getLocation().directionTo(finalLocation));
                		}
            		}
            		else {
                		//check whether I have built all trees
                		//List<Integer> toRemove = new ArrayList<>();
                		int wantBuild = -1;
                		for (int id = 0; id < builtTrees.length; id++) {
                			//if the tree is already built
                			if (builtTrees[id] == true) {
	                			TreeInfo myTree = rc.senseTreeAtLocation(treeLocations[id].add(finalLocation));
	                			if (myTree == null) {
	                				//System.out.println("Tree disapeared!");
	                				builtTrees[id] = false;
	                			}
	                			else {
	                				if (rc.canWater() && myTree.maxHealth - myTree.health > 4.9F) {
	                					//System.out.println("Want water:" + myTree.getLocation().toString() + ",dist = " + rc.getLocation().distanceTo(myTree.getLocation()));
	                					if (rc.canWater(myTree.ID))
	                						rc.water(myTree.ID);
	                				}
	                			}
                			}
                			//tree not yet built
                			else if (rc.isBuildReady() && rc.getTeamBullets() >= GameConstants.BULLET_TREE_COST) {
                				if (wantBuild < 0 && !rc.isCircleOccupiedExceptByThisRobot(treeLocations[id].add(finalLocation), 1F)) {
                					wantBuild = id;
                				}
                			}
                		}
                		if (wantBuild >= 0) {
                			//primy posun
                			if (!rc.isCircleOccupiedExceptByThisRobot(treeLocations[wantBuild].addChanged(finalLocation, 2F), 1F) &&
                					rc.canMove(treeLocations[wantBuild].addChanged(finalLocation, 2F))) {
                				rc.move(treeLocations[wantBuild].addChanged(finalLocation, 2F));
                				if (rc.getLocation().distanceTo(treeLocations[wantBuild].addChanged(finalLocation, 2F)) < 0.005) {
                					rc.plantTree(treeLocations[wantBuild].dir);
                					builtTrees[wantBuild] = true;
                				}
                			}
                			//TODO rozbìhat
                			//kolmo zprava
                			else if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation.translate(treeLocations[wantBuild].dx, 0.01F), 1F) && 
                					treeLocations[wantBuild].dy > 0 && rc.canMove(finalLocation.translate(treeLocations[wantBuild].dx, 0.01F))) {
                				rc.move(finalLocation.translate(treeLocations[wantBuild].dx,  0.01F));
                				if (rc.getLocation().distanceTo(finalLocation.translate(treeLocations[wantBuild].dx,  0)) < 0.005) {
                					rc.plantTree(Direction.NORTH);
                					builtTrees[wantBuild] = true;
                				}
                			}
                			
                			//kolmo zleva
                			else if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation.translate(treeLocations[wantBuild].dx, -0.01F), 1F) && 
                					treeLocations[wantBuild].dy < 0 && rc.canMove(finalLocation.translate(treeLocations[wantBuild].dx, -0.01F))) {
                				rc.move(finalLocation.translate(treeLocations[wantBuild].dx, -0.01F));
                				if (rc.getLocation().distanceTo(finalLocation.translate(treeLocations[wantBuild].dx, -0.001F)) < 0.005) {
                					rc.plantTree(Direction.SOUTH);
                					builtTrees[wantBuild] = true;
                				}
                			}
                			
                			//kolmo shora
                			else if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation.translate(0.01F, treeLocations[wantBuild].dy), 1F) && 
                					treeLocations[wantBuild].dx > 0 && rc.canMove(finalLocation.translate(0.01F, treeLocations[wantBuild].dy))) {
                				rc.move(finalLocation.translate(0.01F, treeLocations[wantBuild].dy));
                				if (rc.getLocation().distanceTo(finalLocation.translate(0.01F, treeLocations[wantBuild].dy)) < 0.005) {
                					rc.plantTree(Direction.EAST);
                					builtTrees[wantBuild] = true;
                				}
                			}
                			//kolmo zdola
                			else if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation.translate(-0.01F, treeLocations[wantBuild].dy), 1F) && 
                					treeLocations[wantBuild].dx < 0 && rc.canMove(finalLocation.translate(-0.01F, treeLocations[wantBuild].dy))) {
                				rc.move(finalLocation.translate(-0.01F, treeLocations[wantBuild].dy));
                				if (rc.getLocation().distanceTo(finalLocation.translate(-0.01F, treeLocations[wantBuild].dy)) < 0.005) {
                					rc.plantTree(Direction.WEST);
                					builtTrees[wantBuild] = true;
                				}
                			}
                			
                			//jinak se vrat na start
                			else if (rc.canMove(finalLocation)) {
                				rc.move(finalLocation);
                			}
                		}
            		}
            	}
                
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("KSTT Gardener Exception");
                e.printStackTrace();
            }
        }
	}
	
	/* Builder execution code */
	static void executeBuilder() {
		float baseFloat = dir.radians;
	    while (true) {    	
        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {         	
            	//broadcaster.refresh();
            	myLife++;
                // Listen for home archon's location - unused (yet)
                //int xPos = rc.readBroadcast(0);
                //int yPos = rc.readBroadcast(1);
                //MapLocation archonLoc = new MapLocation(xPos,yPos);
            	SharedUtils.tryShake(rc);
                if (myLife < 5) {
                	SharedUtils.tryMove(rc, dir);
                }
                
                else {
                	//can build, so maybe do it (TODO)
                	if (rc.isBuildReady() && rc.getTeamBullets() > 170) { 
                		
                		//look around for empty place for non-tank unit
                		Direction freeDir = SharedUtils.tryFindPlace(rc, baseFloat);
                		
                		//build it!
                		if (freeDir != null) {       			
      		                // Randomly attempt to build a unit in this direction
      		                if (rc.canBuildRobot(RobotType.SOLDIER, freeDir) && Math.random() < .01) {
      		                    rc.buildRobot(RobotType.SOLDIER, freeDir);
      		                } 
      		                else if (rc.canBuildRobot(RobotType.LUMBERJACK, freeDir) && Math.random() < .001) {
      		                    rc.buildRobot(RobotType.LUMBERJACK, freeDir);
      		                }
      		                
      		                else if (rc.canBuildRobot(RobotType.SCOUT, freeDir) && Math.random() < .005) {
    		                    rc.buildRobot(RobotType.SCOUT, freeDir);
    		                }
      		                //tank only sometimes possible
      		                else if (rc.canBuildRobot(RobotType.TANK, freeDir) && Math.random() < .01) {
  		                        rc.buildRobot(RobotType.TANK, freeDir);
  		                    }
                		} 	                	
                	}
                	SharedUtils.tryMove(rc, SharedUtils.randomDirection());
                }
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            
            } catch (Exception e) {
                System.out.println("KSTT Gardener Exception");
                e.printStackTrace();
            }
        }
	}
	
	static MapLocation tryFindHole() throws GameActionException {
		float range = 3.05F;
		for (int i = 0; i < 50; i++) {
			dir = SharedUtils.randomDirection();
			float dist = (float)(Math.random() * (RobotType.GARDENER.sensorRadius - range));
			//TODO
			if (!rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(dir, dist), range) && rc.onTheMap(rc.getLocation().add(dir, dist), range)) {
				return rc.getLocation().add(dir, dist);
			}
			
		}
		return null;
	}
}


