package spacegraph.space2d.container;

import jcog.Util;
import jcog.list.FasterList;
import jcog.math.FloatRange;
import jcog.tree.rtree.rect.MovingRectFloat2D;
import spacegraph.space2d.Graph2D;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.List;

import static spacegraph.util.math.v3.v;

public class ForceDirected2D<X> implements Graph2D.Graph2DLayout<X> {

    final List<Graph2D.NodeVis<X>> nodes = new FasterList();
    final List<MovingRectFloat2D> bounds = new FasterList();

    int iterations = 1; //TODO problem with immutable bounds being updated after multiple iterations

    public final FloatRange repelSpeed =new FloatRange(0.001f, 0, 10f);
    public final FloatRange attractSpeed =new FloatRange(0.001f, 0, 1f);
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


        maxRepelDist = Math.max(g.w(), g.h()) * 0.25f; //TODO use diagonal, sqrt(2)/2 or something

        minAttractDistRelativeToRadii = 1.1f;

        float repelSpeed = this.repelSpeed.floatValue();
        float attractSpeed = this.attractSpeed.floatValue();
        for (int ii = 0; ii < iterations; ii++) {
            for (int x = 0; x < n; x++)
                for (int y = x + 1; y < n; y++)
                    repel(bounds.get(x), bounds.get(y), repelSpeed);

            for (int a = 0; a < n; a++)
                attract(nodes.get(a), bounds.get(a), attractSpeed);
        }

        v2 center = new v2();
        for (int a = 0; a < n; a++) {
            MovingRectFloat2D A = bounds.get(a);
            center.add(A.cx(), A.cy());
        }
        center.scaled(1f/n); //average
        float recenterX = -center.x + ox + g.bounds.w/2,
                recenterY = -center.y + oy + g.bounds.h/2;
//        for (int a = 0; a < n; a++) {
//            bounds.get(a).move(recenterX, recenterY);
//        }

//        float ocx = g.bounds.cx();
//        float ocy = g.bounds.cy();
        float gw = g.bounds.w;
        float gh = g.bounds.h;
        for (int i = 0; i < n; i++) {
            MovingRectFloat2D b = bounds.get(i);
            //if (!b.isZeroMotion()) {
                //nodes.get(i).pos(b.get(maxMovement, limit));
                nodes.get(i).pos(b.get(recenterX, recenterY, gw, gh));
            //}
        }
    }

    private void attract(Graph2D.NodeVis<X> from, MovingRectFloat2D b, float attractSpeed) {
        v2 p = v(b.cx(), b.cy());
        v2 delta = new v2();

        float fromRad = from.radius();

        for (Graph2D.EdgeVis<X> l : from.edgeOut.read()) {

            Graph2D.NodeVis<X> to = l.to;

            delta.set(to.cx(), to.cy()).subbed(p);

            float lenSq = delta.lengthSquared();
            if (!Float.isFinite(lenSq))
                return;

            lenSq -= Util.sqr(minAttractDistRelativeToRadii *(fromRad +to.radius()) );
            if (lenSq <= 0)
                return;


            delta.normalize();

            //constant speed
            //delta.scale( speed );

            //speed proportional to length
            //float len = (float) Math.sqrt(lenSq);
            //delta.scaled( Math.min(len, len * attractSpeed) );
            delta.scaled( attractSpeed );

            b.move(delta.x, delta.y);
//                    //delta2.scale(-(speed * (yp.mass() /* + yp.mass()*/) ) * len  );
//                    delta.scale(-1 );
//                    ((Body3D) y).velAdd(delta);

        }
    }

    private void repel(MovingRectFloat2D a, MovingRectFloat2D b, float repelSpeed) {

        Tuple2f delta = new v2(a.cx(), a.cy()).subbed(b.cx(), b.cy());

        float len = delta.normalize();
        len -= minAttractDistRelativeToRadii * (a.radius() + b.radius());
        if (len < 0)
            len = 0;
        else if (len >= maxRepelDist)
            return;

        float s = repelSpeed / (1 +
                (len * len)
        );

        v2 v = v(delta.x * s / 2f, delta.y * s / 2f);
        float ar = a.radius();
        float br = b.radius();
        float abr = (ar + br);
        double baRad = br / abr;
        a.move(v.x * baRad, v.y * baRad);
        double abRad = -ar / abr;
        b.move(v.x * abRad, v.y * abRad);

    }

}
