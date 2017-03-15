package KSTTForTheWin;
import java.util.ArrayList;
import java.util.List;

import KSTTForTheWin.Broadcasting.Broadcaster;
import KSTTForTheWin.Broadcasting.GardenerPlacementInfo;
import battlecode.common.*;

public class Pathfinding {
	//static MapLocation startLoc;
	//static MapLocation endLoc;
	static boolean searching = false;
	//static Direction buildDir;
	static List<MapLocation> waypoints;
	static int wpID = 0;
	static GardenerPlacementInfo gpi;
	static RobotController rc;
	public Pathfinding(RobotController rc, Broadcaster br) {
		gpi = br.gardenerInfo;
		this.rc = rc;
		try {
			gpi.refresh();
		}
		catch (Exception e) {
			System.out.println("Broadcaster refresh fail");
		}
		
		//buildDir = gpi.originDirection;
	}
	
	/**
    * Looks for path to required position
    *
    * @param myLoc robot location
    * @param target target location
    */
	public boolean FindPath(MapLocation myLoc, MapLocation target) {
		return FindPath(myLoc, target, true);
	}
	
	 /**
     * Looks for path to required position
     *
     * @param myLoc robot location
     * @param target target location
     * @param toSquare whether the robot should go into the square (gardeners + lumberjacks)
     * @return whether some path was created (not done if not in used square)
     */
	public boolean FindPath(MapLocation myLoc, MapLocation target, boolean toSquare) {
		if (target == null)
			return false;
		waypoints = new ArrayList<MapLocation>();
		waypoints.add(myLoc); //starting point (not entred)
		boolean endEmpty = false;		
		try {
			if (toSquare && myLoc.distanceTo(target) < 4F) {
				return false;
			}
			gpi.refresh();
			Diff myDiff = gpi.getSquareLocation(myLoc);
			//System.out.println(myDiff.dx + " " + myDiff.dy);
			if (!gpi.usedTargets[(int)(myDiff.dx + 10.1F) - 8][(int)(myDiff.dy + 10.1F)]) {
				//System.out.println("no place");
				if (rc.canMove(rc.getLocation().directionTo(target))) {
					waypoints.add(rc.getLocation().add(rc.getLocation().directionTo(target), rc.getType().strideRadius - 0.001F));
					wpID = 1;
					System.out.println("free" + waypoints.get(1).toString());
					return true;
				}
				else {
					System.out.println("not free");
					return false;
				}
			}
			else {								
				float d0 = (myDiff.dx + 10) % 1; //+10 to avoid negative modulo
				float d1 = (myDiff.dy + 10) % 1;
				float d2 = 1 - d0;
				float d3 = 1 - d1;
				//System.out.println(d0 + " " + d1 + " " + d2 + " " + d3);
				//TODO osetrit okraje mapy
				
				//choose nearest side
				int bestID = 0;
				float best = d0;
				if (d1 < best) {
					best = d1;
					bestID = 1;
				}
				if (d2 < best) {
					best = d2;
					bestID = 2;
				}
				if (d3 < best) {
					best = d3;
					bestID = 3;
				}
				switch (bestID) { //needs corrections for negatives, rounding
					case 0:			
						myDiff.dx = myDiff.dx - best;
						break;
					case 1:
						myDiff.dy = myDiff.dy - best;
						break;
					case 2:
						myDiff.dx = myDiff.dx + best;
						break;
					case 3:
						myDiff.dy = myDiff.dy + best;
						break;
				}									
				waypoints.add(gpi.getMapLocation(myDiff));				
				//path to corner:
				Diff nextDiff = new Diff(myDiff.dx, myDiff.dy);
				/*
				
				if (bestID % 2 == 0) {
					if (((nextDiff.dx + 10.005F) % 2) >= 1)
						nextDiff.dy = (int)(nextDiff.dy + 10.0005F) - 10;
					else
						nextDiff.dy = (int)(nextDiff.dy + 10.0005F) - 9;
					}
			
				else  {
					if (((nextDiff.dy + 10.005F) % 2) >= 1)
						nextDiff.dx = (int)(nextDiff.dx + 10.0005F) - 10;
					else
						nextDiff.dx = (int)(nextDiff.dx + 10.0005F) - 9;					
				}
				*/

				if (bestID % 2 == 0) {
					if (nextDiff.dy > 0.5F)
						nextDiff.dy = (int)(nextDiff.dy + 10.0005F) - 10;
					else
						nextDiff.dy = (int)(nextDiff.dy + 10.0005F) - 9;
					}
			
				else  {
					//if (((nextDiff.dy + 10.005F) % 2) >= 1)
						//nextDiff.dx = (int)(nextDiff.dx + 10.0005F) - 10;
					//else
						nextDiff.dx = (int)(nextDiff.dx + 10.0005F) - 9;					
				}
				waypoints.add(gpi.getMapLocation(nextDiff));
				
				//find path to end: 
				Diff targetDiff = gpi.getSquareLocation(target);
				System.out.println("Target: " + target.toString());
				nextDiff = new Diff(nextDiff.dx, nextDiff.dy);

				int cycles = 0;
				while (Math.abs(nextDiff.dx - targetDiff.dx) > 1 || Math.abs(nextDiff.dy - targetDiff.dy) > 1) {
					cycles++;
					if (cycles > 20) { //safety break
						break;
					}
					if (!gpi.usedTargets[(int)(nextDiff.dx + 0.01*(targetDiff.dx - nextDiff.dx)) + 2][(int)(nextDiff.dy + 0.01*(targetDiff.dy - nextDiff.dy)) + 10]) {
						waypoints.add(gpi.getMapLocation(new Diff(nextDiff.dx + 0.01F*(targetDiff.dx - nextDiff.dx), (nextDiff.dy + 0.01F*(targetDiff.dy - nextDiff.dy)))));
						endEmpty = true;
						break; //empty space in front of me
						
					}
					/*
					boolean isUp = true;
					boolean isRight = true;
					//if (((nextDiff.dy + 10.005F) % 2) >= 1)
						//isRight = false;
					if (((nextDiff.dx + 10.005F) % 2) >= 1)
						isUp = false;
					
					//right path:
					if (isRight) {
						if (targetDiff.dx - nextDiff.dx > 1)
							nextDiff.dx++;
						else if (isUp && targetDiff.dy > nextDiff.dy)
							nextDiff.dy++;
						else if (!isUp && targetDiff.dy < nextDiff.dy)
							nextDiff.dy--;
						else
							nextDiff.dx++;
					}
					*/
					nextDiff.dx++;
					waypoints.add(gpi.getMapLocation(nextDiff));
					nextDiff = new Diff(nextDiff.dx, nextDiff.dy);
					
					//remove redundant waypoints
					if (waypoints.size() > 2) {
						int last = waypoints.size() - 1;
						if (Math.abs(waypoints.get(last).directionTo(waypoints.get(last - 1)).radiansBetween(waypoints.get(last - 1).directionTo(waypoints.get(last - 2)))) < 0.01) {
							waypoints.remove(last - 1);
						}
					}
				}
				for (MapLocation m:waypoints) {
					System.out.println("Path:" + m.toString());
				}
				//System.out.println(nextDiff.dx + " " + nextDiff.dy);
			}				
			if (toSquare && !endEmpty) {
				waypoints.add(target);
			}
		}
		catch (Exception e) {
			System.out.println("Pathfinding failed (crash)!");
			e.printStackTrace();
		}
		wpID = 1;
		return true;		
	}
	
	public int nextPoint(RobotController rc) { //returns: 0 -> walked, 1->returned/stopped, 2-> finished
		if (rc.hasMoved()) //moved already by some another source -> dont do anything
			return 0;
		if (wpID == waypoints.size()) {
			return 2;
		}	
		else {
			try {
				//System.out.println(rc.getLocation().toString());
				//System.out.println(waypoints.get(wpID).x + " " + waypoints.get(wpID).y);
				if (rc.canMove(waypoints.get(wpID))) {
					int newwpID = wpID;
					if (rc.getLocation().distanceTo(waypoints.get(wpID)) <= rc.getType().strideRadius) {
						newwpID++;
					}
					rc.move(waypoints.get(wpID));
					wpID = newwpID;
					if (wpID == waypoints.size()) {
						return 2;
					}
					else {
						return 0;
					}
				}
				//cannot move just after start -> return back to start (otherwise the unit can block path)
				else if (wpID == 1) {
					if (rc.getLocation().distanceTo(waypoints.get(0)) < 0.001F) {
						if (rc.canMove(waypoints.get(1).directionTo(waypoints.get(0))))
							rc.move(waypoints.get(1).directionTo(waypoints.get(0)));
					}
					if (rc.canMove(waypoints.get(0))) {
						rc.move(waypoints.get(0));
					}
					return 1;
				}
				else if (waypoints.get(wpID).x < rc.getLocation().x) { //reseni prednosti
					float dist = (float)Math.random()*rc.getType().strideRadius*2;
					if (dist > rc.getType().strideRadius)
						dist = rc.getType().strideRadius;
					if (rc.canMove(waypoints.get(wpID).directionTo(rc.getLocation()), dist)) {
						rc.move(waypoints.get(wpID).directionTo(rc.getLocation()), dist);
					}
					return 1;
				}
				else {
					float dist = (float)Math.random()*rc.getType().strideRadius;
					if (rc.canMove(waypoints.get(wpID).directionTo(rc.getLocation()), dist)) {
						rc.move(waypoints.get(wpID).directionTo(rc.getLocation()), dist);
					}
					return 1;
				}
			}
			catch (Exception e) {
				System.out.println("pathfinding crash");
				e.printStackTrace();
			}
			return 2;
		}		
	}
}
