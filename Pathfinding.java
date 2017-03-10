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
	public Pathfinding(RobotController rc, Broadcaster br) {
		gpi = br.gardenerInfo;
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
		waypoints = new ArrayList<MapLocation>();
		
		try {
			if (toSquare && myLoc.distanceTo(target) < 3F) {
				return false;
			}
			gpi.refresh();
			Diff myDiff = gpi.getSquareLocation(myLoc);
			//System.out.println(myDiff.dx + " " + myDiff.dy);
			if (!gpi.usedTargets[(int)(myDiff.dx + 10.0005F) - 8][(int)(myDiff.dy + 10.0005)]) {
				//System.out.println("no place");
				return false;
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
				switch (bestID) { //needs corrections for negatives, rounding)
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
				//trick: if sum is even, path leads left; if odd, then right.
				float sum = nextDiff.dx + nextDiff.dy + 10;
				if (Math.abs(sum % 2) >= 1) {
					if (bestID % 2 == 0)
						nextDiff.dy = (int)(nextDiff.dy + 10.0005F) - 9;
					else
						nextDiff.dx = (int)(nextDiff.dx + 10.0005F) - 10;
				}
				else {
					if (bestID % 2 == 0)
						nextDiff.dy = (int)(nextDiff.dy + 10.0005F) - 10;
					else
						nextDiff.dx = (int)(nextDiff.dx + 10.0005F) - 9;
				}
				/*
				switch (bestID) {
					case 0:			
						nextDiff.dy = (int)(nextDiff.dy) + 1;
						break;
					case 1:
						nextDiff.dx = (int)(nextDiff.dx);
						break;
					case 2:
						nextDiff.dy = (int)(nextDiff.dy);
						break;
					case 3:
						nextDiff.dx = (int)(nextDiff.dx) + 1;
						break;
				}
				*/
				waypoints.add(gpi.getMapLocation(nextDiff));
				//find path to end: //TODO
				
				Diff targetDiff = gpi.getSquareLocation(target);
				System.out.println("Target: " + target.toString());
				nextDiff = new Diff(nextDiff.dx, nextDiff.dy);
				
				while (Math.abs(nextDiff.dx - targetDiff.dx) > 1 || Math.abs(nextDiff.dy - targetDiff.dy) > 1) {
					if (waypoints.size() > 20) { //safety break
						break;
					}
					if (!gpi.usedTargets[(int)(nextDiff.dx + 0.001*(targetDiff.dx - nextDiff.dx)) + 2][(int)(nextDiff.dy + 0.001*(targetDiff.dy - nextDiff.dy)) + 10]) {
						waypoints.add(gpi.getMapLocation(new Diff(nextDiff.dx + 0.001F*(targetDiff.dx - nextDiff.dx), (nextDiff.dy + 0.001F*(targetDiff.dy - nextDiff.dy)))));
						break; //empty space in front of me
						
					}
					if (Math.abs((nextDiff.dx + nextDiff.dy + 10) % 2) < 0.05) { //left or right
						if (targetDiff.dx > nextDiff.dx) { 
							nextDiff.dx++;
						}
						else {
							nextDiff.dx--;
						}
					}
					else { //up or down
						if (targetDiff.dy > nextDiff.dy) { 
							nextDiff.dy++;
						}
						else {
							nextDiff.dy--;
						}
					}
					waypoints.add(gpi.getMapLocation(nextDiff));
					nextDiff = new Diff(nextDiff.dx, nextDiff.dy);
				}
				for (MapLocation m:waypoints) {
					System.out.println("Path:" + m.toString());
				}
				//System.out.println(nextDiff.dx + " " + nextDiff.dy);
			}				
			if (toSquare) {
				waypoints.add(target);
			}
		}
		catch (Exception e) {
			System.out.println("Pathfinding failed!");
		}
		wpID = 0;
		return true;		
	}
	
	public int nextPoint(RobotController rc) { //returns: 0 -> walked, 1->returned/stpped, 2-> stopped
		
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
					return 0;
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
			}
			return 0;
		}		
	}
}
