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

    final Random rng = new XoRoShiRo128PlusRandom(1L);


    public final FloatRange repelSpeed = new FloatRange(6f, (float) 0, 10f);

    public final FloatRange attractSpeed = new FloatRange(6f, (float) 0, 10.0F);

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
        float rx = g.w()/ 2.0F *(rng.nextFloat()* 2.0F - 1.0F), ry = g.h()/ 2.0F *(rng.nextFloat()* 2.0F - 1.0F);
        newNode.posXYWH(g.cx() + rx, g.cy() + ry, 1.0F, 1.0F);
    }

    @Override
    protected void layout(Graph2D<X> g, float dtS) {

        int n = nodes.size();
        if (n == 0)
            return;

        float gRad = g.radius();

        float AUTOSCALE = (float) ((double) (nodeScale.floatValue() * gRad) / Math.sqrt((double) (1f + (float) n)));
        assert (AUTOSCALE == AUTOSCALE);


        maxRepelDist = (1.0F * gRad); //estimate

        equilibriumDistFactor = nodeSpacing.floatValue();


        int iterations1 = 1;
        int iterations = iterations1;
        float gRadPerSec = gRad / dtS;
        float repelSpeed = this.repelSpeed.floatValue() * gRadPerSec / (float) iterations / gRad;
        float attractSpeed = this.attractSpeed.floatValue() / (float) iterations / gRad;


//        float maxSpeedPerIter = (nodeSpeedMax.floatValue() * dtS) * gRad / iterations;


        RectFloat gg = g.bounds;

        for (MutableRectFloat<X> a : nodes) {
            size(a, AUTOSCALE);
            a.clamp(gg);
        }


        float speed = this.speed.floatValue();

        for (int ii = 0; ii < iterations; ii++) {

            for (int aa = 0; aa < n; aa++) {

                MutableRectFloat<X> a = nodes.get(aa);


                attract(a, attractSpeed);

                float ar = a.radius();

                for (int y = aa + 1; y < n; y++)
                    repel(a, ar, nodes.get(y), repelSpeed);

            }
            for (MutableRectFloat a : nodes) {
                a.commitLerp(speed);
                a.clamp(gg);
            }
        }

    }

    protected void size(MutableRectFloat<X> m, float a) {
        float p = (float) (1 + Math.sqrt((double) m.node.pri)) * a;
        m.size(p, p);
    }


    /**
     * HACK this reads the positions from the nodevis not the rectangle
     */
    private void attract(MutableRectFloat a, float attractSpeed) {

        NodeVis<X> from = a.node;
        float px = a.cx(), py = a.cy();


        float aRad = a.radius();

        ConcurrentFastIteratingHashMap<X, EdgeVis<X>> read = from.outs;
        //int neighbors = read.size();

        double[] dx = {(double) 0};
        double[] dy = {(double) 0};

        read.forEachValue(edge -> {
            if (edge == null)
                return; //wtf

            NodeVis<X> who = edge.to;
            if (who == null)
                return;

            MutableRectFloat b = who.mover;
            if (b == null)
                return;

            float idealLen = (aRad + b.radius()) * equilibriumDistFactor;

            v2 delta = new v2(b.cx() - px, b.cy() - py);
            float len = delta.normalize();
//            if (len > idealLen) {
//            len = len * (1+Util.tanhFast(len - (scale)))/2;


                //attractSpeed/=neighbors;

            float s = (len - idealLen) * attractSpeed * weightToVelocity(edge.weight);

                //s = Util.tanhFast(s);...
                //s = (float) Math.sqrt(s);

                if (Math.abs(s) > Spatialization.EPSILONf) {
                    dx[0] = dx[0] + (double) delta.x * s;
                    dy[0] = dy[0] + (double) delta.y * s;
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

        float br = b.radius();
//        ar *= ar;
//        br *= br;


        v2 v2 = aCenter.clone();
        v2.x -= b.cx();
        v2.y -= b.cy();
        jcog.math.v2 delta = v2;
        float len = delta.normalize();
        if (len <= Spatialization.EPSILONf) {
            //coincident, apply random vector
            double theta = (double) (float) ((double) rng.nextFloat() * Math.PI * 2.0);
            float tx = (float) Math.cos(theta);
            float ty = (float) Math.sin(theta);
            delta.scaleClone(Spatialization.EPSILONf * 2.0F);
            delta.set(tx, ty);
        }
        if (len >= maxRepelDist)
            return;

        float radii = (ar + br) * equilibriumDistFactor;
//        len -= (radii * equilibriumDistFactor);
//        if (len < 0)
//            len = 0;

        float s = repelSpeed /
                (1.0F + (len * len));
                //Util.sqr(1 + len);

        if (s > Spatialization.EPSILONf) {

            delta.scaled(s);

            float baRad = br / radii;
            a.move(delta.x * baRad, delta.y * baRad);
            float abRad = -ar / radii;
            b.move(delta.x * abRad, delta.y * abRad);
        }

    }

}
