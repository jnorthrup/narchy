package spacegraph.space2d.container;

import jcog.math.FloatRange;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.collision.broad.Broadphase;
import spacegraph.util.math.v3;

import java.util.List;

import static spacegraph.util.math.v3.v;

/**
 * Created by me on 8/24/16.
 */
public class ForceDirected implements spacegraph.space3d.phys.constraint.BroadConstraint {

    public static final int clusters =
            1;
            //13;

    boolean center = true;

    public final FloatRange repel = new FloatRange(8f, 0, 16f);
    public final FloatRange attraction = new FloatRange(0.1f, 0, 3f);



    /** speed at which center correction is applied */
    float centerSpeed = 0.02f;

    final v3 boundsMin, boundsMax;
    final float maxRepelDist;

//        public static class Edge<X> extends MutablePair<X,X> {
//            public final X a, b;
//            public Object aData;
//            public Object bData;
//
//            public Edge(X a, X b) {
//                super(a, b);
//                this.a = a;
//                this.b = b;
//            }
//        }
//
//        final SimpleGraph<X,Edge> graph = new SimpleGraph((a,b)->new Edge(a,b));
//
//        public Edge get(X x, X y) {
//            graph.addVertex(x);
//            graph.addVertex(y);
//            graph.getEdge(x, y);
//        }

    public ForceDirected() {
        float r = 800;
        boundsMin = v(-r, -r, -r);
        boundsMax = v(+r, +r, +r);
        maxRepelDist = r*2;
    }


    @Override
    public void solve(Broadphase b, List<Collidable> objects, float timeStep) {

        int n = objects.size();
        if (n == 0)
            return;

        objects.forEach(c -> {
            Spatial x = ((Spatial) c.data());
            if (x!=null)
                x.stabilize(boundsMin, boundsMax);
        });

        //System.out.print("Force direct " + objects.size() + ": ");
        //final int[] count = {0};
        //count[0] += l.size();
//System.out.print(l.size() + "  ");
        b.forEach((int) Math.ceil((float) n / clusters), objects, this::batch);
        //System.out.println(" total=" + count[0]);


        if (center) {
            float cx = 0, cy = 0, cz = 0;
            for (int i = 0, objectsSize = n; i < objectsSize; i++) {
                v3 c = objects.get(i).transform;
                cx += c.x;
                cy += c.y;
                cz += c.z;
            }
            cx /= -n;
            cy /= -n;
            cz /= -n;

            v3 correction = v3.v(cx, cy, cz);
            if (correction.lengthSquared() > centerSpeed*centerSpeed)
                correction.normalize(centerSpeed);

            for (int i = 0, objectsSize = n; i < objectsSize; i++) {
                objects.get(i).transform.add(correction);
            }

        }


    }

    protected void batch(List<Collidable> l) {

        float speed = repel.floatValue();
        for (int i = 0, lSize = l.size(); i < lSize; i++) {
            Collidable x = l.get(i);
            for (int j = i + 1; j < lSize; j++) {
                repel(x, l.get(j), speed, maxRepelDist);
            }

        }
    }

    protected static void attract(Collidable x, Collidable y, float speed, float idealDist) {
        SimpleSpatial xp = ((SimpleSpatial) x.data());
        SimpleSpatial yp = ((SimpleSpatial) y.data());

        v3 delta = v();
        delta.sub(yp.transform(), xp.transform());

        float lenSq = delta.lengthSquared();
        if (!Float.isFinite(lenSq))
            return;

        if (lenSq < idealDist*idealDist)
            return;


        delta.normalize();
        //delta.scale((speed / (1 + /&xp.mass() /* + yp.mass()*/) ) * len );
        delta.scale( /*(float)(len) * */  speed );
        ((Body3D) x).velAdd(delta);
        //delta2.scale(-(speed * (yp.mass() /* + yp.mass()*/) ) * len  );
        delta.scale(-1 );
        ((Body3D) y).velAdd(delta);

    }

    private static void repel(Collidable x, Collidable y, float speed, float maxDist) {
        SimpleSpatial xp = ((SimpleSpatial) x.data());
        if (xp == null)
            return;
        SimpleSpatial yp = ((SimpleSpatial) y.data());
        if (yp == null)
            return;


        v3 delta = v();
        delta.sub(xp.transform(), yp.transform());

        float len = delta.normalize();
        len -= (xp.radius() + yp.radius());
        if (len < 0)
            len = 0;
        else if (len >= maxDist)
            return;

        float s = speed / ( 1 + ( len*len ));

        v3 v = v(delta.x * s, delta.y * s, delta.z * s );
        ((Body3D) x).velAdd(v);

        v.negate();
        ((Body3D) y).velAdd(v);

    }


}
