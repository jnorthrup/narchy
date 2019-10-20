package nars.experiment.asteroids;

import java.awt.*;

public class Debris extends VectorSprite {
    public Debris(double x, double y) {

        shape = new Polygon();
        shape.addPoint(1, 1);
        shape.addPoint(-1, -1);
        shape.addPoint(-1, 1);
        shape.addPoint(1, -1);

        drawShape = new Polygon();
        drawShape.addPoint(1, 1);
        drawShape.addPoint(-1, -1);
        drawShape.addPoint(-1, 1);
        drawShape.addPoint(1, -1);

        xposition = x;
        yposition = y;

        double a = Math.random() * 2.0 * Math.PI;
        angle = a;

        THRUST = Math.random() * 5.0 + 5.0;

        xspeed = Math.cos(angle) * THRUST;
        yspeed = Math.sin(angle) * THRUST;
    }
}
