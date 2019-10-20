package nars.experiment.asteroids;

import java.awt.*;

public class Rock extends VectorSprite {
    int c;
    int d;
    int e;
    int f;
    int g;
    int h;
    int i;
    int j;
    int k;
    int l;
    int m;
    int n;
    double a;
    double b;

    public Rock() {
        size = 4.0;
        initializeAsteroid();
    }

    public Rock(double x, double y, double size, double x2, double y2) {
        this.size = size;
        initializeAsteroid();
        xposition = x;
        yposition = y;
        xspeed = x2 + ((Math.random() - 0.5) * 2.0);
        yspeed = y2 + ((Math.random() - 0.5) * 2.0);
    }

    public void initializeAsteroid() {
        c = (int) (Math.random() * 12.0 * size);
        d = (int) (Math.random() * 12.0 * size);
        e = (int) (Math.random() * 12.0 * size);
        f = (int) (Math.random() * 12.0 * size);
        g = (int) (Math.random() * 12.0 * size);
        h = (int) (Math.random() * 12.0 * size);
        i = (int) (Math.random() * 12.0 * size);
        j = (int) (Math.random() * 12.0 * size);
        k = (int) (Math.random() * 12.0 * size);
        l = (int) (Math.random() * 12.0 * size);

        shape = new Polygon();
        shape.addPoint(c, i);
        shape.addPoint(d, -j);
        shape.addPoint(-e, -k);
        shape.addPoint(-h, n);
        shape.addPoint(0, m);

        drawShape = new Polygon();
        drawShape.addPoint(c, i);
        drawShape.addPoint(d, -j);
        drawShape.addPoint(-e, -k);
        drawShape.addPoint(-h, n);
        drawShape.addPoint(0, m);

        health = 3.0 * (size - 2.0);

        xposition = 450.0;
        yposition = 300.0;

        a = Math.random() * 2.0;
        b = Math.random() * 2.0 * Math.PI;

        xspeed = Math.cos(b) * a;
        yspeed = Math.sin(b) * a;

        a = Math.random() * 400.0 + 100.0;
        b = Math.random() * 2.0 * Math.PI;

        xposition = Math.cos(b) * a + 450.0;
        yposition = Math.sin(b) * a + 300.0;

        ROTATION = (Math.random() - 0.5) / 5.0;
    }

    @Override
    public void updatePosition(int w, int h) {
        angle += ROTATION;
        super.updatePosition(w, h);
    }
}
