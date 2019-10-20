package spacegraph.util;


import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/*
 *  Convenience class to create and optionally save to a file a
 *  BufferedImage of an area on the screen. Generally there are
 *  four different scenarios. Create an image of:
 *
 *  a) an entire component
 *  b) a region of the component
 *  c) the entire desktop
 *  d) a region of the desktop
 *
 *  The first two use the Swing paint() method to draw the
 *  component image to the BufferedImage. The latter two use the
 *  AWT Robot to create the BufferedImage.
 *
 *	The created image can then be saved to a file by usig the
 *  writeImage(...) method. The type of file must be supported by the
 *  ImageIO write method.
 *
 *  Although this class was originally designed to create an image of a
 *  component on the screen it can be used to create an image of components
 *  not displayed on a GUI. Behind the scenes the component will be given a
 *  size and the component will be layed out. The default size will be the
 *  preferred size of the component although you can invoke the setSize()
 *  method on the component before invoking a createImage(...) method. The
 *  default functionality should work in most cases. However the only
 *  foolproof way to get a image to is make sure the component has been
 *  added to a realized window with code something like the following:
 *
 *  JFrame frame = new JFrame();
 *  frame.setContentPane( someComponent );
 *  frame.pack();
 *  ScreenImage.createImage( someComponent );
 *
 */
public class AWTCamera {

    public static BufferedImage get(Component component, @Nullable BufferedImage image) {
        return get(component, image, null);
    }

    private static final Map<Component,Pair<AtomicBoolean,Graphics2D>> graphicsDrawers = new WeakHashMap<>();

    /*
     *  Create a BufferedImage for Swing components.
     *  The entire component will be captured to an image.
     *
     *  @param  component Swing component to create image from
     *  @return	image the image for the given region
     */
    public static BufferedImage get(Component component, @Nullable BufferedImage image, @Nullable Rectangle region) {
        var pair = graphicsDrawers.get(component);
        if (!((pair == null || pair.getOne().compareAndSet(false, true))))
            return image; 
        try {
            var d = component.getSize();

            if (d.width == 0 || d.height == 0) {
                d = component.getPreferredSize();
                component.setSize(d);
            }

            if ((region == null) || (region.width != d.width) || (region.height != d.height))
                region = new Rectangle(0, 0, d.width, d.height);

            
            

            if (!component.isDisplayable()) {
                var d1 = component.getSize();

                if (d1.width == 0 || d1.height == 0) {
                    d1 = component.getPreferredSize();
                    component.setSize(d1);
                }

                layoutComponent(component);
            }

            if ((region.width <= 0 || region.height <= 0))
                return null;


            var g2d = pair != null ? pair.getTwo() : null;
            if (g2d == null || image == null || image.getWidth() != region.width || image.getHeight() != region.height) {
                if (g2d != null)
                    g2d.dispose();
                var gc = component.getGraphicsConfiguration();
                if (gc != null) {
                    image = gc.createCompatibleImage(region.width, region.height);
                } else {
                    image = new BufferedImage(region.width, region.height, BufferedImage.TYPE_INT_ARGB);
                }
                g2d = (Graphics2D) image.getGraphics();
                graphicsDrawers.put(component, pair = pair(new AtomicBoolean(), g2d));
            }

            if (!component.isOpaque()) {
                g2d.setColor(component.getBackground());
                g2d.fillRect(region.x, region.y, region.width, region.height);
            }
            g2d.setTransform(AffineTransform.getTranslateInstance(0, 0));
            g2d.translate(-region.x, -region.y);
            component.paint(g2d);

            return image;
        } finally {
            if (pair!=null)
                pair.getOne().set(false);
        }
    }

    /**
     * Convenience method to create a BufferedImage of the desktop
     *
     * @param fileName name of file to be created or null
     * @throws AWTException see Robot class constructors
     * @throws IOException  if an error occurs during writing
     * @return image the image for the given region
     */
    public static BufferedImage createDesktopImage()
            throws AWTException {
        var d = Toolkit.getDefaultToolkit().getScreenSize();
        var region = new Rectangle(0, 0, d.width, d.height);
        return AWTCamera.get(region);
    }

    /*
     *  Create a BufferedImage for AWT components.
     *
     *  @param  component AWT component to create image from
     *  @return	image the image for the given region
     *  @exception AWTException see Robot class constructors
     */
    public static BufferedImage get(Component component) throws AWTException {
        var p = new Point(0, 0);
        SwingUtilities.convertPointToScreen(p, component);
        var region = component.getBounds();
        region.x = p.x;
        region.y = p.y;
        return AWTCamera.get(region);
    }

    /**
     * Create a BufferedImage from a rectangular region on the screen.
     * This will include Swing components JFrame, JDialog and JWindow
     * which all extend from Component, not JComponent.
     *
     * @throws AWTException see Robot class constructors
     * @param     region region on the screen to create image from
     * @return image the image for the given region
     */
    private static BufferedImage get(Rectangle region)
            throws AWTException {
        return new Robot().createScreenCapture(region);
    }

    
































    private static void layoutComponent(Component component) {
        component.doLayout();

        if (component instanceof Container) {
            for (var child : ((Container) component).getComponents()) {
                layoutComponent(child);
            }
        }
    }


}