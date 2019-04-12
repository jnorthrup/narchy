package spacegraph.space2d.container.layout;

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
    private final int iterations = 1;
    private float AUTOSCALE = 0f;


    public final FloatRange repelSpeed = new FloatRange(0.05f, 0, 1f);

    public final FloatRange attractSpeed = new FloatRange(0.05f, 0, 1f);

    /** in (visible) graph radii */
    public final FloatRange nodeScale = new FloatRange(0.25f, 0.04f, 1.5f);

    /** in node radii */
    public final FloatRange nodeSpacing  = new FloatRange(2f, 0.25f, 16f);

    /** 1.0 - momentum LERP */
    public final FloatRange speed = new FloatRange(0.5f, 0f, 1f);




    private float maxRepelDist;

    private float equilibriumDistFactor;

    @Override
    public void init(Graph2D<X> g, NodeVis<X> newNode) {
        float rx = g.w()/2*(rng.nextFloat()*2-1), ry = g.h()/2*(rng.nextFloat()*2-1);
        newNode.posXYWH(g.cx() + rx, g.cy() + ry, 1, 1);
    }

    @Override
    protected void layout(Graph2D<X> g, float dtS) {

        int n = nodes.size();
        if (n == 0)
            return;

        float gRad = g.radius();
        float gRadPerSec = gRad / dtS;

        AUTOSCALE = (float) (nodeScale.floatValue() * gRad / Math.sqrt(1f + n));
        assert (AUTOSCALE == AUTOSCALE);


        maxRepelDist = (2 * gRad); //estimate

        equilibriumDistFactor = nodeSpacing.floatValue();


        int iterations = this.iterations;
        float repelSpeed = this.repelSpeed.floatValue() * gRadPerSec / iterations;
        float attractSpeed = this.attractSpeed.floatValue()   / iterations;


//        float maxSpeedPerIter = (nodeSpeedMax.floatValue() * dtS) * gRad / iterations;


        RectFloat gg = g.bounds;

        for (MutableRectFloat<X> a : nodes) {
            size(a, AUTOSCALE);
            a.fenceInside(gg);
        }



        float speed = this.speed.floatValue();

        for (int ii = 0; ii < iterations; ii++) {

            for (int aa = 0; aa < n; aa++) {

                MutableRectFloat<X> a = nodes.get(aa);


                attract(a, attractSpeed);

                final float ar = a.radius();

                for (int y = aa + 1; y < n; y++)
                    repel(a, ar, nodes.get(y), repelSpeed);

            }
            for (MutableRectFloat a : nodes) {
                a.commitLerp(speed);
                a.fenceInside(gg);
            }
        }

    }

    protected void size(MutableRectFloat<X> m, float a) {
        float p = (float) (1f + Math.sqrt(m.node.pri)) * a;
        m.size(p, p);
    }


    /**
     * HACK this reads the positions from the nodevis not the rectangle
     */
    private void attract(MutableRectFloat a, float attractSpeed) {

        NodeVis<X> from = a.node;
        float px = a.cx(), py = a.cy();


        float fromRad = a.radius();

        ConcurrentFastIteratingHashMap<X, EdgeVis<X>> read = from.outs;
        //int neighbors = read.size();

        read.forEachValue(edge -> {
            if (edge == null)
                return; //wtf

            NodeVis<X> who = edge.to;
            if (who == null)
                return;

            MutableRectFloat b = who.mover;
            if (b == null)
                return;

            float idealLen = (fromRad + b.radius()) * equilibriumDistFactor;

            v2 delta = new v2(b.cx() - px, b.cy() - py);
            float len = delta.normalize();
            if (len > idealLen) {
//            len = len * (1+Util.tanhFast(len - (scale)))/2;


                //attractSpeed/=neighbors;

                float s = (len - idealLen) * attractSpeed * weightToVelocity(edge.weight);

                //s = Util.tanhFast(s);...
                //s = (float) Math.sqrt(s);

                if (Math.abs(s) > Spatialization.EPSILONf) {
                    delta.scale(s);
                    a.move(delta.x, delta.y);
                    b.move(-delta.x, -delta.y);
                }
            }
        });

    }

    private float weightToVelocity(float weight) {
        return 1;
        //return weight;
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
        v2 delta = v2;
        float len = delta.normalize();
        if (len <= Spatialization.EPSILONf) {
            //coincident, apply random vector
            double theta = (float) (rng.nextFloat()*Math.PI*2);
            float tx = (float) Math.cos(theta);
            float ty = (float) Math.sin(theta);
            delta.scaleClone(Spatialization.EPSILONf * 2);
            delta.set(tx, ty);
        }
        if (len >= maxRepelDist)
            return;

        float radii = (ar + br) * equilibriumDistFactor;
//        len -= (radii * equilibriumDistFactor);
//        if (len < 0)
//            len = 0;

        float s = repelSpeed /
                (1 + (len * len));
                //Util.sqr(1 + len);

        if (s > Spatialization.EPSILONf) {

            delta.scale(s);

            float baRad = br / radii;
            a.move(delta.x * baRad, delta.y * baRad);
            float abRad = -ar / radii;
            b.move(delta.x * abRad, delta.y * abRad);
        }

    }

}
