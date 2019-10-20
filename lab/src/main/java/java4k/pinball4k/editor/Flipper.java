package java4k.pinball4k.editor;

import java.awt.*;
import java.lang.reflect.Field;

public class Flipper extends LevelObject {
	
	public static final float ANGLE_SCALE = 127 / (2 * (float) Math.PI);
	
	double angle;
	public int length;
	boolean leftFlipper = true;
	public int minAngle;
	public int maxAngle;
	
	private final FlipperHandle handle = new FlipperHandle();
	private final Flipper outerInstance;
	
	
	/**
	 * Creates a flipper with center at p1 extending out to p2 at the default 
	 * down state.
	 * @param p1 the center of the flipper
	 * @param p2 the flipper endpoint at resing pos
	 */
	public Flipper(Point p1, Point p2) {
		this.p = new Point(p1);
		length = (int) p1.distance(p2);
		leftFlipper = (p1.x < p2.x);
		var dx = p2.x - p1.x;
		var dy = p2.y - p1.y;
		angle = (float) Math.atan2(dy, dx);
		
		if (leftFlipper) {
			maxAngle = toPacked(angle);
			minAngle = toPacked(angle - Math.PI / 3);
		} else {
			minAngle = toPacked(angle);
			maxAngle = toPacked(angle + Math.PI / 3);
		}
		handles.add(handle);
		outerInstance = this;
	}
	
	public static double toAngle(int packedAngle) {
		return packedAngle / ANGLE_SCALE;
	}
	
	public static int toPacked(double angle) {
		return (int) (angle * ANGLE_SCALE);
	}
	
	/**
	 * Draws itself to the specified graphics object
	 * @param g where to draw
	 */
	@Override
    public void draw(Graphics2D g, LevelPanel levelPanel) {
		g.setColor(Color.WHITE);
		var startDeg = (int) -Math.toDegrees(toAngle(minAngle));
		var arcDeg = (int) -Math.toDegrees(toAngle(maxAngle) - toAngle(minAngle));
		g.fillArc(p.x - length, p.y - length, length * 2, length * 2, startDeg, arcDeg);
	}	
	
	/**
	 * Draws the handles to the specified graphics object
	 * @param g where to draw
	 */
	@Override
    public void drawHandles(Graphics2D g, LevelPanel levelPanel) {
		handle.draw(g, levelPanel);
	}	
	
	/**
	 * The public properties to show in the editor ui.
	 * @return the properties to show
	 */
	@Override
    public Field[] getProperties() {
		return getFields("visible", "collidable", "score", "bounce"
				, "behaviorId", "p", "minAngle", "maxAngle");
	}	
	
	/**
	 * A handle for the whole line location between the endpoints.
	 * @author tombr
	 *
	 */
	class FlipperHandle extends Handle {
		/**
		 * Overrides Handle. Gets the endpoint.
		 * @return the handles endpoint
		 */
		@Override
        public Point getCenter() {
			return p;
		}
		
		/**
		 * The handle is dragged to the specified position. Moves the line the
		 * distance dragged.
		 * @param p where the handle is dragged to
		 */
		@Override
        public void dragged(int dx, int dy) {
			p.translate(dx, dy);
		}
		
		/**
		 * Gets the level object the hangle controlls. Can be null.
		 * @return the level object
		 */
		@Override
        public LevelObject getLevelObject() {
			return outerInstance;
		}
	}
}
