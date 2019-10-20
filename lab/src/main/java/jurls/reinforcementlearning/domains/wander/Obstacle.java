package jurls.reinforcementlearning.domains.wander;

import java.awt.*;


public class Obstacle {
	private static final double MIN_SIZE = (double) (World.SIZE / 10);
	private static final double MAX_SIZE = (double) (World.SIZE / 5);
	public double x;
	public double y;
	public double r;
	public Color c;
        
	
	public static double d(double from, double to) {
		if(to<from) {
			return (double) 0;
		}
		return (to-from)*(Math.random())+from;
	}        

	public Obstacle() {
		x = d((double) -World.SIZE, (double) World.SIZE);
		y = d((double) -World.SIZE, (double) World.SIZE);
		r = d(MIN_SIZE, MAX_SIZE);
		
		c = Color.ORANGE;
	}

	public boolean circleCollides(double x2, double y2, double r2) {
        double dist = getDistanceSq(x2, y2);
		return dist - (r2*r2) < (r*r);
	}

	public boolean pointCollides(double x2, double y2) {
		return circleCollides(x2, y2, (double) 0);
	}

	private double getDistanceSq(double x2, double y2) {
        double xd = (x2-x);
        double yd = (y2-y);
        double dist = (xd*xd + yd*yd);
		return dist;
	}
	private double getDistance(double x2, double y2) {
		return Math.sqrt( getDistanceSq(x2, y2) );
	}
	
}
