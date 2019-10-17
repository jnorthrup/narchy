package spacegraph.space3d.phys.constraint;

import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.Broadphase;

import java.util.List;


/** for applying NxN interactions */
@FunctionalInterface
public interface BroadConstraint {
    void solve(Broadphase b, List<Collidable> objects, float timeStep);
}
