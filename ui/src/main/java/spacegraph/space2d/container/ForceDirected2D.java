package spacegraph.space2d.container;

import jcog.math.FloatRange;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.util.MovingRectFloat2D;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.List;
import java.util.Random;

public class ForceDirected2D<X> extends DynamicLayout2D<X, MovingRectFloat2D> {

    final Random rng = new XoRoShiRo128PlusRandom(1);
    private int iterations = 2;
    private float AUTOSCALE = 0f;


    @Override
    protected MovingRectFloat2D newContainer() {
        return new MovingRectFloat2D();
    }

    public final FloatRange repelSpeed = new FloatRange(4f, 0, 8f);

    public final FloatRange attractSpeed = new FloatRange(0.25f, 0, 1f);

    public final FloatRange nodeScale = new FloatRange(0.5f, 0.04f, 1.5f);

    public final FloatRange nodeSpacing  = new FloatRange(1f, 1f, 4f);

    public final FloatRange maxSpeed  = new FloatRange(5f, 0f, 100f);



    private float maxRepelDist;

    private float equilibriumDist;

    @Override
    public void initialize(Graph2D<X> g, Graph2D.NodeVis<X> n) {
        float rx = g.w()/2*(rng.nextFloat()*2-1), ry = g.h()/2*(rng.nextFloat()*2-1);
        n.posXYWH(g.cx() + rx, g.cy() + ry, 1, 1);
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

        for (MovingRectFloat2D m : nodes) {
            float pri = m.node.pri;
            float p = (float) (1f + Math.sqrt(pri)) * AUTOSCALE;
            m.size(p, p);
        }

        maxRepelDist = (float) ((2 * g.radius()) * Math.sqrt(2) / 2); //estimate

        equilibriumDist = nodeSpacing.floatValue();


        int iterations = this.iterations;
        float repelSpeed = this.repelSpeed.floatValue() / iterations;
        float attractSpeed = this.attractSpeed.floatValue() / iterations;

        float maxSpeedPerIter = maxSpeed.floatValue() / iterations;

        for (int ii = 0; ii < iterations; ii++) {

            for (int x = 0; x < n; x++) {
                MovingRectFloat2D bx = nodes.get(x);
                attract(bx, attractSpeed);
                for (int y = x + 1; y < n; y++)
                    repel(bx, nodes.get(y), repelSpeed);

            }

            RectFloat2D gg = g.bounds;
            for (MovingRectFloat2D b : nodes) {
                b.limitSpeed(maxSpeedPerIter);
                b.fence(gg);
            }


        }

    }


    /**
     * HACK this reads the positions from the nodevis not the rectangle
     */
    private void attract(MovingRectFloat2D b, float attractSpeed) {

        Graph2D.NodeVis<X> from = b.node;
        float px = b.cx(), py = b.cy();


        float fromRad = b.radius();

        List<Graph2D.EdgeVis<X>> read = from.edgeOut.read();
        //int neighbors = read.size();

        v2 total = new v2();
        read.forEach(edge -> {
            if (edge == null)
                return; //wtf

            MovingRectFloat2D to = edge.to.mover;
            if (to == null)
                return;

            v2 delta = new v2(to.cx(), to.cy());
            delta.subbed(px, py);

            float scale = fromRad + to.radius();
            float len = delta.normalize() - (scale * equilibriumDist);
//            len = len * (1+Util.tanhFast(len - (scale)))/2;


            //attractSpeed/=neighbors;

            float s = attractSpeed * len * weightToVelocity(edge.weight);
            total.add(delta.x * s, delta.y * s);
        });

        b.move(total.x, total.y);
    }

    private float weightToVelocity(float weight) {
        return weight * weight;
    }

    private void repel(MovingRectFloat2D a, MovingRectFloat2D b, float repelSpeed) {

        Tuple2f delta = new v2(a.cx(), a.cy()).subbed(b.cx(), b.cy());


        float ar = a.radius();
        float br = b.radius();
//        ar *= ar;
//        br *= br;

        float scale = (ar + br);

        float len = delta.normalize() - scale; // -  (scale*equilibriumDist);
        if (len < Spatialization.EPSILONf) {
            //coincident, apply random vector
            double theta = (float) (rng.nextFloat()*Math.PI*2);
            float tx = (float) Math.cos(theta);
            float ty = (float) Math.sin(theta);
            delta.set(tx, ty);
            len = 0;
        } else if (len >= maxRepelDist)
            return;

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
