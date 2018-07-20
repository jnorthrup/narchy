package nars.experiment.minicraft.side;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface Sprite extends java.io.Serializable {

    /**
     * Get the width of the drawn sprite
     *
     * @return The width in pixels of this sprite
     */
    int getWidth();

    /**
     * Get the height of the drawn sprite
     *
     * @return The height in pixels of this sprite
     */
    int getHeight();

    /**
     * Draw the sprite onto the graphics context provided
     *
     * @param g The graphics context on which to draw the sprite
     * @param x The x location at which to draw the sprite
     * @param y The y location at which to draw the sprite
     */
    void draw(GraphicsHandler g, int x, int y);

    void draw(GraphicsHandler g, int x, int y, Color tint);

    void draw(GraphicsHandler g, int x, int y, int width, int height);

    void draw(GraphicsHandler g, int x, int y, int width, int height, Color tint);

    void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException,
            IOException;

    void writeObject(ObjectOutputStream aOutputStream) throws IOException;
}
