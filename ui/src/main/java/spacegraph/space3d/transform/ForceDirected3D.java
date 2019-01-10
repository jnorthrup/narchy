package spacegraph.space3d.transform;

import jcog.math.FloatRange;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.Broadphase;
import jcog.math.v3;

import java.util.List;

import static jcog.math.v3.v;

/**
 * Created by me on 8/24/16.
 */
public class ForceDirected3D implements spacegraph.space3d.phys.constraint.BroadConstraint {

    private final int iterations;
    private boolean center = true;

    public final FloatRange expand = new FloatRange(14f, 0, 32f);
    public final FloatRange condense = new FloatRange(0.05f, 0, 3f);



    /** speed at which center correction is applied */
    private float centerSpeed = 0.02f;

    private final v3 boundsMin;
    private final v3 boundsMax;
    private final float maxRepelDist;





















    ForceDirected3D() {
        float r = 800;
        boundsMin = v(-r, -r, -r);
        boundsMax = v(+r, +r, +r);
        maxRepelDist = r*2;
        iterations = 1;
    }


    @Override
    public void solve(Broadphase b, List<Collidable> objects, float timeStep) {

        int n = objects.size();
        if (n == 0)
            return;

        for (int iter = 0; iter < iterations; iter++) {

            objects.forEach(c -> {
                Spatial x = ((Spatial) c.data());
                if (x != null)
                    x.stabilize(boundsMin, boundsMax);
            });

            
            
            
            
            int clusters = (int) Math.ceil(/*Math.log*/(((float)n) / 32));
            if (clusters % 2 == 0)
                clusters++; 

            b.forEach((int) Math.ceil((float) n / clusters), objects, this::batch);
            


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
                if (correction.lengthSquared() > centerSpeed * centerSpeed)
                    correction.normalize(centerSpeed);

                for (int i = 0, objectsSize = n; i < objectsSize; i++) {
                    objects.get(i).transform.add(correction);
                }

            }
        }

    }

    private void batch(List<Collidable> l) {

        float speed = expand.floatValue();
        for (int i = 0, lSize = l.size(); i < lSize; i++) {
            Collidable x = l.get(i);
            for (int j = i + 1; j < lSize; j++) {
                repel(x, l.get(j), speed, maxRepelDist);
            }

        }
    }

    static void attract(Collidable x, Collidable y, float speed, float idealDist) {
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

        
        

        
        float len = (float) Math.sqrt(lenSq);
        delta.scale( Math.min(len, len*  speed ) );

        ((Body3D) x).velAdd(delta);
        
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

        v.negated();
        ((Body3D) y).velAdd(v);

    }


}
