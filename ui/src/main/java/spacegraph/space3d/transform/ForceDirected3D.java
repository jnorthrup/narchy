package spacegraph.space3d.transform;

import jcog.math.FloatRange;
import jcog.math.v3;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.Broadphase;

import java.util.List;
import java.util.Objects;

import static jcog.math.v3.v;

/**
 * Created by me on 8/24/16.
 */
public class ForceDirected3D implements spacegraph.space3d.phys.constraint.BroadConstraint {

    public final FloatRange expand = new FloatRange(1f, 0, 32f);
    public final FloatRange condense = new FloatRange(1f, 0, 3f);
    private final int iterations;

    private final v3 boundsMin;
    private final v3 boundsMax;
    private final float maxRepelDist;


    ForceDirected3D() {
        float r = 100;
        boundsMin = v(-r, -r, -r);
        boundsMax = v(+r, +r, +r);
        maxRepelDist = r * 2;
        iterations = 1;
    }

    static void attract(Collidable x, Collidable y, float speed, float idealDist) {
        var xp = ((SimpleSpatial) x.data());
        var yp = ((SimpleSpatial) y.data());

        var delta = v();
        delta.sub(yp.transform(), xp.transform());

        var lenSq = delta.lengthSquared();
        if (!Float.isFinite(lenSq))
            return;

        if (lenSq < idealDist * idealDist)
            return;


        delta.normalize();


        var len = (float) Math.sqrt(lenSq);
        delta.scaled(Math.min(len, len * speed));

        ((Body3D) x).velAdd(delta);

        delta.scaled(-1);
        ((Body3D) y).velAdd(delta);

    }

    private static void repel(Collidable x, Collidable y, float speed, float maxDist) {
        var xp = ((SimpleSpatial) x.data());
        if (xp == null)
            return;
        var yp = ((SimpleSpatial) y.data());
        if (yp == null)
            return;


        var delta = v();
        delta.sub(xp.transform(), yp.transform());

        var len = delta.normalize();
        len -= (xp.radius() + yp.radius());
        if (len < Float.MIN_NORMAL)
            len = 0;
        else if (len >= maxDist)
            return;

        var s = speed / (1 + (len * len));

        var v = v(delta.x * s, delta.y * s, delta.z * s);
        ((Body3D) x).velAdd(v);

        v.negated();
        ((Body3D) y).velAdd(v);

    }

    @Override
    public void solve(Broadphase b, List<Collidable> objects, float timeStep) {

        var n = objects.size();
        if (n == 0)
            return;

        for (var iter = 0; iter < iterations; iter++) {

            objects.stream().map(c -> ((Spatial) c.data())).filter(Objects::nonNull).forEach(x -> x.stabilize(boundsMin, boundsMax));


            var clusters = (int) Math.ceil(/*Math.log*/(((float) n) / 32));
            if (clusters % 2 == 0)
                clusters++;

            b.forEach((int) Math.ceil((float) n / clusters), objects, this::batch);


            var center = true;
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

                var correction = v3.v(cx, cy, cz);
                /**
                 * speed at which center correction is applied
                 */
                var centerSpeed = 0.1f;
                if (correction.lengthSquared() > centerSpeed * centerSpeed)
                    correction.normalize(centerSpeed);

                for (int i = 0, objectsSize = n; i < objectsSize; i++) {
                    objects.get(i).transform.add(correction);
                }

            }
        }

    }

    private void batch(List<Collidable> l) {

        var speed = expand.floatValue();
        for (int i = 0, lSize = l.size(); i < lSize; i++) {
            var x = l.get(i);
            for (var j = i + 1; j < lSize; j++) {
                repel(x, l.get(j), speed, maxRepelDist);
            }

        }
    }


}
