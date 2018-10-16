package spacegraph.space2d.container.layout;

import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.math.FloatRange;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.Graph2D;
import spacegraph.util.MutableFloatRect;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.Random;

public class ForceDirected2D<X> extends DynamicLayout2D<X, MutableFloatRect> {

    final Random rng = new XoRoShiRo128PlusRandom(1);
    private int iterations = 1;
    private float AUTOSCALE = 0f;


    @Override
    protected MutableFloatRect newContainer() {
        return new MutableFloatRect();
    }

    public final FloatRange repelSpeed = new FloatRange(4f, 0, 16f);

    public final FloatRange attractSpeed = new FloatRange(0.25f, 0, 16f);

    public final FloatRange nodeScale = new FloatRange(0.25f, 0.04f, 1.5f);


    public final FloatRange nodeSpacing  = new FloatRange(1f, 0.1f, 4f);

    public final FloatRange needSpeedLimit = new FloatRange(25f, 0f, 100f);

    public final FloatRange nodeMomentum = new FloatRange(0.5f, 0f, 1f);


    private float maxRepelDist;

    private float equilibriumDist;

    @Override
    public void init(Graph2D<X> g, Graph2D.NodeVis<X> newNode) {
        float rx = g.w()/2*(rng.nextFloat()*2-1), ry = g.h()/2*(rng.nextFloat()*2-1);
        newNode.posXYWH(g.cx() + rx, g.cy() + ry, 1, 1);
    }

//    @Override protected void put(MovingRectFloat2D mover, Graph2D.NodeVis node) {
//        node.posX0Y0WH(
//                Util.lerp(_momentum, mover.x, node.bounds.x),
//                Util.lerp(_momentum, mover.y, node.bounds.y),
//                mover.w, mover.h);
//    }

    @Override
    protected void layout(Graph2D<X> g) {

        int n = nodes.size();
        if (n == 0)
            return;

        AUTOSCALE = nodeScale.floatValue() *
                (float) (Math.min(g.bounds.w, g.bounds.h)
                        / Math.sqrt(1f + n));

        assert (AUTOSCALE == AUTOSCALE);

        for (MutableFloatRect m : nodes) {
            float pri = m.node.pri;
            float p = (float) (1f + Math.sqrt(pri)) * AUTOSCALE;
            m.size(p, p);
        }

        maxRepelDist = (float) ((2 * g.radius()) * Math.sqrt(2) / 2); //estimate

        equilibriumDist = nodeSpacing.floatValue();


        int iterations = this.iterations;
        float repelSpeed = this.repelSpeed.floatValue() / iterations;
        float attractSpeed = this.attractSpeed.floatValue() / iterations;

        float maxSpeedPerIter = needSpeedLimit.floatValue() / iterations;

        for (int ii = 0; ii < iterations; ii++) {

            for (int x = 0; x < n; x++) {
                MutableFloatRect a = nodes.get(x);

                attract(a, attractSpeed);

                final float ar = a.radius();
                final v2 aCenter = new v2(a.cx(), a.cy());

                for (int y = x + 1; y < n; y++)
                    repel(a, aCenter, ar, nodes.get(y), repelSpeed);

            }

            RectFloat gg = g.bounds;
            float momentum = nodeMomentum.floatValue();
            for (MutableFloatRect b : nodes) {
                b.commit(maxSpeedPerIter, momentum);
                b.fence(gg);
            }


        }

    }


    /**
     * HACK this reads the positions from the nodevis not the rectangle
     */
    private void attract(MutableFloatRect b, float attractSpeed) {

        Graph2D.NodeVis<X> from = b.node;
        float px = b.cx(), py = b.cy();


        float fromRad = b.radius();

        ConcurrentFastIteratingHashMap<X, Graph2D.EdgeVis<X>> read = from.outs;
        //int neighbors = read.size();

        v2 total = new v2();
        read.forEachValue(edge -> {
            if (edge == null)
                return; //wtf

            Graph2D.NodeVis<X> who = edge.to;
            if (who == null)
                return;

            MutableFloatRect to = who.mover;
            if (to == null)
                return;

            v2 delta = new v2(to.cx(), to.cy());
            delta.subbed(px, py);

            float scale = fromRad + to.radius();
            float len = delta.normalize();
            if (len > (scale * equilibriumDist)) {
//            len = len * (1+Util.tanhFast(len - (scale)))/2;


                //attractSpeed/=neighbors;

                float s = attractSpeed * len * weightToVelocity(edge.weight);
                total.add(delta.x * s, delta.y * s);
            }
        });

        b.move(total.x, total.y);
    }

    private float weightToVelocity(float weight) {
        return weight * weight;
    }

    private void repel(MutableFloatRect a, v2 aCenter, float ar, MutableFloatRect b, float repelSpeed) {

        Tuple2f delta = aCenter.clone().subbed(b.cx(), b.cy());



        float br = b.radius();
//        ar *= ar;
//        br *= br;

        float scale = (ar + br);

        float len = delta.normalize();
        if (len < Spatialization.EPSILONf) {
            //coincident, apply random vector
            double theta = (float) (rng.nextFloat()*Math.PI*2);
            float tx = (float) Math.cos(theta);
            float ty = (float) Math.sin(theta);
            delta.set(tx, ty);
            len = 0;
        } else if (len >= maxRepelDist)
            return;

        len -= (scale) * equilibriumDist;
        len = Math.max(0, len);

        float s = repelSpeed /
                (1 + (len * len));
                //Util.sqr(1 + len);

        delta.scaled(s);

        double baRad = br / scale;
        a.move(delta.x * baRad, delta.y * baRad);
        double abRad = -ar / scale;
        b.move(delta.x * abRad, delta.y * abRad);

    }

}
