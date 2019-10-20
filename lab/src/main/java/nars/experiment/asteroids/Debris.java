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

        var a = Math.random() * 2 * Math.PI;
        angle = a;

        THRUST = Math.random() * 5 + 5;

        xspeed = Math.cos(angle) * THRUST;
        yspeed = Math.sin(angle) * THRUST;
    }
}
