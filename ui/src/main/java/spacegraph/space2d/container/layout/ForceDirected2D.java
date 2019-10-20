package spacegraph.space2d.container.layout;

import jcog.Util;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.math.FloatRange;
import jcog.math.v2;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.graph.EdgeVis;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.util.MutableRectFloat;

import java.util.Random;

public class ForceDirected2D<X> extends DynamicLayout2D<X> {

    final Random rng = new XoRoShiRo128PlusRandom(1);


    public final FloatRange repelSpeed = new FloatRange(6f, 0, 10f);

    public final FloatRange attractSpeed = new FloatRange(6f, 0, 10);

    /** in (visible) graph radii */
    public final FloatRange nodeScale = new FloatRange(0.2f, 0.04f, 1.5f);

    /** in node radii */
    public final FloatRange nodeSpacing  = new FloatRange(Util.PHIf, 0.5f, 3f);

    /** 1.0 - momentum LERP */
    public final FloatRange speed = new FloatRange(0.75f, 0f, 1f);




    private float maxRepelDist;

    private float equilibriumDistFactor;

    @Override
    public void init(Graph2D<X> g, NodeVis<X> newNode) {
        float rx = g.w()/2*(rng.nextFloat()*2-1), ry = g.h()/2*(rng.nextFloat()*2-1);
        newNode.posXYWH(g.cx() + rx, g.cy() + ry, 1, 1);
    }

    @Override
    protected void layout(Graph2D<X> g, float dtS) {

        var n = nodes.size();
        if (n == 0)
            return;

        var gRad = g.radius();

        var AUTOSCALE = (float) (nodeScale.floatValue() * gRad / Math.sqrt(1f + n));
        assert (AUTOSCALE == AUTOSCALE);


        maxRepelDist = (1 * gRad); //estimate

        equilibriumDistFactor = nodeSpacing.floatValue();


        var iterations1 = 1;
        var iterations = iterations1;
        var gRadPerSec = gRad / dtS;
        var repelSpeed = this.repelSpeed.floatValue() * gRadPerSec / iterations / gRad;
        var attractSpeed = this.attractSpeed.floatValue() / iterations / gRad;


//        float maxSpeedPerIter = (nodeSpeedMax.floatValue() * dtS) * gRad / iterations;


        var gg = g.bounds;

        for (var a : nodes) {
            size(a, AUTOSCALE);
            a.clamp(gg);
        }


        var speed = this.speed.floatValue();

        for (var ii = 0; ii < iterations; ii++) {

            for (var aa = 0; aa < n; aa++) {

                var a = nodes.get(aa);


                attract(a, attractSpeed);

                var ar = a.radius();

                for (var y = aa + 1; y < n; y++)
                    repel(a, ar, nodes.get(y), repelSpeed);

            }
            for (MutableRectFloat a : nodes) {
                a.commitLerp(speed);
                a.clamp(gg);
            }
        }

    }

    protected void size(MutableRectFloat<X> m, float a) {
        var p = (float) (1f + Math.sqrt(m.node.pri)) * a;
        m.size(p, p);
    }


    /**
     * HACK this reads the positions from the nodevis not the rectangle
     */
    private void attract(MutableRectFloat a, float attractSpeed) {

        NodeVis<X> from = a.node;
        float px = a.cx(), py = a.cy();


        var aRad = a.radius();

        var read = from.outs;
        //int neighbors = read.size();

        double[] dx = {0};
        double[] dy = { 0 };

        read.forEachValue(edge -> {
            if (edge == null)
                return; //wtf

            var who = edge.to;
            if (who == null)
                return;

            var b = who.mover;
            if (b == null)
                return;

            var idealLen = (aRad + b.radius()) * equilibriumDistFactor;

            var delta = new v2(b.cx() - px, b.cy() - py);
            var len = delta.normalize();
//            if (len > idealLen) {
//            len = len * (1+Util.tanhFast(len - (scale)))/2;


                //attractSpeed/=neighbors;

            var s = (len - idealLen) * attractSpeed * weightToVelocity(edge.weight);

                //s = Util.tanhFast(s);...
                //s = (float) Math.sqrt(s);

                if (Math.abs(s) > Spatialization.EPSILONf) {
                    dx[0] += delta.x * s;
                    dy[0] += delta.y * s;
//                    a.move(delta.x, delta.y);
//                    b.move(-delta.x, -delta.y);
                }
//            }
        });

        a.move((float) dx[0], (float) dy[0]);
    }

    private static float weightToVelocity(float weight) {
        //return 1;
        return weight;
        //return weight * weight;
    }

    private void repel(MutableRectFloat a, float ar, MutableRectFloat b, float repelSpeed) {

        v2 aCenter = a;

        var br = b.radius();
//        ar *= ar;
//        br *= br;


        var v2 = aCenter.clone();
        v2.x -= b.cx();
        v2.y -= b.cy();
        var delta = v2;
        var len = delta.normalize();
        if (len <= Spatialization.EPSILONf) {
            //coincident, apply random vector
            double theta = (float) (rng.nextFloat()*Math.PI*2);
            var tx = (float) Math.cos(theta);
            var ty = (float) Math.sin(theta);
            delta.scaleClone(Spatialization.EPSILONf * 2);
            delta.set(tx, ty);
        }
        if (len >= maxRepelDist)
            return;

        var radii = (ar + br) * equilibriumDistFactor;
//        len -= (radii * equilibriumDistFactor);
//        if (len < 0)
//            len = 0;

        var s = repelSpeed /
                (1 + (len * len));
                //Util.sqr(1 + len);

        if (s > Spatialization.EPSILONf) {

            delta.scaled(s);

            var baRad = br / radii;
            a.move(delta.x * baRad, delta.y * baRad);
            var abRad = -ar / radii;
            b.move(delta.x * abRad, delta.y * abRad);
        }

    }

}
