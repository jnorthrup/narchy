package java4k.pinball4k.editor;


import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.reflect.Field;

public class Bezier extends LevelObject {
	public Point p2;
	public Point p3;
	public int subdivs = 8;
	
	private final EndpointHandle p1Handle = new EndpointHandle(0);
	private final EndpointHandle p2Handle = new EndpointHandle(1);
	private final EndpointHandle p3Handle = new EndpointHandle(2);
	
	public Bezier(Point p1, Point p2, Point p3) {
		this.p = new Point(p1);
		this.p2 = new Point(p2);
		this.p3 = new Point(p3);
		
		handles.add(p1Handle);
		handles.add(p2Handle);
		handles.add(p3Handle);
	}
	
	/**
	 * Draws itself to the specified graphics object
	 * @param g where to draw
	 */
	@Override
    public void draw(Graphics2D g, LevelPanel levelPanel) {
		g.setColor(Color.WHITE);
		float x0 = p.x;
		float y0 = p.y;
		float x1 = p2.x;
		float y1 = p2.y;
		float x2 = p3.x;
		float y2 = p3.y;


		var prevx = x0;
		var prevy = y0;
		for (var i = 1; i<=subdivs; i++) {
			var t = i / (float) (subdivs);
			var t2 = t*t;
			var tinv = 1-t;
			var tinv2 = tinv*tinv;

			var x = tinv2 * x0 + 2*t*tinv*x1 + t2*x2;
			var y = tinv2 * y0 + 2*t*tinv*y1 + t2*y2;
			g.drawLine((int) prevx, (int) prevy, (int) x, (int) y);
			prevx = x;
			prevy = y;
		}
	}	
	
	public Point2D.Float interpolate(float t) {
		var t2 = t*t;
		var tinv = 1-t;
		var tinv2 = tinv*tinv;

		var x = tinv2 * p.x + 2*t*tinv*p2.x + t2*p3.x;
		var y = tinv2 * p.y + 2*t*tinv*p2.y + t2*p3.y;
		return new Point2D.Float(x, y);
	}
	
	/**
	 * Draws the handles to the specified graphics object
	 * @param g where to draw
	 */
	@Override
    public void drawHandles(Graphics2D g, LevelPanel levelPanel) {
		p1Handle.draw(g, levelPanel);
		p2Handle.draw(g, levelPanel);
		p3Handle.draw(g, levelPanel);
	}	
	
	/**
	 * The public properties to show in the editor ui.
	 * @return the properties to show
	 */
	@Override
    public Field[] getProperties() {
		return getFields("visible", "collidable", "isRollOver", "score", "bounce", "behaviorId", "p", "p2", "p3", "subdivs");
	}	
	
	/*
	 *  (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Bezier (" + p.x + ", " + p.y + ")" + " (" + p2.x + ", " + p2.y + ")" + " (" + p3.x + ", " + p3.y + ")";
	}
	
	/**
	 * A handle for one of the lines.
	 * @author tombr
	 *
	 */
	class EndpointHandle extends Handle {
		int pointIdx;
		EndpointHandle(int pointIdx) {
			this.pointIdx = pointIdx;
		}
		
		/**
		 * Overrides Handle. Gets the endpoint.
		 * @return the handles endpoint
		 */
		@Override
        public Point getCenter() {
            switch (pointIdx) {
                case 1:
                    return p2;
                case 2:
                    return p3;
            }
			
			return p;
		}
		
		/**
		 * The handle is dragged to the specified position. Moves the endpoin 
		 * the distance dragged.
		 * @param p where the handle is dragged to
		 */
		@Override
        public void dragged(int dx, int dy) {
			getCenter().translate(dx, dy);
		}
		
		/**
		 * Gets the level object the hangle controlls. Can be null.
		 * @return the level object
		 */
		@Override
        public LevelObject getLevelObject() {
			return Bezier.this;
		}
	}
}
