package spacegraph.space2d.container;

import jcog.Util;
import jcog.list.FasterList;
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

    float repelSpeed = 2;
    float attractSpeed= 2;
    float maxDist, maxMovement;
    float idealDistProportionalToRadii;

    @Override
    public void layout(Graph2D<X> g, int dtMS) {


        nodes.clear();
        bounds.clear();
        g.forEachValue(v -> {
            if (v.visible()) {
                nodes.add(v);
                bounds.add(new MovingRectFloat2D(v.bounds));
            }
        });
        int n = nodes.size();
        if (n == 0)
            return;


        maxDist = Math.max(g.w(), g.h()) * 1f; //TODO use diagonal, sqrt(2)/2 or something
        maxMovement = Math.max(g.w(), g.h()) * 0.25f;
        idealDistProportionalToRadii = 1.5f;

        for (int ii = 0; ii < iterations; ii++) {
            for (int x = 0; x < n; x++)
                for (int y = x + 1; y < n; y++)
                    repel(bounds.get(x), bounds.get(y));

            for (int a = 0; a < n; a++)
                attract(nodes.get(a), bounds.get(a));
        }

        v2 center = new v2();
        for (int a = 0; a < n; a++) {
            MovingRectFloat2D A = bounds.get(a);
            center.add(A.cx(), A.cy());
        }
        center.scaled(1/n);
        center.normalize();
        float centerSpeed = 5f;
        center.scaled(centerSpeed);
        float recenterX = -center.x, recenterY = -center.y;
        for (int a = 0; a < n; a++) {
            bounds.get(a).move(recenterX, recenterY);
        }



        for (int i = 0; i < n; i++) {
            MovingRectFloat2D b = bounds.get(i);
            if (!b.isZeroMotion()) {
                //nodes.get(i).pos(b.get(maxMovement, limit));
                nodes.get(i).pos(b.get(maxMovement, g.bounds));
            }
        }
    }

    private void attract(Graph2D.NodeVis<X> from, MovingRectFloat2D b) {
        v2 p = v(b.cx(), b.cy());
        v2 delta = new v2();

        float fromRad = from.radius();

        for (Graph2D.Link<X> l : from.edgeOut.read()) {

            Graph2D.NodeVis<X> to = l.to;

            delta.set(p).subbed(to.cx(), to.cy());

            float lenSq = delta.lengthSquared();
            if (!Float.isFinite(lenSq))
                return;

            if (lenSq < Util.sqr(idealDistProportionalToRadii *(fromRad +to.radius()) ))
                return;


            delta.normalize();

            //constant speed
            //delta.scale( speed );

            //speed proportional to length
            float len = (float) Math.sqrt(lenSq);
            delta.scale( Math.min(len, len * attractSpeed ) );

            b.move(-delta.x, -delta.y);
//                    //delta2.scale(-(speed * (yp.mass() /* + yp.mass()*/) ) * len  );
//                    delta.scale(-1 );
//                    ((Body3D) y).velAdd(delta);

        }
    }

    private void repel(MovingRectFloat2D a, MovingRectFloat2D b) {

        Tuple2f delta = new v2(a.cx(), a.cy()).subbed(b.cx(), b.cy());

        float len = delta.normalize();
        len -= idealDistProportionalToRadii * (a.radius() + b.radius());
        if (len < 0)
            len = 0;
        else if (len >= maxDist)
            return;

        float s = repelSpeed / (1 + (len * len));

        v2 v = v(delta.x * s / 2f, delta.y * s / 2f);
        a.move(v.x, v.y);
        b.move(-v.x, -v.y);

    }

}
