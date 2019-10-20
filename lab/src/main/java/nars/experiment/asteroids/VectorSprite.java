package nars.experiment.asteroids;

import java.awt.*;

public class VectorSprite {

    double xposition;
    double yposition;
    double xspeed;
    double yspeed;
    double angle;
    double size;
    double health;
    double damage;
    double spreadModifier;


    double ROTATION;
    double THRUST;


    int counter;
    int burstCounter;
    boolean bursting;
    int invincCounter;
    int weaponType;
    int fireDelay;

    boolean weaponSwitched;
    boolean invincible;

    Polygon shape;
    Polygon drawShape;

    boolean active;

    final int[][] upgrades = new int[3][4];
    final int[][] upgradeCost = new int[3][4];

    public VectorSprite() {
        active = true;
    }

    public void paint(Graphics2D g, boolean fill) {

        if (fill)
            g.fillPolygon(drawShape);
        else
            g.drawPolygon(drawShape);
    }

    public void updatePosition(int w, int h) {
        counter++;

        boolean eternal = true;
        if (!eternal)
            invincCounter++;

        xposition += xspeed;
        yposition += yspeed;

        int y;
        int x;
        for (int i = 0; i < shape.npoints; i++) {

            x = (int) Math.round((double) shape.xpoints[i] * Math.cos(angle) - (double) shape.ypoints[i] * Math.sin(angle));
            y = (int) Math.round((double) shape.xpoints[i] * Math.sin(angle) + (double) shape.ypoints[i] * Math.cos(angle));

            drawShape.xpoints[i] = x;
            drawShape.ypoints[i] = y;
        }

        wraparound(w, h);

        x = (int) Math.round(xposition);
        y = (int) Math.round(yposition);

        drawShape.invalidate();

        drawShape.translate(x, y);

    }

    private void wraparound(int w, int h) {
        if (xposition >= (double) w) {
            xposition = (double) 0;
        }

        if (xposition < (double) 0) {
            xposition = (double) (w - 1);
        }

        if (yposition >= (double) h) {
            yposition = (double) 0;
        }

        if (yposition < (double) 0) {
            yposition = (double) (h - 1);
        }
    }

}
