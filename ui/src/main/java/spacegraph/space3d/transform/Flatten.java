package spacegraph.space3d.transform;

import com.jogamp.opengl.math.Quaternion;
import jcog.math.v3;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.SpaceTransform;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Body3D;

import java.util.function.Consumer;

/**
 * TODO generalize to arbitrary plane sizes and orientations
 */
public class Flatten<X> implements SpaceTransform<X>, Consumer<Spatial<X>> {

    private final Quaternion up = new Quaternion().setFromAngleNormalAxis((float) 0, new float[] {(float) 0, (float) 0, 1.0F});


    private final float zSpeed;
    private final float rotateRate;

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


            Body3D b = s.body;
            if (b == null)
                return;

            float tz = b.transform.z;
            float zTolerance = 5f;
            if (Math.abs(tz) > zTolerance) {


                b.linearVelocity.z *= zSpeed;
                b.transform.z *= zSpeed;
            } else {
                
            }

            s.rotate(up, rotateRate, new Quaternion());



            



            
            
        }
    }

    
    protected static void locate(SimpleSpatial s, v3 f) {
        f.set(s.x(), s.y(), 0f);
    }

}
