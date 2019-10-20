package java4k.pinball4k.editor;

import java.awt.*;

public abstract class Handle {

	/**
	 * Gets the center of the handle.
	 * @return the center
	 */
	public abstract Point getCenter();
	
	/** 
	 * Gets the size of the handle.
	 * @return the size of the handle
	 */
	public static int getSize() {
		return 12;
	}
	
	/**
	 * Draws itself to the specified graphics object
	 * @param g where to draw
	 */
	public void draw(Graphics2D g, LevelPanel levelPanel) {
		if (levelPanel.getState() == LevelPanel.State.SELECT) {
			var handleCenter = getCenter();
			var x = handleCenter.x;
			var y = handleCenter.y;
			var size = getSize();
			x -= size / 2;
			y -= size / 2;
			var rect = new Rectangle(x, y, size, size);
			g.setColor(new Color(0xff0000ff));
			var mousePos = levelPanel.getMousePosition();
			if ((mousePos != null && rect.contains(mousePos)) 
					|| levelPanel.isHandleSelected(this)) {
				g.fillRect(x, y, size, size);
			} else {
				g.drawRect(x, y, size, size);
			}
		}
	}
	
	/**
	 * Checks if handle contains the specified point.
	 * @param p the point to check
	 * @return true if handle contains point
	 */
	/*public boolean contains(Point p) {
		return getBounds().contains(p);
	}*/
	public boolean intersects(Rectangle rect) {
		return (getBounds().intersects(rect) || rect.contains(getCenter()));
	}
	
	/**
	 * Gets the bounds of the handle
	 * @return the bounds
	 */
	private Rectangle getBounds() {
		var p = getCenter();
		var s = getSize();
		return new Rectangle(p.x - (s / 2), p.y - (s / 2), s, s);
	}
	
	public abstract void dragged(int dx, int dy);
	
	/**
	 * Gets the level object the hangle controlls. Can be null.
	 * @return the level object
	 */
	public LevelObject getLevelObject() {
		return null;
	}
}
