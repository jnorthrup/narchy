package spacegraph.space3d.phys;

import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.CollisionShape;

import static spacegraph.util.math.v3.v;

/** dynamic which allows no z-movement or rotation */
public class FlatDynamic extends Body3D {
    public FlatDynamic(float mass, CollisionShape shape, Transform transform, short group, short mask) {
        super(mass, transform, shape);
        this.group = group;
        this.mask = mask;
        if (mass != 0f) { // rigidbody is dynamic if and only if mass is non zero, otherwise static
            shape.calculateLocalInertia(mass, v());
        }
    }

//    @Override
//    public v3 getAngularVelocity(v3 out) {
//        angularVelocity.zero();
//        return super.getAngularVelocity(out);
//    }
//
//    @Override
//    public void setAngularVelocity(v3 ang_vel) {
//        //super.setAngularVelocity(ang_vel);
//    }
//
//    @Override
//    public void torque(v3 torque) {
//        //super.torque(torque);
//    }

//    @Override
//    public void force(v3 f) {
//        totalForce.add(f.x, f.y, 0);
//    }

//    @Override
//    public void torqueImpulse(v3 torque) {
//        super.torqueImpulse(torque);
//    }

//    @Override
//    public void impulse(v3 impulse) {
//        super.impulse(impulse);
//        linearVelocity.z = 0;
//    }

    //    @Override
//    public void setAngularFactor(float angFac) {
//        super.setAngularFactor(angFac);
//    }

//    @Override
//    public void proceedToTransform(Transform newTrans) {
//        newTrans.z = 0;
//        super.proceedToTransform(newTrans);
//    }

//    @Override
//    public v3 getLinearVelocity(v3 out) {
//        linearVelocity.z = 0;
//        return super.getLinearVelocity(out);
//    }
//
//    @Override
//    public void setLinearVelocity(v3 out) {
//        super.setLinearVelocity(out);
//        linearVelocity.z = 0;
//    }
}
