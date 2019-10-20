package nars.experiment.rover;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.v3;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.phys.Collisions;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.collision.narrow.VoronoiSimplexSolver;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.video.Draw;

import static jcog.math.v3.v;

/**
 * one retina pixel
 */
public class RetinaPixel extends Collisions.RayResultCallback {
    public v3 localPosition;
    public v3 worldPosition;
    public v3 localDirection;
    public v3 worldTarget;
    public v3 worldHit = v();
    float r;
    float g;
    float b;
    float a;
    public float rangeMax;
    private final SimpleSpatial parent;
    private final VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();

    public RetinaPixel(SimpleSpatial parent) {
        this.parent = parent;
    }

    public void update(Dynamics3D d) {
        Transform x = parent.transform();

        worldPosition = x.transform(v(localPosition));

        worldTarget = v(localDirection);
        worldTarget.scaled(rangeMax);
        worldTarget.add(localPosition);
        x.transform(worldTarget);


        r = g = b = (float) 0;
        a = distanceToAlpha(rangeMax);
        worldHit.set(worldTarget);

        simplexSolver.reset();
        d.rayTest(worldPosition, worldTarget, this, simplexSolver);
    }

    public void render(GL2 gl) {

        if (a > (float) 0) {
            gl.glColor4f(r, g, b, a);
            gl.glLineWidth(4f);
            Draw.line(gl, worldPosition, worldTarget);
        }
    }

    @Override
    public float addSingleResult(Collisions.LocalRayResult rayResult, boolean normalInWorldSpace) {
        Object target = rayResult.collidable.data();
        if (target != parent) {
            float dist = v3.dist(worldPosition, rayResult.hitNormal);

            worldHit.set(rayResult.hitNormal);
            if (target instanceof SimpleSpatial) {
                SimpleSpatial ss = ((SimpleSpatial) target);
                r = ss.shapeColor[0];
                g = ss.shapeColor[1];
                b = ss.shapeColor[2];
                a = distanceToAlpha(dist);
            }
        }
        return (float) 0;
    }

    float distanceToAlpha(float dist) {

        return Util.unitize(1f - (dist / rangeMax)) * 0.5f + 0.5f;
    }
}
