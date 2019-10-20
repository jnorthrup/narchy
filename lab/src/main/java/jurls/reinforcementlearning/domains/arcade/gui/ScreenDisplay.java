/*
 * Java Arcade Learning Environment (A.L.E) Agent
 *  Copyright (C) 2011-2012 Marc G. Bellemare <mgbellemare@ualberta.ca>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http:
 */
package jurls.reinforcementlearning.domains.arcade.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Displays the current Atari frame in a simple GUI.
 *
 * @author Marc G. Bellemare
 */
public class ScreenDisplay extends JPanel {

    /** The image to be displayed */
    BufferedImage image;
    /** The scale at which we want to display (3x normal height) */
    int yScaleFactor = 3;
    /** The x-axis scale at which we want to display (6x normal width) */
    int xScaleFactor = 6;
    /** The default screen width */
    int defaultWidth = 160;
    /** The default screen height */
    int defaultHeight = 210;
    /** The height of the status bar at the bottom of the GUI */
    int statusBarHeight = 20;
    /** Variables storing some relevant GUI dimensions */
    int statusBarY;
    int windowWidth;
    int windowHeight;
    /** Variables used to compute the GUI frames per second */
    int frameCount = 0;
    double fps = (double) 0;
    long frameTime = 0L;
    int updateRate = 5; 
    double fpsAlpha = 0.9;
    
    /** Additional user strings to be displayed */
    String centerString;
    MessageHistory messages;

    long maxMessageAge = 3000L;
    
    public ScreenDisplay() {
        super();

        messages = new MessageHistory();
    }

    public Dimension getPreferredSize() {

        statusBarY = defaultHeight * yScaleFactor;
        int width = defaultWidth * xScaleFactor;
        int height = statusBarY + statusBarHeight;

        windowWidth = width;
        windowHeight = height;

        return new Dimension(width, height);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawImages(g);
    }

    public void setImage(BufferedImage img) {
        synchronized (this) {
            this.image = img;
        }
    }

    public void setCenterString(String s) {
        synchronized (this) {
            centerString = s;
        }
    }

    public void addMessage(String s) {
        synchronized (this) {
            messages.addMessage(s);
        }
    }

    /** This methods calculates how many frames per second are being displayed.
     *   Exponential averaging is used for smoothness.
     */
    public void updateFrameCount() {
        synchronized (this) {
            frameCount++;
            long time = System.currentTimeMillis();

            
            if (time - frameTime >= (long) (1000 / updateRate)) {
                if (fps == (double) 0) {
                    fps = (double) frameCount;
                } else {

                    double ticksSinceUpdate = (double) ((time - frameTime) * (long) updateRate) / 1000.0;
                    double alpha = Math.pow(fpsAlpha, ticksSinceUpdate);

                    fps = alpha * fps + (1.0 - alpha) * ((double) (frameCount * updateRate) / ticksSinceUpdate);
                }

                frameCount = 0;
                frameTime = time;
            }
        }
    }

    /** Helper method that the display by the given (x,y) factors.
     *
     * @param g
     * @param xFactor 
     * @param yFactor
     */
    private static void rescale(Graphics g, double xFactor, double yFactor) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.scale(xFactor, yFactor);
        }
    }

    public void drawImages(Graphics g) {
        synchronized (this) {
            
            messages.update(maxMessageAge);

            
            rescale(g, (double) xScaleFactor, (double) yScaleFactor);
            
            if (image != null) {
                g.drawImage(image, 0, 0, null);
            }

            
            rescale(g, 1.0 / (double) xScaleFactor, 1.0 / (double) yScaleFactor);

            int statusBarTextOffset = statusBarY + 15;

            
            if (fps > (double) 0) {
                g.setColor(Color.BLACK);
                double roundedFPS = ((double) Math.round(fps * 10.0) / 10.0);
                g.drawString("FPS: " + roundedFPS, 0, statusBarTextOffset);
            }

            
            if (centerString != null) {
                int stringLength = g.getFontMetrics().stringWidth(centerString);
                g.drawString(centerString, (windowWidth - stringLength) / 2, statusBarTextOffset);
            }

            int textOffset = statusBarY - 4;

            g.setColor(Color.YELLOW);

            
            for (MessageHistory.Message m : messages.getMessages()) {

                String text = m.getText();
                int stringLength = g.getFontMetrics().stringWidth(text);
                g.drawString(text, windowWidth - stringLength - 2, textOffset);

                
                
                textOffset -= g.getFontMetrics().getHeight();
            }
        }
    }
}
