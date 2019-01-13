package spacegraph.space3d.phys.collision;

import jcog.math.v3;
import spacegraph.space3d.phys.Collisions;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;

/**
 * Created by me on 7/22/16.
 */
public class ClosestRay extends Collisions.RayResultCallback {
    private final v3 rayFromWorld = new v3();
    public final v3 rayToWorld = new v3();

    private final v3 hitNormalWorld = new v3();
    public final v3 hitPointWorld = new v3();

    public ClosestRay(short group) {
        collisionFilterGroup = group;
    }

    public ClosestRay(v3 rayFromWorld, v3 rayToWorld) {
        set(rayFromWorld, rayToWorld);
    }

    private ClosestRay set(v3 rayFromWorld, v3 rayToWorld) {
        this.rayFromWorld.set(rayFromWorld);
        this.rayToWorld.set(rayToWorld);
        hitNormalWorld.zero();
        hitPointWorld.zero();
        closestHitFraction = 1f;
        collidable = null;
        return this;
    }

    @Override
    public float addSingleResult(Collisions.LocalRayResult rayResult, boolean normalInWorldSpace) {
        
        float f = rayResult.hitFraction;
        if ((f > closestHitFraction))
            throw new RuntimeException();

        closestHitFraction = f;
        collidable = rayResult.collidable;

        hitNormalWorld.set(rayResult.hitNormal);
        if (!normalInWorldSpace) {
            
            collidable.getWorldTransform(new Transform()).transform(hitNormalWorld);
        }

        VectorUtil.setInterpolate3(hitPointWorld, rayFromWorld, rayToWorld, f);
        return f;
    }
}
