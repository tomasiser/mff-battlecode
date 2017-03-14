package KSTTForTheWin;
import battlecode.common.*;
import battlecode.world.GameStats;

import java.util.ArrayList;
import java.util.List;

import KSTTForTheWin.Broadcasting.Broadcaster;
import KSTTForTheWin.Broadcasting.GardenerPlacementInfo;

enum MY_TYPE {
	GARDENER,
	BUILDER,
	UNKNOWN;
}


public strictfp class GardenerPlayer {

    @SuppressWarnings("unused")

    static private Broadcaster broadcaster;
    static private MY_TYPE myType = MY_TYPE.UNKNOWN;
    //static private MapLocation parentLoc;
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
    static Diff[] treeLocations = new Diff[]{       	
    		new Diff(2.01F, 0F),
        	new Diff(2.01F, -2.01F),
        	new Diff(2.01F, 2.01F),          	 
        	new Diff(-2.01F, -2.01F),
        	new Diff(-2.01F, 2.01F),     	
        	new Diff(0F, -2.01F),
        	new Diff(0F, 2.01F)
    	};
    
    //zaloha pro hledani i pod nenulovym uhlem
    static Diff[] origtreeLocations = new Diff[]{        	
    		new Diff(2.01F, 0F),
        	new Diff(2.01F, -2.01F),
        	new Diff(2.01F, 2.01F),          	 
        	new Diff(-2.01F, -2.01F),
        	new Diff(-2.01F, 2.01F),     	
        	new Diff(0F, -2.01F),
        	new Diff(0F, 2.01F)
    	};
    
    static Diff unitHole = new Diff(-2.01F,0F);
    //static diff archonDist = new diff(8F, 0F);
    //static diff gardenerDist = new diff(0F, -6.03F);
    static float rotation = 0; //TODO
    static int unitsMade = 0;
	static void runGardener(RobotController controller) throws GameActionException {
		try {
			/*//debug test
			if (controller.getRoundNum() < 50) {
				diff test = new diff(0,2);
				test.rotate((float)Math.PI);
			    System.out.println("dx=" + test.dx + ", dy=" + test.dy);
			    test.rotate((float)Math.PI);
			    System.out.println("dx=" + test.dx + ", dy=" + test.dy);
			    test.rotate((float)Math.PI/2);
				System.out.println("dx=" + test.dx + ", dy=" + test.dy);
				test.rotate((float)Math.PI/4);
				System.out.println("dx=" + test.dx + ", dy=" + test.dy);
			}
			*/
			//initialization of the gardener			
	        System.out.println("I'm a KSTT gardener!");
	        rc = controller;
	        broadcaster = new Broadcaster(rc);	
	        broadcaster.gardenerInfo.refresh();
	        dir = broadcaster.gardenerInfo.originDirection;
	        rotation = dir.radians;
	        //EXPERIMENTAL:
	        
	        unitHole.rotate(rotation);
	        //gardenerDist.rotate(rotation);
			//archonDist.rotate(rotation);
			for (Diff loc: treeLocations) {
				loc.rotate(rotation);
			}
			
			if (rc.getRoundNum() < 10 && RobotType.SCOUT.bulletCost <= rc.getTeamBullets()) {
				Direction freeDir = SharedUtils.tryFindPlace(rc, dir.opposite().radians);
				if (freeDir != null && rc.canBuildRobot(RobotType.SCOUT, freeDir)) {
					rc.buildRobot(RobotType.SCOUT, freeDir);
				}
			}
		}
		catch (Exception e) {
            System.out.println("KSTT Gardener Init Exception");
            e.printStackTrace();
        }
		
		
		
        // The code you want your robot to perform every round should be in this loop  
		while (true) {
			myType = MY_TYPE.GARDENER;
	        //GARDENER:
			if (myType == MY_TYPE.GARDENER) {
		        	executeGardener();
		        }
			
	        //BUILDER
			/*
	        else {
	        	executeBuilder();
	        }*/
		}
    }
	
	/* Gardener execution code */
	static void executeGardener() {		
		boolean foundPlace = false;
		boolean onPlace = false;
		boolean targetAcquired = false;
		boolean lumberjackBuilt = false;
		MapLocation finalLocation = null;
		int myTrees = 0;
		float myHealth = rc.getHealth();
		float baseFloat = (float)Math.random() * 2F * (float)Math.PI;
		int faults = 0;
		//nema smysl si pamatovat TreeInfo, protoze se jen okopiruje
		//List<MapLocation> myTrees = new ArrayList<>();
		boolean[] builtTrees = new boolean[treeLocations.length];
		boolean hasPath = false;
		boolean underAtack = false;
		boolean seekHole = false;
		int holeX = 1000;
		int holeY = 1000;
		Pathfinding myPath;
		myPath = new Pathfinding(rc, broadcaster);
        while (true) {    	
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {         	
            	broadcaster.refresh();
				broadcaster.gardenerInfo.refresh();	
				if (rc.getRoundNum() % 10 == 0) {
					broadcaster.addGardener();
				}
            	myLife++;
            	SharedUtils.tryShake(rc);
                SharedUtils.tryToWin(rc);
    			broadcaster.tryReportEmptyPlace();
                
                if (!rc.onTheMap(rc.getLocation(), RobotType.GARDENER.sensorRadius - 0.01F)) { //TODO zrychlit
                	broadcaster.reportWall(0);
                	broadcaster.reportWall(1);
                	broadcaster.reportWall(2);
                	broadcaster.reportWall(3);
                }              
        		if (!onPlace) {
        			if (!targetAcquired)  {
        				if (rc.readBroadcastInt(Broadcaster.EMPTY_PLACE_COUNT) > 0) {
        					int newX = rc.readBroadcastInt(Broadcaster.EMPTY_PLACE_REPORTS);
        					int newY = rc.readBroadcastInt(Broadcaster.EMPTY_PLACE_REPORTS + 1);
        					if (!seekHole || newX != holeX || newY != holeY) {
        						holeX = newX;
        						holeY = newY;
        						seekHole = true;
        						finalLocation = broadcaster.gardenerInfo.originPoint.add(broadcaster.gardenerInfo.originDirection.rotateLeftDegrees(90f), GardenerPlacementInfo.NEIGHBOUR_DISTANCE*(holeY + 0.5F)).add(broadcaster.gardenerInfo.originDirection, GardenerPlacementInfo.LINE_DISTANCE*(holeX + 0.5F));
        						hasPath = myPath.FindPath(rc.getLocation(), finalLocation, true);
        					}
        				}
        				else if (seekHole) {
        					seekHole = false;
        					holeX = 1000;
        					holeY = 1000;
        				}
        				else {
							MapLocation newfinalLocation = broadcaster.gardenerInfo.currentTarget;
							if (finalLocation == null || finalLocation.distanceTo(newfinalLocation) > 0.05F) {
								finalLocation = newfinalLocation;
								hasPath = myPath.FindPath(rc.getLocation(), finalLocation, true);							
							}
        				}
        				/*
						if (faults > 20) {
			        		faults++;
			        		SharedUtils.tryMove(rc, SharedUtils.randomDirection());
						}
			        	if (faults > 40)
			        		faults = 0;
			        		*/
				        if(!hasPath) { 
				        	hasPath = myPath.FindPath(rc.getLocation(), finalLocation, true);
				        	if (!hasPath) {
								SharedUtils.tryMove(rc, rc.getLocation().directionTo(finalLocation));
							}
				        }
				        if (hasPath) {
				        	int status = myPath.nextPoint(rc);
				        	if (status == 2) {
				        		hasPath = false;
				        	}
				        	else if (status == 1) {
				        		faults++;
				        		if (faults > 20) {
				        			faults = 0;
				        			hasPath = false;
				        		}
				        	}
				        }   				
					}
									
        			if (myLife > 100 && lumberjackBuilt == false && rc.getTeamBullets() > 100) {
        				if (rc.senseNearbyTrees(3F, Team.NEUTRAL).length > 0) {
	        				Direction freeDir = SharedUtils.tryFindPlace(rc, baseFloat);
	        				if (freeDir != null && rc.canBuildRobot(RobotType.LUMBERJACK, freeDir)) {
	        					rc.buildRobot(RobotType.LUMBERJACK, freeDir);
	        					lumberjackBuilt = true;
	        				}
        				}
        			}
        			/*
        			if (rc.canSenseLocation(finalLocation)) {
						if (!rc.onTheMap(finalLocation) || 
								(rc.canSenseAllOfCircle(finalLocation, GardenerPlacementInfo.RADIUS_ON_MAP)	&& !rc.onTheMap(finalLocation, GardenerPlacementInfo.RADIUS_ON_MAP))) {*/
        			if (targetAcquired && !broadcaster.gardenerInfo.checkWall(finalLocation)) {
        				System.out.println(broadcaster.gardenerInfo.walls[0] + " " + broadcaster.gardenerInfo.walls[1] + " " + broadcaster.gardenerInfo.walls[2] + " " + broadcaster.gardenerInfo.walls[3] + " ");
						broadcaster.reportWall(0);
						broadcaster.reportWall(1);
						broadcaster.reportWall(2);
						broadcaster.reportWall(3);
						
						//broadcaster.gardenerInfo.targetNotFound();
						System.out.println("Target failed:" + finalLocation.toString());
						targetAcquired = false;
						broadcaster.gardenerInfo.removeTarget(finalLocation);
						Clock.yield();
						return;
					}
        			if (targetAcquired && rc.getLocation().distanceTo(finalLocation) > 1) {
        				RobotInfo info = rc.senseRobotAtLocation(finalLocation);
        				if (info != null && info.type == RobotType.GARDENER) {
        					targetAcquired = false;
        					seekHole = false;
        					System.out.println("throwed");
        					SharedUtils.tryMove(rc, rc.getLocation().directionTo(finalLocation));
        					continue;
        				}
        			}
        			//}
        			if (rc.getLocation().distanceTo(finalLocation) >= RobotType.GARDENER.strideRadius) {
						if (!targetAcquired && (rc.getLocation().distanceTo(finalLocation) < GardenerPlacementInfo.ACQUIRE_DISTANCE) && !rc.isLocationOccupiedByTree(finalLocation)) {
							if (!seekHole)
								broadcaster.gardenerInfo.targetAcquired();
							else
								broadcaster.gardenerInfo.addTarget(finalLocation);
							targetAcquired = true;
						}
						if (targetAcquired)
							SharedUtils.tryMove(rc, rc.getLocation().directionTo(finalLocation));
            		}
            		else {
						if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation, 1F)) {
							if (!rc.hasMoved() && rc.canMove(finalLocation)) {
								rc.move(finalLocation);
								if (!targetAcquired) {
									if (!seekHole)
										broadcaster.gardenerInfo.targetAcquired();
									else
										broadcaster.gardenerInfo.addTarget(finalLocation);
									targetAcquired = true;
								}
							}
							else {
								if (targetAcquired)
									SharedUtils.tryMove(rc, rc.getLocation().directionTo(finalLocation));
							}
            			}
						if (rc.getLocation().distanceTo(finalLocation) < 1F) {
							onPlace = true;
							broadcaster.removeEmptyPlace(finalLocation);
						}
					}
        		}
        		else {
            		//check whether I have built all trees
            		//List<Integer> toRemove = new ArrayList<>();
            		int wantBuild = -1;
            		//are there any trees left after previous gardener? (only once)
            		if (seekHole) {
            			for (int id = 0; id < builtTrees.length; id++) {
                			TreeInfo myTree = rc.senseTreeAtLocation(treeLocations[id].add(finalLocation));
                			if (myTree != null) {
                				//System.out.println("Tree disapeared!");
                				builtTrees[id] = true;
                				myTrees++;
                			}          			
            			}
            			seekHole = false;
            		}
            		for (int id = 0; id < builtTrees.length; id++) {
            			//if the tree is already built
            			if (builtTrees[id] == true) {
                			TreeInfo myTree = rc.senseTreeAtLocation(treeLocations[id].add(finalLocation));
                			if (myTree == null) {
                				//System.out.println("Tree disapeared!");
                				builtTrees[id] = false;
                				myTrees--;
                			}
                			else {
                				if (rc.canWater() && myTree.maxHealth - myTree.health > 4.4F) {
                					//System.out.println("Want water:" + myTree.getLocation().toString() + ",dist = " + rc.getLocation().distanceTo(myTree.getLocation()));
                					if (rc.canWater(myTree.ID))
                						rc.water(myTree.ID);
                				}
                			}
            			}
            			//tree not yet built - increases the value to let other gardeners plant too
            			else if (rc.isBuildReady() && rc.getTeamBullets() >= GameConstants.BULLET_TREE_COST + 3*myTrees) {
            				if (wantBuild < 0 && !rc.isCircleOccupiedExceptByThisRobot(treeLocations[id].add(finalLocation), 1F)) {
            					wantBuild = id;
            				}
            			}
            		}
            		if (wantBuild >= 0 && !underAtack) {
            			//primy posun
            			if (!rc.isCircleOccupiedExceptByThisRobot(treeLocations[wantBuild].addChanged(finalLocation, 2.01F), 1F) &&
            					rc.canMove(treeLocations[wantBuild].addChanged(finalLocation, 2.01F))) {
            				rc.move(treeLocations[wantBuild].addChanged(finalLocation, 2.01F));
            				if (rc.getLocation().distanceTo(treeLocations[wantBuild].addChanged(finalLocation, 2.01F)) < 0.005) {
            					if (rc.canPlantTree(treeLocations[wantBuild].getDirection())) {
            						rc.plantTree(treeLocations[wantBuild].getDirection());
            						builtTrees[wantBuild] = true;
            						myTrees++;
            					}
            				}
            			}
            			else {
                			//kolmo zprava
                			if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation.add(rotation, origtreeLocations[wantBuild].dx), 1.005F) && 
                					origtreeLocations[wantBuild].dy > 0 && rc.canMove(finalLocation.add(rotation, origtreeLocations[wantBuild].dx))) {
                				rc.move(finalLocation.add(rotation, origtreeLocations[wantBuild].dx));
                				if (rc.getLocation().distanceTo(finalLocation.add(rotation, origtreeLocations[wantBuild].dx)) < 0.005) {
                					if (rc.canPlantTree(Direction.NORTH.rotateLeftRads(rotation))) {
	                					rc.plantTree(Direction.NORTH.rotateLeftRads(rotation));
	                					builtTrees[wantBuild] = true;
	                					myTrees++;
                					}
                				}
                			}
                			
                			//kolmo zleva
                			else if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation.add(rotation, origtreeLocations[wantBuild].dx), 1.005F) && 
                					origtreeLocations[wantBuild].dy < 0 && rc.canMove(finalLocation.add(rotation, origtreeLocations[wantBuild].dx))) {
                				rc.move(finalLocation.add(rotation, origtreeLocations[wantBuild].dx));
                				if (rc.getLocation().distanceTo(finalLocation.add(rotation, origtreeLocations[wantBuild].dx)) < 0.005) {
                					if (rc.canPlantTree(Direction.SOUTH.rotateLeftRads(rotation))) {
                						rc.plantTree(Direction.SOUTH.rotateLeftRads(rotation));
                						builtTrees[wantBuild] = true;
                						myTrees++;
                					}
                				}
                			}
                			
                			//kolmo shora
                			else if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation.add(rotation + (float)Math.PI/2, origtreeLocations[wantBuild].dy), 1.005F) && 
                					origtreeLocations[wantBuild].dx > 0 && rc.canMove(finalLocation.add(rotation + (float)Math.PI/2, origtreeLocations[wantBuild].dy))) {
                				rc.move(finalLocation.add(rotation + (float)Math.PI/2, origtreeLocations[wantBuild].dy));
                				if (rc.getLocation().distanceTo(finalLocation.add(rotation + (float)Math.PI/2, origtreeLocations[wantBuild].dy)) < 0.005) {
                					if (rc.canPlantTree(Direction.EAST.rotateLeftRads(rotation))) {
                						rc.plantTree(Direction.EAST.rotateLeftRads(rotation));
                						builtTrees[wantBuild] = true;
                						myTrees++;
                					}
                				}
                			}
                			//kolmo zdola
                			else if (!rc.isCircleOccupiedExceptByThisRobot(finalLocation.add(rotation + (float)Math.PI/2, origtreeLocations[wantBuild].dy), 1.005F) && 
                					origtreeLocations[wantBuild].dx < 0 && rc.canMove(finalLocation.add(rotation + (float)Math.PI/2, origtreeLocations[wantBuild].dy))) {
                				rc.move(finalLocation.add(rotation  + (float)Math.PI/2, origtreeLocations[wantBuild].dy));
                				if (rc.getLocation().distanceTo(finalLocation.add(rotation + (float)Math.PI/2, origtreeLocations[wantBuild].dy)) < 0.005) {
                					if (rc.canPlantTree(Direction.WEST.rotateLeftRads(rotation))) {
                						rc.plantTree(Direction.WEST.rotateLeftRads(rotation));
                						builtTrees[wantBuild] = true;
                						myTrees++;
                					}
                				}
                			}
                			
                			//jinak se vrat na start
                			else if (rc.canMove(finalLocation)) {
                				rc.move(finalLocation);
                			}
            			}
            		}
        			
                    if ((rc.getTeamBullets() > 400 || (underAtack && rc.getTeamBullets() >= 100)) && !rc.isCircleOccupied(unitHole.add(finalLocation), 1F)) {
                    	underAtack = false;
                    	if ((unitsMade % 5) != 3 && rc.canBuildRobot(RobotType.SOLDIER, unitHole.getDirection())) {
                    		rc.buildRobot(RobotType.SOLDIER, unitHole.getDirection());
                    		unitsMade++;
                    	}
                    	else if (rc.canBuildRobot(RobotType.SCOUT, unitHole.getDirection())) {
                    		rc.buildRobot(RobotType.SCOUT, unitHole.getDirection());
                    		unitsMade++;
                    	}
                    }
                    else if (!rc.hasMoved() && rc.canMove(finalLocation)) {
        				rc.move(finalLocation);
        			}
        		}
          		if (myHealth > rc.getHealth())
        			underAtack = true;
          		//I am under attack, try to build a soldier
          		if (underAtack) {
          			Direction dir = SharedUtils.tryFindPlace(rc, baseFloat);
          			if (dir != null && rc.canBuildRobot(RobotType.SOLDIER, dir)) {
          				underAtack = false;
          				rc.buildRobot(RobotType.SOLDIER, dir);
          			}
          		}
                myHealth = rc.getHealth();
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("KSTT Gardener Exception");
                e.printStackTrace();
            }
        }
	}
	
	/* Builder execution code */
	/*
	static void executeBuilder() {
		boolean spaceProblemSolved = false;
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

						if (!spaceProblemSolved) {
							MapLocation hole = tryFindHole();
							if (hole != null) {
								spaceProblemSolved = true;
							}
							else if (rc.getTeamBullets() >= RobotType.LUMBERJACK.bulletCost) {
								Direction lumberLoc = SharedUtils.tryFindPlace(rc, baseFloat);
								if (lumberLoc != null && rc.canBuildRobot(RobotType.LUMBERJACK, lumberLoc)) {
									rc.buildRobot(RobotType.LUMBERJACK, lumberLoc);
									spaceProblemSolved = true;
								}
							}
						} else {
							//look around for empty place for non-tank unit
							Direction freeDir = SharedUtils.tryFindPlace(rc, baseFloat);
							
							//build it!
							if (freeDir != null) {       			
								// Randomly attempt to build a unit in this direction
								if (rc.canBuildRobot(RobotType.SOLDIER, freeDir) && Math.random() < .04) {
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
	*/
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


