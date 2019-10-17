/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http:
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.space3d.phys.constraint;

import jcog.math.v3;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.solve.JacobianEntry;
import spacegraph.util.math.Matrix3f;

/**
 * Point to point constraint between two rigid bodies each with a pivot point that
 * descibes the "ballsocket" location in local space.
 * 
 * @author jezek2
 */
public class Point2PointConstraint extends TypedConstraint {

	private final JacobianEntry[] jac = { new JacobianEntry(), new JacobianEntry(), new JacobianEntry() }; 

	private final v3 pivotInA = new v3();
	private final v3 pivotInB = new v3();

	/** strength */
	public float tau = 0.3f;

	private final float damping = 1f;
	public float impulseClamp;



	public Point2PointConstraint() {
		super(TypedConstraintType.POINT2POINT_CONSTRAINT_TYPE);
	}

	public Point2PointConstraint(Body3D rbA, Body3D rbB, v3 pivotInA, v3 pivotInB) {
		super(TypedConstraintType.POINT2POINT_CONSTRAINT_TYPE, rbA, rbB);
		this.pivotInA.set(pivotInA);
		this.pivotInB.set(pivotInB);
	}

	public Point2PointConstraint(Body3D rbA, v3 pivotInA) {
		super(TypedConstraintType.POINT2POINT_CONSTRAINT_TYPE, rbA);
		this.pivotInA.set(pivotInA);
		this.pivotInB.set(pivotInA);
		rbA.getCenterOfMassTransform(new Transform()).transform(this.pivotInB);
	}


	@Override
	public void buildJacobian() {
		appliedImpulse = 0f;

		v3 normal = new v3();
		normal.set(0f, 0f, 0f);

		Matrix3f tmpMat1 = new Matrix3f();
		Matrix3f tmpMat2 = new Matrix3f();
		v3 tmp1 = new v3();
		v3 tmp2 = new v3();
		v3 tmpVec = new v3();
		
		Transform centerOfMassA = rbA.getCenterOfMassTransform(new Transform());
		Transform centerOfMassB = rbB.getCenterOfMassTransform(new Transform());

		for (int i = 0; i < 3; i++) {
			VectorUtil.setCoord(normal, i, 1f);

			tmpMat1.transpose(centerOfMassA.basis);
			tmpMat2.transpose(centerOfMassB.basis);

			tmp1.set(pivotInA);
			centerOfMassA.transform(tmp1);
			tmp1.sub(rbA.getCenterOfMassPosition(tmpVec));

			tmp2.set(pivotInB);
			centerOfMassB.transform(tmp2);
			tmp2.sub(rbB.getCenterOfMassPosition(tmpVec));

			jac[i].init(
					tmpMat1,
					tmpMat2,
					tmp1,
					tmp2,
					normal,
					rbA.getInvInertiaDiagLocal(new v3()),
					rbA.getInvMass(),
					rbB.getInvInertiaDiagLocal(new v3()),
					rbB.getInvMass());
			VectorUtil.setCoord(normal, i, 0f);
		}
	}



	@Override
	public void solveConstraint(float timeStep) {
		v3 tmp = new v3();
		v3 tmp2 = new v3();
		

		Transform centerOfMassA = rbA.getCenterOfMassTransform(new Transform());
		Transform centerOfMassB = rbB.getCenterOfMassTransform(new Transform());
		
		v3 pivotAInW = new v3(pivotInA);
		centerOfMassA.transform(pivotAInW);

		v3 pivotBInW = new v3(pivotInB);
		centerOfMassB.transform(pivotBInW);



		v3 normal = new v3();
		normal.set(0f, 0f, 0f);

		
		

		v3 rel_pos1 = new v3();
		rel_pos1.sub(pivotAInW, centerOfMassA);
		v3 rel_pos2 = new v3();
		rel_pos2.sub(pivotBInW, centerOfMassB);


		v3 vel1 = rbA.getVelocityInLocalPoint(rel_pos1, new v3());
		v3 vel2 = rbB.getVelocityInLocalPoint(rel_pos2, new v3());
		v3 vel = new v3();
		vel.sub(vel1, vel2);

        float rel_vel = normal.dot(vel);


		for (int i = 0; i < 3; i++) {
			VectorUtil.setCoord(normal, i, 1f);
			float jacDiagABInv = 1f / jac[i].Adiag;


			/*
			
			btScalar rel_vel = m_jac[i].getRelativeVelocity(m_rbA.getLinearVelocity(),angvelA,
			m_rbB.getLinearVelocity(),angvelB);
			 */

			
			tmp.sub(pivotAInW, pivotBInW);
			float depth = -tmp.dot(normal); 

			float impulse = depth * tau / timeStep * jacDiagABInv - damping * rel_vel * jacDiagABInv;

			float impulseClamp = this.impulseClamp;
			if (impulseClamp > 0f) {
				if (impulse < -impulseClamp) {
					impulse = -impulseClamp;
				}
				if (impulse > impulseClamp) {
					impulse = impulseClamp;
				}
			}

			appliedImpulse += impulse;
			v3 impulse_vector = new v3();
			impulse_vector.scale(impulse, normal);
			tmp.sub(pivotAInW, centerOfMassA);
			rbA.impulse(impulse_vector, tmp);
			tmp.negated(impulse_vector);
			tmp2.sub(pivotBInW, centerOfMassB);
			rbB.impulse(tmp, tmp2);

			VectorUtil.setCoord(normal, i, 0f);
		}
	}
	
	public void updateRHS(float timeStep) {
	}

	public void setPivotA(v3 pivotA) {
		pivotInA.set(pivotA);
	}

	public void setPivotB(v3 pivotB) {
		pivotInB.set(pivotB);
	}

	public v3 getPivotInA(v3 out) {
		out.set(pivotInA);
		return out;
	}

	public v3 getPivotInB(v3 out) {
		out.set(pivotInB);
		return out;
	}
	
	
	

}
