package nars.experiment.bomberman;

/**
 * File:         BomberImagePanel.java
 * Copyright:    Copyright (c) 2001
 *
 * @author Sammy Leong
 * @version 1.0
 */

import javax.swing.*;
import java.awt.*;

/**
 * This class creates a custom image button object.
 */
public class BomberImageButton {
    /** this object's container */
    private final JPanel panel;
    /** x co-ordinate where the image is drawn */
    private int x;
    /** y co-ordinate where the image is drawn */
    private int y;
    /** ID of object (for command) */
    private int ID;
    /** image width */
    private final int w;
    /** image height */
    private final int h;
    /** area the object controls */
    private Rectangle rect;
    /** the images of the button: normal / outerglowed */
    private final Image[] images;
    /** stae of button: normal / outglowed */
    private int state;

    /** rendering hints */
    private static Object hints;

    static {
        /** if java runtime is Java 2 */
        if (Main.J2) {
            /** create the rendering hints for better graphics output */
            RenderingHints h = null;
            h = new RenderingHints(null);
            h.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            h.put(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            h.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            h.put(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            h.put(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            hints = h;
        }
    }

    /**
     * Construct with a MainMenu object and an array of images for use with
     * the button.
     * @param mainMenu object's container
     * @param images images for the button
     */
    public BomberImageButton(JPanel panel, Image[] images) {
        this.panel = panel;
        this.images = images;
        /** calculate image dimension */
        w = images[0].getWidth(panel);
        h = images[0].getHeight(panel);
    }

    /**
     * Set object's parameters.
     * @param x x co-ordinate on window
     * @param y y co-ordinate on window
     * @param ID object's ID or command return code
     */
    public void setInfo(int x, int y, int ID) {
        this.x = x;
        this.y = y;
        this.ID = ID;
        /** calculate area in which the object owns to handle mouse events */
        rect = new Rectangle(this.x, this.y - 5, w, h + 10);
    }

    /**
     * @return the ojbect's ID
     */
    public int getID() {
        return ID;
    }

    /**
     * Set bevel on or off
     * @param bevelOn true or false
     */
    public void setBevel(boolean bevelOn) {
        if (bevelOn) state = 1;
        else state = 0;
        panel.repaint();
        panel.paintImmediately(x, y, w / (32 / BomberMain.size * 2),
                h / (32 / BomberMain.size * 2));
    }

    /**
     * Draws the button onto the window.
     * @param graphics graphics handler
     */
    public void paint(Graphics graphics) {
        /** if java runtime is Java 2 */
        if (Main.J2) {
            paint2D(graphics);
        }
        /** if java runtime isn't Java 2 */
        else {
            Graphics g = graphics;
            /** draw the button */
            g.drawImage(images[state], x, y, w / (32 / BomberMain.size * 2),
                    h / (32 / BomberMain.size * 2), null);
        }
    }

    /**
     * Drawing method for Java 2's Graphics2D
     * @param graphics graphics handle
     */
    public void paint2D(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics;
        /** set the rendering hints */
        g2.setRenderingHints((RenderingHints) hints);
        /** draw the button */
        g2.drawImage(images[state], x, y, w / (32 / BomberMain.size * 2),
                h / (32 / BomberMain.size * 2), null);
    }
}