package nars.experiment.minicraft.side;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class SpriteStore {
    /**
     * The single instance of this class
     */
    protected static final SpriteStore single = new AwtSpriteStore();

    /**
     * Get the single instance of this class
     *
     * @return The single instance of this class
     */
    public static SpriteStore get() {


        return single;
    }

    public static class AwtSpriteStore extends SpriteStore {

        @Override
        public Sprite loadSprite(String ref) {


            BufferedImage sourceImage = null;

            try {


                URL url = this.getClass().getResource(ref);

                if (url == null) {
                    fail("Can't find ref: " + ref);
                }


                sourceImage = ImageIO.read(url);
            } catch (IOException ignored) {
                fail("Failed to load: " + ref);
            }


            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            Image image = gc.createCompatibleImage(sourceImage.getWidth(), sourceImage.getHeight(),
                    Transparency.BITMASK);


            image.getGraphics().drawImage(sourceImage, 0, 0, null);


            Sprite sprite = new AwtSprite(image, ref);
            return sprite;
        }

        /**
         * Utility method to handle resource loading failure
         *
         * @param message The message to display on failure
         */
        private static void fail(String message) {


            System.err.println(message);
            System.exit(1);
        }
    }

    /**
     * A sprite to be displayed on the screen. Note that a sprite
     * contains no state information, i.e. its just the image and
     * not the location. This allows us to use a single sprite in
     * lots of different places without having to store multiple
     * copies of the image.
     *
     * @author Kevin Glass
     */
    public static class AwtSprite implements Sprite {
        private static final long serialVersionUID = 1L;

        /**
         * The image to be drawn for this sprite
         */
        transient public Image image;
        public String ref;


        public AwtSprite() {
            AwtSprite s = (AwtSprite) SpriteStore.get().getSprite(ref);
            this.image = s.image;
        }

        /**
         * Create a new sprite based on an image
         *
         * @param image The image that is this sprite
         */
        public AwtSprite(Image image, String ref) {
            this.image = image;
            this.ref = ref;
        }

        /**
         * Get the width of the drawn sprite
         *
         * @return The width in pixels of this sprite
         */
        @Override
        public int getWidth() {
            return image.getWidth(null);
        }

        /**
         * Get the height of the drawn sprite
         *
         * @return The height in pixels of this sprite
         */
        @Override
        public int getHeight() {
            return image.getHeight(null);
        }

        /**
         * Draw the sprite onto the graphics context provided
         *
         * @param g The graphics context on which to draw the sprite
         * @param x The x location at which to draw the sprite
         * @param y The y location at which to draw the sprite
         */
        @Override
        public void draw(GraphicsHandler g, int x, int y) {
            g.drawImage(this, x, y);
        }

        @Override
        public void draw(GraphicsHandler g, int x, int y, Color tint) {
            g.drawImage(this, x, y, tint);
        }

        @Override
        public void draw(GraphicsHandler g, int x, int y, int width, int height) {
            g.drawImage(this, x, y, width, height);
        }

        @Override
        public void draw(GraphicsHandler g, int x, int y, int width, int height, Color tint) {
            g.drawImage(this, x, y, width, height, tint);
        }

        /**
         * Always treat de-serialization as a full-blown constructor, by
         * validating the final state of the de-serialized object.
         */
        @Override
        public void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException,
                IOException {


            ref = (String) aInputStream.readObject();
            this.image = ((AwtSprite) SpriteStore.get().getSprite(ref)).image;
        }

        /**
         * This is the default implementation of writeObject.
         * Customise if necessary.
         */
        @Override
        public void writeObject(ObjectOutputStream aOutputStream) throws IOException {

            aOutputStream.writeObject(ref);
            aOutputStream.defaultWriteObject();
        }
    }

    /**
     * The cached sprite map, from reference to sprite instance
     */
    private final Map<String, Sprite> sprites = new HashMap<>();

    /**
     * Retrieve a sprite from the store
     *
     * @param ref The reference to the image to use for the sprite
     * @return A sprite instance containing an accelerate image of the request reference
     */
    public Sprite getSprite(String ref) {


        if (sprites.get(ref) != null) {
            return sprites.get(ref);
        }


        Sprite sprite = loadSprite(ref);
        sprites.put(ref, sprite);

        return sprite;
    }

    public abstract Sprite loadSprite(String ref);
}
