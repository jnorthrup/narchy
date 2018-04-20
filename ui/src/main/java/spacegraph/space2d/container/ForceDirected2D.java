package spacegraph.space2d.container;

import jcog.list.FasterList;
import jcog.math.FloatRange;
import spacegraph.space2d.Graph2D;
import spacegraph.util.MovingRectFloat2D;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.List;

import static spacegraph.util.math.v3.v;

public class ForceDirected2D<X> implements Graph2D.Graph2DLayout<X> {

    final List<Graph2D.NodeVis<X>> nodes = new FasterList();
    final List<MovingRectFloat2D> bounds = new FasterList();

    int iterations =1; //TODO problem with immutable bounds being updated after multiple iterations

    public final FloatRange repelSpeed =new FloatRange(2f, 0, 5f);

    /** attractspeed << 0.5 */
    public final FloatRange attractSpeed =new FloatRange(0.005f, 0, 0.05f);
    float maxRepelDist;

    float minAttractDistRelativeToRadii;

    @Override
    public void layout(Graph2D<X> g, int dtMS) {


        nodes.clear();
        bounds.clear();

        float ox = g.bounds.x;
        float oy = g.bounds.y;

        g.forEachValue(v -> {
            if (v.visible()) {
                nodes.add(v);
                MovingRectFloat2D m = new MovingRectFloat2D(v.bounds);
                bounds.add(m);
                m.move(-ox, -oy); //shift to relative coordinates
            }
        });
        int n = nodes.size();
        if (n == 0)
            return;

        maxRepelDist = g.radius()*8;

        minAttractDistRelativeToRadii = 1f;

        int iterations = this.iterations;
        float repelSpeed = this.repelSpeed.floatValue()/iterations;
        float attractSpeed = this.attractSpeed.floatValue()/iterations;
        for (int ii = 0; ii < iterations; ii++) {
            v2 center = new v2();
            for (int x = 0; x < n; x++) {
                MovingRectFloat2D bx = bounds.get(x);
                attract(nodes.get(x), bx, attractSpeed);
                for (int y = x + 1; y < n; y++)
                    repel(bx, bounds.get(y), repelSpeed);

                center.add(bx.cx(), bx.cy());
            }

            center.scaled(1f/n); //average
            for (int x = 0; x < n; x++) {
                bounds.get(x).move(-center.x, -center.y);
            }


        }


        float recenterX =  + ox + g.bounds.w/2,
                recenterY = + oy + g.bounds.h/2;
//        for (int a = 0; a < n; a++) {
//            bounds.get(a).move(recenterX, recenterY);
//        }

//        float ocx = g.bounds.cx();
//        float ocy = g.bounds.cy();
        for (int i = 0; i < n; i++) {
            //if (!b.isZeroMotion()) {
                //nodes.get(i).pos(b.get(maxMovement, limit));
            nodes.get(i).pos(bounds.get(i).get(recenterX, recenterY));
            //}
        }
    }

    private void attract(Graph2D.NodeVis<X> from, MovingRectFloat2D b, float attractSpeed) {
        float px = b.cx(); float py = b.cy();
        v2 delta = new v2();

        float fromRad = from.radius();

        List<Graph2D.EdgeVis<X>> read = from.edgeOut.read();

        for (int i = 0, readSize = read.size(); i < readSize; i++) {

            Graph2D.NodeVis<X> to = read.get(i).to;

            delta.set(to.cx(), to.cy()).subbed(px, py);

            float lenSq = delta.lengthSquared();
            if (!Float.isFinite(lenSq))
                continue;

            float len = (float) Math.sqrt(lenSq);
            len -= (minAttractDistRelativeToRadii * (fromRad + to.radius()));
            if (len <= 0)
                continue;

            //delta.normalize();

            float s = attractSpeed;// * len;
            b.move(delta.x * s, delta.y * s);
        }
    }

    private void repel(MovingRectFloat2D a, MovingRectFloat2D b, float repelSpeed) {

        Tuple2f delta = new v2(a.cx(), a.cy()).subbed(b.cx(), b.cy());

        float len = delta.normalize();

        float ar = a.radius();
        float br = b.radius();
        float abr = (ar + br);

        len -= (abr);
        if (len < 0)
            len = 0;
        else if (len >= maxRepelDist)
            return;

        float s = repelSpeed / ( 1 + (len * len) );

        v2 v = v(delta.x * s, delta.y * s);

        double baRad = 1; //br / abr;
        a.move(v.x * baRad, v.y * baRad);
        double abRad = -1; //-ar / abr;
        b.move(v.x * abRad, v.y * abRad);

    }

}
