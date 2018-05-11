package spacegraph.space2d.container;

import jcog.Util;
import jcog.data.pool.DequePool;
import jcog.list.FasterList;
import jcog.math.FloatRange;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.util.MovingRectFloat2D;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.List;

import static spacegraph.util.math.v3.v;

public class ForceDirected2D<X> implements Graph2D.Graph2DLayout<X> {

    final List<Graph2D.NodeVis<X>> nodes = new FasterList();
    final List<MovingRectFloat2D> bounds = new FasterList();
    final DequePool<MovingRectFloat2D> boundsPool = new DequePool<MovingRectFloat2D>(128) {
        @Override
        public MovingRectFloat2D create() {
            return new MovingRectFloat2D();
        }
    };

    int iterations = 1;

    public final FloatRange repelSpeed =new FloatRange(0.1f, 0, 0.2f);

    /** attractspeed << 0.5 */
    public final FloatRange attractSpeed =new FloatRange(0.02f, 0, 0.1f);
    float maxRepelDist;

    float minAttractDistRelativeToRadii;

    @Override
    public void layout(Graph2D<X> g, int dtMS) {


        nodes.clear();
        boundsPool.take(bounds);

        float ox = g.bounds.x;
        float oy = g.bounds.y;
        float recenterX =  + ox + g.bounds.w/2,
                recenterY = + oy + g.bounds.h/2;

        g.forEachValue(v -> {
            if (v.visible()) {
                nodes.add(v);
                MovingRectFloat2D m = boundsPool.get();
                m.set(v.bounds);
                m.move(-recenterX, -recenterY, 1f); //shift to relative coordinates
                bounds.add(m);
            }
        });
        int n = nodes.size();
        if (n == 0)
            return;

        maxRepelDist = g.radius();

        minAttractDistRelativeToRadii = 1f;

        int iterations = this.iterations;
        float repelSpeed = this.repelSpeed.floatValue()/iterations;
        float attractSpeed = this.attractSpeed.floatValue()/iterations;
        for (int ii = 0; ii < iterations; ii++) {
            for (int x = 0; x < n; x++) {
                MovingRectFloat2D bx = bounds.get(x);
                attract(nodes.get(x), bx, attractSpeed);
                for (int y = x + 1; y < n; y++)
                    repel(bx, bounds.get(y), repelSpeed);

            }


            for (int x = 0; x < n; x++) {
                bounds.get(x).moveTo(0, 0, 0.01f);
            }


        }


        v2 center = new v2();
        for (int x = 0; x < n; x++) {
            MovingRectFloat2D bx = bounds.get(x);
            center.add(bx.cx(), bx.cy());
        }
        center.scaled(1f/n); //average
        float tx = Util.lerp(0.5f, recenterX - center.x, recenterX);
        float ty = Util.lerp(0.5f, recenterY - center.y, recenterY);
        for (int i = 0; i < n; i++) {
            //if (!b.isZeroMotion()) {
                //nodes.get(i).pos(b.get(maxMovement, limit));
            nodes.get(i).pos(bounds.get(i).get(tx, ty));
            //}
        }
    }

    /** HACK this reads the positions from the nodevis not the rectangle */
    private void attract(Graph2D.NodeVis<X> from, MovingRectFloat2D b, float attractSpeed) {
        float px = from.cx(); float py = from.cy();
        v2 delta = new v2();

        float fromRad = from.radius();

        List<Graph2D.EdgeVis<X>> read = from.edgeOut.read();

        v2 total = new v2();
        for (int i = 0, readSize = read.size(); i < readSize; i++) {

            Graph2D.EdgeVis<X> edge = read.get(i);

            Graph2D.NodeVis<X> to = edge.to;

            delta.set(to.cx(), to.cy()).subbed(px, py);

            float lenSq = delta.lengthSquared();
            if (!Float.isFinite(lenSq))
                continue;

            float len = (float) Math.sqrt(lenSq);
            len -= (minAttractDistRelativeToRadii * (fromRad + to.radius()));
            if (len <= 0)
                continue;

            delta.normalize();


            float s = attractSpeed * len * weightToVelocity(edge.weight);
            total.add(delta.x * s, delta.y * s);
        }

        b.move(total.x, total.y);
    }

    protected float weightToVelocity(float weight) {
        return weight*weight; //curved response
    }

    private void repel(MovingRectFloat2D a, MovingRectFloat2D b, float repelSpeed) {

        Tuple2f delta = new v2(a.cx(), a.cy()).subbed(b.cx(), b.cy());

        float len = delta.normalize();

        float ar = a.radius();
        float br = b.radius();

        //for proportionality to area, not linear
        ar *= ar;
        br *= br;

        float abr = (ar + br);

        len -= (abr);
        if (len < 0)
            len = 0;
        else if (len >= maxRepelDist)
            return;

        float s = repelSpeed / ( 1 + (len * len) );

        v2 v = v(delta.x * s, delta.y * s);

        double baRad = br / abr;
        a.move(v.x * baRad, v.y * baRad);
        double abRad = -ar / abr;
        b.move(v.x * abRad, v.y * abRad);

    }

}
