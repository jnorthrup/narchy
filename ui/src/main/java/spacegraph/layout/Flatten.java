package spacegraph.layout;

import com.jogamp.opengl.math.Quaternion;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceTransform;
import spacegraph.Spatial;
import spacegraph.math.v3;
import spacegraph.phys.Dynamic;

import java.util.function.Consumer;

/**
 * TODO generalize to arbitrary plane sizes and orientations
 */
public class Flatten<X> implements SpaceTransform<X>, Consumer<Spatial<X>> {

    private final Quaternion up = new Quaternion().setFromAngleNormalAxis(0, new float[] { 0,0,1});


    private final float zTolerance = 0.01f;

    float zSpeed;
    float rotateRate;

    public Flatten(float zSpeed, float rotateRate) {
        this.zSpeed = zSpeed; this.rotateRate = rotateRate;
    }

    @Override
    public void update(Iterable<Spatial<X>> g, float dt) {
        g.forEach(this);
    }

    @Override
    public void accept(Spatial<X> ss) {

        if (ss instanceof SimpleSpatial) {
            SimpleSpatial s = (SimpleSpatial) ss;
            //v3 f = v();
            //locate(s, f);
            //s.move(f, rate);

            Dynamic b = s.body;
            if (b == null)
                return;

            float tz = b.transform.z;
            if (Math.abs(tz) > zTolerance) {
//                b.velAdd(v( 0, 0,
//                        -(tz > 0 ? (tz - zTolerance) : (tz + zTolerance)) * zSpeed));
                b.linearVelocity.z *= zSpeed;
                b.transform.z *= zSpeed;
            } else {
                b.linearVelocity.z = 0;
                b.transform.z = 0;
            }
            s.rotate(up, rotateRate, new Quaternion());



            //dampening: keep upright and eliminate z-component of linear velocity
//            b.linearVelocity.scale(
//                    1f, 1f, 0.9f
//            );
            //b.angularVelocity.scale(1f-rotateRate);
            //b.angularVelocity.zero();
        }
    }

    //TODO abstract this
    protected static void locate(SimpleSpatial s, v3 f) {
        f.set(s.x(), s.y(), 0f);
    }

}
