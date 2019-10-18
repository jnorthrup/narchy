/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * btConeTwistConstraint is Copyright (c) 2007 Starbreeze Studios
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
 * 
 * Written by: Marcus Hennix
 */

package spacegraph.space3d.phys.constraint;

import jcog.math.v3;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.math.QuaternionUtil;
import spacegraph.space3d.phys.math.ScalarUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.TransformUtil;
import spacegraph.space3d.phys.solve.JacobianEntry;
import spacegraph.util.math.Matrix3f;
import spacegraph.util.math.Quat4f;

/**
 * ConeTwistConstraint can be used to simulate ragdoll joints (upper arm, leg etc).
 * 
 * @author jezek2
 */
public class ConeTwistConstraint extends TypedConstraint {

	private final JacobianEntry[] jac/*[3]*/ = { new JacobianEntry(), new JacobianEntry(), new JacobianEntry() }; 

	private final Transform rbAFrame = new Transform();
	private final Transform rbBFrame = new Transform();

	private float limitSoftness;
	private float biasFactor;
	private float relaxationFactor;

	private float swingSpan1;
	private float swingSpan2;
	private float twistSpan;

	private final v3 swingAxis = new v3();
	private final v3 twistAxis = new v3();

	private float kSwing;
	private float kTwist;

	private float twistLimitSign;
	private float swingCorrection;
	private float twistCorrection;

	private float accSwingLimitImpulse;
	private float accTwistLimitImpulse;

	private boolean angularOnly;
	private boolean solveTwistLimit;
	private boolean solveSwingLimit;

	public ConeTwistConstraint() {
		super(TypedConstraintType.CONETWIST_CONSTRAINT_TYPE);
	}

	public ConeTwistConstraint(Body3D rbA, Body3D rbB, Transform rbAFrame, Transform rbBFrame) {
		super(TypedConstraintType.CONETWIST_CONSTRAINT_TYPE, rbA, rbB);
		this.rbAFrame.set(rbAFrame);
		this.rbBFrame.set(rbBFrame);

		swingSpan1 = 1e30f;
		swingSpan2 = 1e30f;
		twistSpan = 1e30f;
		biasFactor = 0.3f;
		relaxationFactor = 1.0f;

		solveTwistLimit = false;
		solveSwingLimit = false;
	}

	public ConeTwistConstraint(Body3D rbA, Transform rbAFrame) {
		super(TypedConstraintType.CONETWIST_CONSTRAINT_TYPE, rbA);
		this.rbAFrame.set(rbAFrame);
		this.rbBFrame.set(this.rbAFrame);

		swingSpan1 = 1e30f;
		swingSpan2 = 1e30f;
		twistSpan = 1e30f;
		biasFactor = 0.3f;
		relaxationFactor = 1.0f;

		solveTwistLimit = false;
		solveSwingLimit = false;
	}
	
	@Override
	public void buildJacobian() {
		v3 tmp = new v3();
		v3 tmp1 = new v3();
		v3 tmp2 = new v3();

		Transform tmpTrans = new Transform();

		appliedImpulse = 0f;

		
		swingCorrection = 0f;
		twistLimitSign = 0f;
		solveTwistLimit = false;
		solveSwingLimit = false;
		accTwistLimitImpulse = 0f;
		accSwingLimitImpulse = 0f;

		if (!angularOnly) {
			v3 pivotAInW = new v3(rbAFrame);
			rbA.getCenterOfMassTransform(tmpTrans).transform(pivotAInW);

			v3 pivotBInW = new v3(rbBFrame);
			rbB.getCenterOfMassTransform(tmpTrans).transform(pivotBInW);

			v3 relPos = new v3();
			relPos.sub(pivotBInW, pivotAInW);

			
			v3[] normal/*[3]*/ = {new v3(), new v3(), new v3()};
			if (relPos.lengthSquared() > BulletGlobals.FLT_EPSILON) {
				normal[0].normalize(relPos);
			}
			else {
				normal[0].set(1f, 0f, 0f);
			}

			TransformUtil.planeSpace1(normal[0], normal[1], normal[2]);

			for (int i = 0; i < 3; i++) {
				Matrix3f mat1 = rbA.getCenterOfMassTransform(new Transform()).basis;
				mat1.transpose();

				Matrix3f mat2 = rbB.getCenterOfMassTransform(new Transform()).basis;
				mat2.transpose();

				tmp1.sub(pivotAInW, rbA.getCenterOfMassPosition(tmp));
				tmp2.sub(pivotBInW, rbB.getCenterOfMassPosition(tmp));

				jac[i].init(
						mat1,
						mat2,
						tmp1,
						tmp2,
						normal[i],
						rbA.getInvInertiaDiagLocal(new v3()),
						rbA.getInvMass(),
						rbB.getInvInertiaDiagLocal(new v3()),
						rbB.getInvMass());
			}
		}

		v3 b1Axis1 = new v3(), b1Axis2 = new v3(), b1Axis3 = new v3();
		v3 b2Axis1 = new v3(), b2Axis2 = new v3();

		rbAFrame.basis.getColumn(0, b1Axis1);
		getRigidBodyA().getCenterOfMassTransform(tmpTrans).basis.transform(b1Axis1);

		rbBFrame.basis.getColumn(0, b2Axis1);
		getRigidBodyB().getCenterOfMassTransform(tmpTrans).basis.transform(b2Axis1);

		float swing1 = 0f;

        float swx = 0f, swy = 0f;
		float thresh = 10f;
		float fact;

		
		if (swingSpan1 >= 0.05f) {
			rbAFrame.basis.getColumn(1, b1Axis2);
			getRigidBodyA().getCenterOfMassTransform(tmpTrans).basis.transform(b1Axis2);

			swx = b2Axis1.dot(b1Axis1);
			swy = b2Axis1.dot(b1Axis2);
			swing1 = ScalarUtil.atan2Fast(swy, swx);
			fact = (swy*swy + swx*swx) * thresh * thresh;
            fact /= (fact + 1f);
			swing1 *= fact;
		}

        float swing2 = 0f;
        if (swingSpan2 >= 0.05f) {
			rbAFrame.basis.getColumn(2, b1Axis3);
			getRigidBodyA().getCenterOfMassTransform(tmpTrans).basis.transform(b1Axis3);

			swx = b2Axis1.dot(b1Axis1);
			swy = b2Axis1.dot(b1Axis3);
			swing2 = ScalarUtil.atan2Fast(swy, swx);
			fact = (swy*swy + swx*swx) * thresh * thresh;
            fact /= (fact + 1f);
			swing2 *= fact;
		}

		float RMaxAngle1Sq = 1.0f / (swingSpan1 * swingSpan1);
		float RMaxAngle2Sq = 1.0f / (swingSpan2 * swingSpan2);
		float EllipseAngle = Math.abs(swing1*swing1) * RMaxAngle1Sq + Math.abs(swing2*swing2) * RMaxAngle2Sq;

		if (EllipseAngle > 1.0f) {
			swingCorrection = EllipseAngle - 1.0f;
			solveSwingLimit = true;

			
			tmp1.scale(b2Axis1.dot(b1Axis2), b1Axis2);
			tmp2.scale(b2Axis1.dot(b1Axis3), b1Axis3);
			tmp.add(tmp1, tmp2);
			swingAxis.cross(b2Axis1, tmp);
			swingAxis.normalize();

			float swingAxisSign = (b2Axis1.dot(b1Axis1) >= 0.0f) ? 1.0f : -1.0f;
			swingAxis.scaled(swingAxisSign);

			kSwing = 1f / (getRigidBodyA().computeAngularImpulseDenominator(swingAxis) +
					getRigidBodyB().computeAngularImpulseDenominator(swingAxis));

		}

		
		if (twistSpan >= 0f) {
			
			rbBFrame.basis.getColumn(1, b2Axis2);
			getRigidBodyB().getCenterOfMassTransform(tmpTrans).basis.transform(b2Axis2);

			Quat4f rotationArc = QuaternionUtil.shortestArcQuat(b2Axis1, b1Axis1, new Quat4f());
			v3 TwistRef = QuaternionUtil.quatRotate(rotationArc, b2Axis2, new v3());
			float twist = ScalarUtil.atan2Fast(TwistRef.dot(b1Axis3), TwistRef.dot(b1Axis2));

			float lockedFreeFactor = (twistSpan > 0.05f) ? limitSoftness : 0f;
			if (twist <= -twistSpan * lockedFreeFactor) {
				twistCorrection = -(twist + twistSpan);
				solveTwistLimit = true;

				twistAxis.add(b2Axis1, b1Axis1);
				twistAxis.scaled(0.5f);
				twistAxis.normalize();
				twistAxis.scaled(-1.0f);

				kTwist = 1f / (getRigidBodyA().computeAngularImpulseDenominator(twistAxis) +
						getRigidBodyB().computeAngularImpulseDenominator(twistAxis));

			}
			else if (twist > twistSpan * lockedFreeFactor) {
				twistCorrection = (twist - twistSpan);
				solveTwistLimit = true;

				twistAxis.add(b2Axis1, b1Axis1);
				twistAxis.scaled(0.5f);
				twistAxis.normalize();

				kTwist = 1f / (getRigidBodyA().computeAngularImpulseDenominator(twistAxis) +
						getRigidBodyB().computeAngularImpulseDenominator(twistAxis));
			}
		}
	}

	@Override
	public void solveConstraint(float timeStep) {
		v3 tmp = new v3();
		v3 tmp2 = new v3();

		v3 tmpVec = new v3();
		Transform tmpTrans = new Transform();

		v3 pivotAInW = new v3(rbAFrame);
		rbA.getCenterOfMassTransform(tmpTrans).transform(pivotAInW);

		v3 pivotBInW = new v3(rbBFrame);
		rbB.getCenterOfMassTransform(tmpTrans).transform(pivotBInW);


        if (!angularOnly) {
			v3 rel_pos1 = new v3();
			rel_pos1.sub(pivotAInW, rbA.getCenterOfMassPosition(tmpVec));

			v3 rel_pos2 = new v3();
			rel_pos2.sub(pivotBInW, rbB.getCenterOfMassPosition(tmpVec));

			v3 vel1 = rbA.getVelocityInLocalPoint(rel_pos1, new v3());
			v3 vel2 = rbB.getVelocityInLocalPoint(rel_pos2, new v3());
			v3 vel = new v3();
			vel.sub(vel1, vel2);

            float tau = 0.3f;
            for (int i = 0; i < 3; i++) {
				v3 normal = jac[i].linearJointAxis;
                float jacDiagABInv = 1f / jac[i].Adiag;

                float rel_vel = normal.dot(vel);
				
				tmp.sub(pivotAInW, pivotBInW);
				float depth = -(tmp).dot(normal); 
				float impulse = depth * tau / timeStep * jacDiagABInv - rel_vel * jacDiagABInv;
				appliedImpulse += impulse;
				v3 impulse_vector = new v3();
				impulse_vector.scale(impulse, normal);

				tmp.sub(pivotAInW, rbA.getCenterOfMassPosition(tmpVec));
				rbA.impulse(impulse_vector, tmp);

				tmp.negated(impulse_vector);
				tmp2.sub(pivotBInW, rbB.getCenterOfMassPosition(tmpVec));
				rbB.impulse(tmp, tmp2);
			}
		}

        
        v3 angVelA = getRigidBodyA().getAngularVelocity(new v3());
        v3 angVelB = getRigidBodyB().getAngularVelocity(new v3());

        
        if (solveSwingLimit) {
            tmp.sub(angVelB, angVelA);
            float amplitude = ((tmp).dot(swingAxis) * relaxationFactor * relaxationFactor + swingCorrection * (1f / timeStep) * biasFactor);
            float impulseMag = amplitude * kSwing;

            
            float temp = accSwingLimitImpulse;
            accSwingLimitImpulse = Math.max(accSwingLimitImpulse + impulseMag, 0.0f);
            impulseMag = accSwingLimitImpulse - temp;

            v3 impulse = new v3();
            impulse.scale(impulseMag, swingAxis);

            rbA.torqueImpulse(impulse);

            tmp.negated(impulse);
            rbB.torqueImpulse(tmp);
        }

        
        if (solveTwistLimit) {
            tmp.sub(angVelB, angVelA);
            float amplitude = ((tmp).dot(twistAxis) * relaxationFactor * relaxationFactor + twistCorrection * (1f / timeStep) * biasFactor);
            float impulseMag = amplitude * kTwist;

            
            float temp = accTwistLimitImpulse;
            accTwistLimitImpulse = Math.max(accTwistLimitImpulse + impulseMag, 0.0f);
            impulseMag = accTwistLimitImpulse - temp;

            v3 impulse = new v3();
            impulse.scale(impulseMag, twistAxis);

            rbA.torqueImpulse(impulse);

            tmp.negated(impulse);
            rbB.torqueImpulse(tmp);
        }
    }

	public void updateRHS(float timeStep) {
	}

	public void setAngularOnly(boolean angularOnly) {
		this.angularOnly = angularOnly;
	}

	public void setLimit(float _swingSpan1, float _swingSpan2, float _twistSpan) {
		setLimit(_swingSpan1, _swingSpan2, _twistSpan, 0.8f, 0.3f, 1.0f);
	}

	private void setLimit(float _swingSpan1, float _swingSpan2, float _twistSpan, float _softness, float _biasFactor, float _relaxationFactor) {
		swingSpan1 = _swingSpan1;
		swingSpan2 = _swingSpan2;
		twistSpan = _twistSpan;

		limitSoftness = _softness;
		biasFactor = _biasFactor;
		relaxationFactor = _relaxationFactor;
	}

	public Transform getAFrame(Transform out) {
		out.set(rbAFrame);
		return out;
	}

	public Transform getBFrame(Transform out) {
		out.set(rbBFrame);
		return out;
	}

	public boolean getSolveTwistLimit() {
		return solveTwistLimit;
	}

	public boolean getSolveSwingLimit() {
		return solveTwistLimit;
	}

	public float getTwistLimitSign() {
		return twistLimitSign;
	}
	
}
