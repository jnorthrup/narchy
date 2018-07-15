package spacegraph.space2d.container;

import jcog.Util;
import jcog.math.FloatRange;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.util.MovingRectFloat2D;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.List;

import static spacegraph.util.math.v3.v;

public class ForceDirected2D<X> extends DynamicLayout2D<X, MovingRectFloat2D> {

    private int iterations = 1;

    @Override
    protected MovingRectFloat2D newContainer() {
        return new MovingRectFloat2D();
    }

    public final FloatRange repelSpeed =new FloatRange(0.02f, 0, 0.5f);

    /** attractspeed << 0.5 */
    public final FloatRange attractSpeed =new FloatRange(0.005f, 0, 0.025f);
    private float maxRepelDist;

    private float minAttractDistRelativeToRadii;


    @Override protected void layoutDynamic(Graph2D<X> g) {

        maxRepelDist = (float) ((2*g.radius()) * Math.sqrt(2)/2); //estimate

        minAttractDistRelativeToRadii = 1f;

        int iterations = this.iterations;
        float repelSpeed = this.repelSpeed.floatValue()/iterations;
        float attractSpeed = this.attractSpeed.floatValue()/iterations;
        int n = bounds.size();
        for (int ii = 0; ii < iterations; ii++) {
            for (int x = 0; x < n; x++) {
                MovingRectFloat2D bx = bounds.get(x);
                attract(nodes.get(x), bx, attractSpeed);
                for (int y = x + 1; y < n; y++)
                    repel(bx, bounds.get(y), repelSpeed);

            }


            for (MovingRectFloat2D bound : bounds) {
                bound.moveTo(0, 0, 0.01f);
            }


        }


        v2 center = new v2();
        for (MovingRectFloat2D bx : bounds) {
            center.add(bx.cx(), bx.cy());
        }
        center.scaled(1f/n); 
        tx = Util.lerp(0.5f, recenterX - center.x, recenterX);
        ty = Util.lerp(0.5f, recenterY - center.y, recenterY);
    }


    /** HACK this reads the positions from the nodevis not the rectangle */
    private void attract(Graph2D.NodeVis<X> from, MovingRectFloat2D b, float attractSpeed) {
        float px = from.cx(); float py = from.cy();
        v2 delta = new v2();

        float fromRad = from.radius();

        List<Graph2D.EdgeVis<X>> read = from.edgeOut.read();

        v2 total = new v2();
        for (Graph2D.EdgeVis<X> edge : read) {

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

    private float weightToVelocity(float weight) {
        return weight*weight; 
    }

    private void repel(MovingRectFloat2D a, MovingRectFloat2D b, float repelSpeed) {

        Tuple2f delta = new v2(a.cx(), a.cy()).subbed(b.cx(), b.cy());

        float len = delta.normalize();

        float ar = a.radius();
        float br = b.radius();

        
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
        double abRad = ar / abr;
        b.move(v.x * -abRad, v.y * abRad);

    }

}
