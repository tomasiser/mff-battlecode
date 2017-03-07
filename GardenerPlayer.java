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

public strictfp class GardenerPlayer {

    @SuppressWarnings("unused")

    static private Broadcaster broadcaster;
    static private MY_TYPE myType = MY_TYPE.UNKNOWN;
	static void runGardener(RobotController rc) throws GameActionException {
		
		
		int myLife = 0;	
		//a direction, where the gardener wants to go
		Direction dir = SharedUtils.randomDirection();
		
		try {
			//initialization of the gardener
	        // System.out.println("I'm a KSTT gardener!");
	        broadcaster = new Broadcaster(rc);
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
        //GARDENER:
        if (myType == MY_TYPE.GARDENER) {
    		boolean foundPlace = false;
    		float baseFloat = (float)Math.random() * 2F * (float)Math.PI;
    		//nema smysl si pamatovat TreeInfo, protoze se jen okopiruje
    		List<MapLocation> myTrees = new ArrayList<>();
	        while (true) {    	
	            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
	            try {         	
	            	broadcaster.refresh();
	            	myLife++;
	                // Listen for home archon's location - unused
	                //int xPos = rc.readBroadcast(0);
	                //int yPos = rc.readBroadcast(1);
	                //MapLocation archonLoc = new MapLocation(xPos,yPos);
	                
	                if (myLife < 40) {
	                	SharedUtils.tryMove(rc, dir);
	                	if(myLife % 20 == 0) 
	                		dir = SharedUtils.randomDirection();
	                }
	                else {
	                	if (!foundPlace) {
	                		if (rc.onTheMap(rc.getLocation(), 2.5F) && rc.senseNearbyTrees(2.9F).length == 0) {
	                			foundPlace = true;
	                		}
	                		else {
	                			if (myLife % 10 == 0)
	                				dir = SharedUtils.randomDirection();
	                			SharedUtils.tryMove(rc, dir);
	                		}
	                	}
	                	else {
	                		//check whether I have all built trees
	                		List<Integer> toRemove = new ArrayList<>();
	                		for (int id = 0; id < myTrees.size(); id++) {
	                			TreeInfo myTree = rc.senseTreeAtLocation(myTrees.get(id));
	                			if (myTree == null) {
	                				//System.out.println("Tree disapeared!");
	                				toRemove.add(id);
	                			}
	                			else {
	                				//System.out.println("Tree here:" + myTree.getLocation().toString());
	                				if (rc.canWater() && myTree.maxHealth - myTree.health > 3) {
	                					rc.water(myTree.ID);
	                				}
	                			}
	                		}
	                		//remove destroyed trees
	                		if (toRemove.size() > 0) {
		                		for (int id = toRemove.size() - 1; id >= 0 ;id--) {
		                			myTrees.remove(toRemove.get(id));
		                		}
	                		}
	                		
	                		//try to plant trees, if it makes sense
		                	if (rc.isBuildReady() && rc.getTeamBullets() >= GameConstants.BULLET_TREE_COST) {
		                		float angle = 0;
		                		//look around for empty place for a tree
		                		while (angle < 2*Math.PI) {
		                			if (rc.canPlantTree(new Direction(angle + baseFloat))) {
		                				break;
		                			}
		                			angle += 0.2;
		                		}
		                		//put it as nearby as possible
		                		while (rc.canPlantTree(new Direction(angle + baseFloat)) && angle > 0) {
		                			angle -= 0.01;
		                		}
		                		angle += 0.01;
		                		
		                		//build it!
		                		if (angle < 2*Math.PI) {
		                			Direction newDir = new Direction(angle + baseFloat);
		                		    if (rc.canPlantTree(newDir)) {      			
		                		    	rc.plantTree(newDir);
		                		    	MapLocation loc = rc.getLocation().add(newDir, 2);
			                			myTrees.add(loc);
		                		    }
		                		    else {
		                		    	System.out.println("Error?" + newDir.toString());
		                		    }
		                		}       
		                		//the position is not good anymore
		                		else if (myTrees.isEmpty()) {
		                			foundPlace = false;
		                			SharedUtils.tryMove(rc, SharedUtils.randomDirection());
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
        //BUILDER
        else {
        	float baseFloat = dir.radians;
    	  while (true) {    	
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {         	
            	broadcaster.refresh();
            	myLife++;
                // Listen for home archon's location - unused (yet)
                //int xPos = rc.readBroadcast(0);
                //int yPos = rc.readBroadcast(1);
                //MapLocation archonLoc = new MapLocation(xPos,yPos);
                
                if (myLife < 5) {
                	SharedUtils.tryMove(rc, dir);
                }
                
                else {
                	//can build, so maybe do it (TODO)
                	if (rc.isBuildReady() && rc.getTeamBullets() > 180) {           		
                		//look around for empty place for non-tank unit
                		float angle = 0;
                		while (angle < 2*Math.PI) {
                			if (rc.canBuildRobot(RobotType.SOLDIER, new Direction(angle + baseFloat))) {
                				break;
                			}
                			angle += 0.2;
                		}
                		
                		//build it!
                		if (angle < 2*Math.PI) {
                			Direction newDir = new Direction(angle + baseFloat);
                			
      		                // Randomly attempt to build a unit in this direction
      		                if (rc.canBuildRobot(RobotType.SOLDIER, newDir) && Math.random() < .005) {
      		                    rc.buildRobot(RobotType.SOLDIER, newDir);
      		                } 
      		                else if (rc.canBuildRobot(RobotType.LUMBERJACK, newDir) && Math.random() < .005) {
      		                    rc.buildRobot(RobotType.LUMBERJACK, newDir);
      		                }
      		                
      		                else if (rc.canBuildRobot(RobotType.SCOUT, newDir) && Math.random() < .005) {
    		                    rc.buildRobot(RobotType.SCOUT, newDir);
    		                }
      		                //tank only sometimes possible
      		                else if (rc.canBuildRobot(RobotType.TANK, newDir) && Math.random() < .005) {
  		                        rc.buildRobot(RobotType.TANK, newDir);
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
    }
}
