package spacegraph.space3d.phys;

import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.CollisionShape;

import static jcog.math.v3.v;

/** dynamic which allows no z-movement or rotation */
class FlatDynamic extends Body3D {
    public FlatDynamic(float mass, CollisionShape shape, Transform transform, short group, short mask) {
        super(mass, transform, shape);
        this.group = group;
        this.mask = mask;
        if (mass != 0f) { 
            shape.calculateLocalInertia(mass, v());
        }
    }

































    





















}
