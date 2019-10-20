package spacegraph.space3d.phys.constraint;

import jcog.math.v3;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.solve.JacobianEntry;
import spacegraph.util.math.Matrix3f;

import static jcog.math.v3.v;

/**
 * http:
 */
public class DistanceConstraint extends TypedConstraint {

    private final float dist;
    private final float speed;

    private final JacobianEntry jac = new JacobianEntry();

    private final v3 pivotInA = new v3();
    private final v3 pivotInB = new v3();

    /**
     * strength
     */
    private float tau = 0.3f;

    private final float impulseClamp;
    private float error;

    public DistanceConstraint(Body3D rbA, Body3D rbB, float dist, float speed, float impulseClamp, float tau) {
        this(rbA, rbB, v(), v(), dist, speed, impulseClamp, tau);
    }
    private DistanceConstraint(Body3D rbA, Body3D rbB, v3 pivotInA, v3 pivotInB, float dist, float speed, float impulseClamp, float tau) {
        super(TypedConstraintType.POINT2POINT_CONSTRAINT_TYPE, rbA, rbB);
        this.pivotInA.set(pivotInA);
        this.pivotInB.set(pivotInB);
        this.dist = dist;
        this.speed = speed;
        this.impulseClamp = impulseClamp;
        this.tau = tau;
    }


    @Override
    public void solveConstraint(float timeStep) {

        var tmp = new v3();
        var tmp2 = new v3();


        var centerOfMassA = rbA.getCenterOfMassTransform(new Transform());
        var centerOfMassB = rbB.getCenterOfMassTransform(new Transform());

        var pivotAInW = new v3(pivotInA);
        centerOfMassA.transform(pivotAInW);

        var pivotBInW = new v3(pivotInB);
        centerOfMassB.transform(pivotBInW);


        var rel_pos1 = new v3();
        rel_pos1.sub(pivotAInW, centerOfMassA);
        var rel_pos2 = new v3();
        rel_pos2.sub(pivotBInW, centerOfMassB);


        var vel1 = rbA.getVelocityInLocalPoint(rel_pos1, new v3());
        var vel2 = rbB.getVelocityInLocalPoint(rel_pos2, new v3());
        var vel = new v3();
        vel.sub(vel1, vel2);

        var normal = jac.linearJointAxis;
        var rel_vel = vel.dot(normal);


			/*
            
			btScalar rel_vel = m_jac.getRelativeVelocity(m_rbA.getLinearVelocity(),angvelA,
			m_rbB.getLinearVelocity(),angvelB);
			 */

        
        tmp.sub(pivotAInW, pivotBInW);
        var depth = -tmp.dot(normal);

        var damping = 1f;
        var impulse = error * ((depth * tau / timeStep) - (damping * rel_vel ));

        var impulseClamp = this.impulseClamp;
        if (impulseClamp > 0f) {
            if (impulse < -impulseClamp) {
                impulse = -impulseClamp;
            }
            if (impulse > impulseClamp) {
                impulse = impulseClamp;
            }
        }

        appliedImpulse += impulse;
        var impulse_vector = new v3();
        impulse_vector.scale(impulse, normal);

        tmp.sub(pivotAInW, centerOfMassA);
        rbA.impulse(impulse_vector, tmp);
        tmp.negated(impulse_vector);
        tmp2.sub(pivotBInW, centerOfMassB);
        rbB.impulse(tmp, tmp2);


    }

    @Override
    public void buildJacobian() {

        appliedImpulse = 0;

        var posA = rbA.getCenterOfMassTransform(new Transform());
        var posB = rbB.getCenterOfMassTransform(new Transform());

        var relA = new v3(pivotInA);
        posA.transform(relA);

        var relB = new v3(pivotInB);
        posB.transform(relB);

        var del = new v3();
        del.sub(posB, posA);

        var currDist = (float) Math.sqrt(del.dot(del));

        var ortho = del;
        ortho.scaled(1f / currDist);


        Matrix3f tmpMat1 = new Matrix3f(), tmpMat2 = new Matrix3f();
        tmpMat1.transpose(posA.basis);
        tmpMat2.transpose(posB.basis);

        v3 tmp1 = v(pivotInA), tmp2 = v(pivotInB);
        posA.transform(tmp1);
        tmp1.sub(rbA.getCenterOfMassPosition(v()));

        posB.transform(tmp2);
        tmp2.sub(rbB.getCenterOfMassPosition(v()));

        jac.init(
                tmpMat1,
                tmpMat2,
                tmp1,
                tmp2,
                ortho,
                rbA.getInvInertiaDiagLocal(new v3()),
                rbA.getInvMass(),
                rbB.getInvInertiaDiagLocal(new v3()),
                rbB.getInvMass());

        this.error = (currDist - dist) * speed;

        


    }
}
