package java4k.pinball4k.editor;

import java.awt.*;
import java.lang.reflect.Field;

public class Sircle extends LevelObject {
	public int radius;
	private final SircleHandle handle;
	private final RadiusHandle radiusHandle;
	
	private final Sircle outerInstance;
	
	public Sircle(Point p1, Point p2) {
		this.p = new Point(p1);
    	radius = (int) p2.distance(p1);
    	
    	outerInstance = this;
    	handle = new SircleHandle();
    	radiusHandle = new RadiusHandle();
    	handles.add(handle);
    	handles.add(radiusHandle);
	}
	
	/**
	 * Draws itself to the specified graphics object
	 * @param g where to draw
	 */
	@Override
    public void draw(Graphics2D g, LevelPanel levelPanel) {
		g.setColor(Color.WHITE);
    	g.fillOval(p.x - radius, p.y - radius, radius * 2, radius * 2);
	}	
	
	/**
	 * Draws the handles to the specified graphics object
	 * @param g where to draw
	 */
	@Override
    public void drawHandles(Graphics2D g, LevelPanel levelPanel) {
    	handle.draw(g, levelPanel);
    	radiusHandle.draw(g, levelPanel);
	}	
	
	/**
	 * The public properties to show in the editor ui.
	 * @return the properties to show
	 */
	@Override
    public Field[] getProperties() {
		return getFields("visible", "collidable", "isRollOver", "score", "bounce", "behaviorId", "p", "radius");
	}	
	
	@Override
    public int getSortValue() {
		var val = super.getSortValue();
		return (radius << 26) | (val >>> 6);
	}

	
	class SircleHandle extends Handle {

		/**
		 * Gets the center of the handle.
		 * @return the center
		 */
		@Override
        public Point getCenter() {
			return p;
		}
		
		/**
		 * The handle is dragged to the specified location
		 * @param pos where the handle was dragged to 
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
	
	/*
	 *  (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Sircle(" + p.x + ", " + p.y + ")";
	}
	
	class RadiusHandle extends Handle {

		/**
		 * Gets the center of the handle.
		 * @return the center
		 */
		@Override
        public Point getCenter() {
			return new Point(p.x + radius, p.y);
		}
		
		/**
		 * The handle is dragged to the specified location
		 * @param pos where the handle was dragged to 
		 */
		@Override
        public void dragged(int dx, int dy) {
			radius += dx;
	    	
		}		
		








	}
}
