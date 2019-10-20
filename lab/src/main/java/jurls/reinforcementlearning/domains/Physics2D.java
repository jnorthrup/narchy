/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.reinforcementlearning.domains;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author thorsten
 */
public class Physics2D {

    public double gravity;
    public final List<Point> points = new ArrayList<>();
    public final List<Connection> connections = new ArrayList<>();
    public double floor;

    double w = 0.4;

    public Physics2D(double gravity,double floor) {
        this.gravity = gravity;
        this.floor = floor;
    }

    public void step(double delta) {


        var numConnections = connections.size();
        for (var i = 0; i < numConnections; i++) {
            var c = connections.get(i);
            var dx = c.p1.x - c.p2.x;
            var dy = c.p1.y - c.p2.y;
            var l = Math.sqrt(dx * dx + dy * dy);
            var dl = c.length - l;
            c.p1.vx += delta * w * dl * dx / l;
            c.p1.vy += delta * w * dl * dy / l;
            c.p2.vx -= delta * w * dl * dx / l;
            c.p2.vy -= delta * w * dl * dy / l;
        }

        var numPoints = points.size();
        for (var i = 0; i < numPoints; i++) {
            var p = points.get(i);

            p.vy += delta * gravity;

            p.vx *= p.decayx;
            p.vy *= p.decayy;


            p.x += p.vx;
            p.y += p.vy;


        }


    }
}
