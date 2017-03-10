package KSTTForTheWin;
import battlecode.common.*;

/* Class for holding relative position of some object */
public strictfp class Diff {	
	public float dx;
	public float dy;
	//public Direction dir;
	//public float length;
	public Diff(float dx, float dy){
		this.dx = dx; 
		this.dy = dy;
		//dir = new Direction(dx, dy);
		//length = (float)Math.sqrt((double)(dx*dx + dy*dy));
	}
	
	/**
	 * Gets coordinates difference end - start.
	 * */
	public Diff(MapLocation start, MapLocation end) {
		dx = end.x - start.x;
		dy = end.y - start.y;
		//dir = new Direction(dx, dy);
		//length = (float)Math.sqrt((double)(dx*dx + dy*dy));
	}
	public Direction getDirection() {
		return new Direction(dx, dy);
	}
	public float getLength() {
		return (float)Math.sqrt((double)(dx*dx + dy*dy));
	}
	/**
	 * will add relative position to center
	 * @param center Point to which diff is added
	 * @return transformed position 
	 * */
	public MapLocation add(MapLocation center) {
		return center.translate(dx, dy);
	}
	public MapLocation addChanged(MapLocation center, float reduction) {
		Direction dir = getDirection();
		float length = getLength();
		return center.translate(dir.getDeltaX(length - reduction), dir.getDeltaY(length - reduction));
	}
	
	public void invert() {
		dx = -dx;
		dy = -dy;
		//dir = dir.rotateLeftDegrees(180);
	}
	
	//TODO rychlejsi alg.
	public void rotate(float angle) {
		MapLocation m = new MapLocation(0,0);
		m = m.add(getDirection().radians + angle, getLength());
		dx = m.x;
		dy = m.y;
		//dir = dir.rotateLeftRads(angle);
	}
	public void multiply(float compX, float compY) {
		dx *= compX;
		dy *= compY;
		//dir = new Direction(dx, dy);
		//length = (float)Math.sqrt((double)(dx*dx + dy*dy));
	}
	
	public void addDiff(float plusX, float plusY) {
		dx += plusX;
		dy += plusY;
	}
}