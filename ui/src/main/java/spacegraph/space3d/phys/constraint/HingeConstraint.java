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

/* Hinge Constraint by Dirk Gregorius. Limits added by Marcus Hennix at Starbreeze Studios */

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
 * Hinge constraint between two rigid bodies each with a pivot point that descibes
 * the axis location in local space. Axis defines the orientation of the hinge axis.
 * 
 * @author jezek2
 */
public class HingeConstraint extends TypedConstraint {

	private final JacobianEntry[] jac/*[3]*/ = { new JacobianEntry(), new JacobianEntry(), new JacobianEntry() }; 
	private final JacobianEntry[] jacAng/*[3]*/ = { new JacobianEntry(), new JacobianEntry(), new JacobianEntry() }; 

	private final Transform rbAFrame = new Transform(); 
	private final Transform rbBFrame = new Transform();

	private float motorTargetVelocity;
	private float maxMotorImpulse;

	private float limitSoftness;
	private float biasFactor;
	private float relaxationFactor;

	private float lowerLimit;
	private float upperLimit;

	private float kHinge;

	private float limitSign;
	private float correction;

	private float accLimitImpulse;

	private boolean angularOnly;
	private boolean enableAngularMotor;
	private boolean solveLimit;

	public HingeConstraint() {
		super(TypedConstraintType.HINGE_CONSTRAINT_TYPE);
		enableAngularMotor = false;
	}

	public HingeConstraint(Body3D rbA, Body3D rbB, v3 pivotInA, v3 pivotInB, v3 axisInA, v3 axisInB) {
		super(TypedConstraintType.HINGE_CONSTRAINT_TYPE, rbA, rbB);
		angularOnly = false;
		enableAngularMotor = false;

		rbAFrame.set(pivotInA);

		
		v3 rbAxisA1 = new v3();
		v3 rbAxisA2 = new v3();

		Transform centerOfMassA = rbA.getCenterOfMassTransform(new Transform());
		centerOfMassA.basis.getColumn(0, rbAxisA1);
		float projection = axisInA.dot(rbAxisA1);

		if (projection >= 1.0f - BulletGlobals.SIMD_EPSILON) {
			centerOfMassA.basis.getColumn(2, rbAxisA1);
			rbAxisA1.negated();
			centerOfMassA.basis.getColumn(1, rbAxisA2);
		} else if (projection <= -1.0f + BulletGlobals.SIMD_EPSILON) {
			centerOfMassA.basis.getColumn(2, rbAxisA1);
			centerOfMassA.basis.getColumn(1, rbAxisA2);
		} else {
			rbAxisA2.cross(axisInA, rbAxisA1);
			rbAxisA1.cross(rbAxisA2, axisInA);
		}

		rbAFrame.basis.setRow(0, rbAxisA1.x, rbAxisA2.x, axisInA.x);
		rbAFrame.basis.setRow(1, rbAxisA1.y, rbAxisA2.y, axisInA.y);
		rbAFrame.basis.setRow(2, rbAxisA1.z, rbAxisA2.z, axisInA.z);

		Quat4f rotationArc = QuaternionUtil.shortestArcQuat(axisInA, axisInB, new Quat4f());
		v3 rbAxisB1 = QuaternionUtil.quatRotate(rotationArc, rbAxisA1, new v3());
		v3 rbAxisB2 = new v3();
		rbAxisB2.cross(axisInB, rbAxisB1);

		rbBFrame.set(pivotInB);
		rbBFrame.basis.setRow(0, rbAxisB1.x, rbAxisB2.x, -axisInB.x);
		rbBFrame.basis.setRow(1, rbAxisB1.y, rbAxisB2.y, -axisInB.y);
		rbBFrame.basis.setRow(2, rbAxisB1.z, rbAxisB2.z, -axisInB.z);

		
		lowerLimit = 1e30f;
		upperLimit = -1e30f;
		biasFactor = 0.3f;
		relaxationFactor = 1.0f;
		limitSoftness = 0.9f;
		solveLimit = false;
	}

	public HingeConstraint(Body3D rbA, v3 pivotInA, v3 axisInA) {
		super(TypedConstraintType.HINGE_CONSTRAINT_TYPE, rbA);
		angularOnly = false;
		enableAngularMotor = false;

		
		
		v3 rbAxisA1 = new v3();
		Transform centerOfMassA = rbA.getCenterOfMassTransform(new Transform());
		centerOfMassA.basis.getColumn(0, rbAxisA1);

		float projection = rbAxisA1.dot(axisInA);
		if (projection > BulletGlobals.FLT_EPSILON) {
			rbAxisA1.scaled(projection);
			rbAxisA1.sub(axisInA);
		}
		else {
			centerOfMassA.basis.getColumn(1, rbAxisA1);
		}

		v3 rbAxisA2 = new v3();
		rbAxisA2.cross(axisInA, rbAxisA1);

		rbAFrame.set(pivotInA);
		rbAFrame.basis.setRow(0, rbAxisA1.x, rbAxisA2.x, axisInA.x);
		rbAFrame.basis.setRow(1, rbAxisA1.y, rbAxisA2.y, axisInA.y);
		rbAFrame.basis.setRow(2, rbAxisA1.z, rbAxisA2.z, axisInA.z);

		v3 axisInB = new v3();
		axisInB.negated(axisInA);
		centerOfMassA.basis.transform(axisInB);

		Quat4f rotationArc = QuaternionUtil.shortestArcQuat(axisInA, axisInB, new Quat4f());
		v3 rbAxisB1 = QuaternionUtil.quatRotate(rotationArc, rbAxisA1, new v3());
		v3 rbAxisB2 = new v3();
		rbAxisB2.cross(axisInB, rbAxisB1);

		rbBFrame.set(pivotInA);
		centerOfMassA.transform(rbBFrame);
		rbBFrame.basis.setRow(0, rbAxisB1.x, rbAxisB2.x, axisInB.x);
		rbBFrame.basis.setRow(1, rbAxisB1.y, rbAxisB2.y, axisInB.y);
		rbBFrame.basis.setRow(2, rbAxisB1.z, rbAxisB2.z, axisInB.z);

		
		lowerLimit = 1e30f;
		upperLimit = -1e30f;
		biasFactor = 0.3f;
		relaxationFactor = 1.0f;
		limitSoftness = 0.9f;
		solveLimit = false;
	}

	public HingeConstraint(Body3D rbA, Body3D rbB, Transform rbAFrame, Transform rbBFrame) {
		super(TypedConstraintType.HINGE_CONSTRAINT_TYPE, rbA, rbB);
		this.rbAFrame.set(rbAFrame);
		this.rbBFrame.set(rbBFrame);
		angularOnly = false;
		enableAngularMotor = false;

		
		this.rbBFrame.basis.m02 *= -1f;
		this.rbBFrame.basis.m12 *= -1f;
		this.rbBFrame.basis.m22 *= -1f;

		
		lowerLimit = 1e30f;
		upperLimit = -1e30f;
		biasFactor = 0.3f;
		relaxationFactor = 1.0f;
		limitSoftness = 0.9f;
		solveLimit = false;
	}

	public HingeConstraint(Body3D rbA, Transform rbAFrame) {
		super(TypedConstraintType.HINGE_CONSTRAINT_TYPE, rbA);
		this.rbAFrame.set(rbAFrame);
		this.rbBFrame.set(rbAFrame);
		angularOnly = false;
		enableAngularMotor = false;

		

		
		this.rbBFrame.basis.m02 *= -1f;
		this.rbBFrame.basis.m12 *= -1f;
		this.rbBFrame.basis.m22 *= -1f;

		this.rbBFrame.set(this.rbAFrame);
		rbA.getCenterOfMassTransform(new Transform()).transform(this.rbBFrame);

		
		lowerLimit = 1e30f;
		upperLimit = -1e30f;
		biasFactor = 0.3f;
		relaxationFactor = 1.0f;
		limitSoftness = 0.9f;
		solveLimit = false;
	}
	
	@Override
	public void buildJacobian() {
		v3 tmp = new v3();
		v3 tmp1 = new v3();
		v3 tmp2 = new v3();
		v3 tmpVec = new v3();
		Matrix3f mat1 = new Matrix3f();
		Matrix3f mat2 = new Matrix3f();
		
		Transform centerOfMassA = rbA.getCenterOfMassTransform(new Transform());
		Transform centerOfMassB = rbB.getCenterOfMassTransform(new Transform());

		appliedImpulse = 0f;

		if (!angularOnly) {
			v3 pivotAInW = new v3(rbAFrame);
			centerOfMassA.transform(pivotAInW);

			v3 pivotBInW = new v3(rbBFrame);
			centerOfMassB.transform(pivotBInW);

			v3 relPos = new v3();
			relPos.sub(pivotBInW, pivotAInW);

			v3[] normal/*[3]*/ = {new v3(), new v3(), new v3()};
			if (relPos.lengthSquared() > BulletGlobals.FLT_EPSILON) {
				normal[0].set(relPos);
				normal[0].normalize();
			}
			else {
				normal[0].set(1f, 0f, 0f);
			}

			TransformUtil.planeSpace1(normal[0], normal[1], normal[2]);

			for (int i = 0; i < 3; i++) {
				mat1.transpose(centerOfMassA.basis);
				mat2.transpose(centerOfMassB.basis);

				tmp1.sub(pivotAInW, rbA.getCenterOfMassPosition(tmpVec));
				tmp2.sub(pivotBInW, rbB.getCenterOfMassPosition(tmpVec));

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

		
		

		
		v3 jointAxis0local = new v3();
		v3 jointAxis1local = new v3();

		rbAFrame.basis.getColumn(2, tmp);
		TransformUtil.planeSpace1(tmp, jointAxis0local, jointAxis1local);

		
		

		v3 jointAxis0 = new v3(jointAxis0local);
		centerOfMassA.basis.transform(jointAxis0);

		v3 jointAxis1 = new v3(jointAxis1local);
		centerOfMassA.basis.transform(jointAxis1);

		v3 hingeAxisWorld = new v3();
		rbAFrame.basis.getColumn(2, hingeAxisWorld);
		centerOfMassA.basis.transform(hingeAxisWorld);

		mat1.transpose(centerOfMassA.basis);
		mat2.transpose(centerOfMassB.basis);
		jacAng[0].init(jointAxis0,
				mat1,
				mat2,
				rbA.getInvInertiaDiagLocal(new v3()),
				rbB.getInvInertiaDiagLocal(new v3()));

		
		jacAng[1].init(jointAxis1,
				mat1,
				mat2,
				rbA.getInvInertiaDiagLocal(new v3()),
				rbB.getInvInertiaDiagLocal(new v3()));

		
		jacAng[2].init(hingeAxisWorld,
				mat1,
				mat2,
				rbA.getInvInertiaDiagLocal(new v3()),
				rbB.getInvInertiaDiagLocal(new v3()));

		
		float hingeAngle = getHingeAngle();

		
		correction = 0f;
		limitSign = 0f;
		solveLimit = false;
		accLimitImpulse = 0f;

		if (lowerLimit < upperLimit) {
			if (hingeAngle <= lowerLimit * limitSoftness) {
				correction = (lowerLimit - hingeAngle);
				limitSign = 1.0f;
				solveLimit = true;
			}
			else if (hingeAngle >= upperLimit * limitSoftness) {
				correction = upperLimit - hingeAngle;
				limitSign = -1.0f;
				solveLimit = true;
			}
		}

		
		v3 axisA = new v3();
		rbAFrame.basis.getColumn(2, axisA);
		centerOfMassA.basis.transform(axisA);

		kHinge = 1.0f / (getRigidBodyA().computeAngularImpulseDenominator(axisA) +
				getRigidBodyB().computeAngularImpulseDenominator(axisA));
	}

	@Override
	public void solveConstraint(float timeStep) {
		v3 tmp = new v3();
		v3 tmp2 = new v3();
		v3 tmpVec = new v3();

		Transform centerOfMassA = rbA.getCenterOfMassTransform(new Transform());
		Transform centerOfMassB = rbB.getCenterOfMassTransform(new Transform());
		
		v3 pivotAInW = new v3(rbAFrame);
		centerOfMassA.transform(pivotAInW);

		v3 pivotBInW = new v3(rbBFrame);
		centerOfMassB.transform(pivotBInW);


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


        

        
        v3 axisA = new v3();
        rbAFrame.basis.getColumn(2, axisA);
        centerOfMassA.basis.transform(axisA);

        v3 axisB = new v3();
        rbBFrame.basis.getColumn(2, axisB);
        centerOfMassB.basis.transform(axisB);

        v3 angVelA = getRigidBodyA().getAngularVelocity(new v3());
        v3 angVelB = getRigidBodyB().getAngularVelocity(new v3());

        v3 angVelAroundHingeAxisA = new v3();
        angVelAroundHingeAxisA.scale(axisA.dot(angVelA), axisA);

        v3 angVelAroundHingeAxisB = new v3();
        angVelAroundHingeAxisB.scale(axisB.dot(angVelB), axisB);

        v3 angAorthog = new v3();
        angAorthog.sub(angVelA, angVelAroundHingeAxisA);

        v3 angBorthog = new v3();
        angBorthog.sub(angVelB, angVelAroundHingeAxisB);

        v3 velrelOrthog = new v3();
        velrelOrthog.sub(angAorthog, angBorthog);


        float len = velrelOrthog.length();
        if (len > 0.00001f) {
            v3 normal = new v3();
            normal.normalize(velrelOrthog);

            float denom = getRigidBodyA().computeAngularImpulseDenominator(normal) +
                    getRigidBodyB().computeAngularImpulseDenominator(normal);
            
            
            velrelOrthog.scaled((1f / denom) * relaxationFactor);
        }

        
        
        
        v3 angularError = new v3();
        angularError.cross(axisA, axisB);
        angularError.negated();
        angularError.scaled(1f / timeStep);
        float len2 = angularError.length();
        if (len2 > 0.00001f) {
            v3 normal2 = new v3();
            normal2.normalize(angularError);

            float denom2 = getRigidBodyA().computeAngularImpulseDenominator(normal2) +
                    getRigidBodyB().computeAngularImpulseDenominator(normal2);
            float relaxation = 1f;
            angularError.scaled((1f / denom2) * relaxation);
        }

        tmp.negated(velrelOrthog);
        tmp.add(angularError);
        rbA.torqueImpulse(tmp);

        tmp.sub(velrelOrthog, angularError);
        rbB.torqueImpulse(tmp);

        
        if (solveLimit) {
            tmp.sub(angVelB, angVelA);
            float amplitude = ((tmp).dot(axisA) * relaxationFactor + correction * (1f / timeStep) * biasFactor) * limitSign;

            float impulseMag = amplitude * kHinge;

            
            float temp = accLimitImpulse;
            accLimitImpulse = Math.max(accLimitImpulse + impulseMag, 0f);
            impulseMag = accLimitImpulse - temp;

            v3 impulse = new v3();
            impulse.scale(impulseMag * limitSign, axisA);

            rbA.torqueImpulse(impulse);

            tmp.negated(impulse);
            rbB.torqueImpulse(tmp);
        }

        
        if (enableAngularMotor) {
            
            v3 angularLimit = new v3();
            angularLimit.set(0f, 0f, 0f);

            v3 velrel = new v3();
            velrel.sub(angVelAroundHingeAxisA, angVelAroundHingeAxisB);
            float projRelVel = velrel.dot(axisA);

            float desiredMotorVel = motorTargetVelocity;
            float motor_relvel = desiredMotorVel - projRelVel;

            float unclippedMotorImpulse = kHinge * motor_relvel;
            
            float clippedMotorImpulse = Math.min(unclippedMotorImpulse, maxMotorImpulse);
            clippedMotorImpulse = Math.max(clippedMotorImpulse, -maxMotorImpulse);
            v3 motorImp = new v3();
            motorImp.scale(clippedMotorImpulse, axisA);

            tmp.add(motorImp, angularLimit);
            rbA.torqueImpulse(tmp);

            tmp.negated(motorImp);
            tmp.sub(angularLimit);
            rbB.torqueImpulse(tmp);
        }
    }

	public void updateRHS(float timeStep) {
	}

	private float getHingeAngle() {
		Transform centerOfMassA = rbA.getCenterOfMassTransform(new Transform());
		Transform centerOfMassB = rbB.getCenterOfMassTransform(new Transform());
		
		v3 refAxis0 = new v3();
		rbAFrame.basis.getColumn(0, refAxis0);
		centerOfMassA.basis.transform(refAxis0);

		v3 refAxis1 = new v3();
		rbAFrame.basis.getColumn(1, refAxis1);
		centerOfMassA.basis.transform(refAxis1);

		v3 swingAxis = new v3();
		rbBFrame.basis.getColumn(1, swingAxis);
		centerOfMassB.basis.transform(swingAxis);

		return ScalarUtil.atan2Fast(swingAxis.dot(refAxis0), swingAxis.dot(refAxis1));
	}
	
	public void setAngularOnly(boolean angularOnly) {
		this.angularOnly = angularOnly;
	}

	public void enableAngularMotor(boolean enableMotor, float targetVelocity, float maxMotorImpulse) {
		this.enableAngularMotor = enableMotor;
		this.motorTargetVelocity = targetVelocity;
		this.maxMotorImpulse = maxMotorImpulse;
	}

	public void setLimit(float low, float high) {
		setLimit(low, high, 0.9f, 0.3f, 1.0f);
	}

	private void setLimit(float low, float high, float _softness, float _biasFactor, float _relaxationFactor) {
		lowerLimit = low;
		upperLimit = high;

		limitSoftness = _softness;
		biasFactor = _biasFactor;
		relaxationFactor = _relaxationFactor;
	}

	public float getLowerLimit() {
		return lowerLimit;
	}

	public float getUpperLimit() {
		return upperLimit;
	}

	public Transform getAFrame(Transform out) {
		out.set(rbAFrame);
		return out;
	}

	public Transform getBFrame(Transform out) {
		out.set(rbBFrame);
		return out;
	}

	public boolean getSolveLimit() {
		return solveLimit;
	}

	public float getLimitSign() {
		return limitSign;
	}

	public boolean getAngularOnly() {
		return angularOnly;
	}

	public boolean getEnableAngularMotor() {
		return enableAngularMotor;
	}

	public float getMotorTargetVelosity() {
		return motorTargetVelocity;
	}

	public float getMaxMotorImpulse() {
		return maxMotorImpulse;
	}
	
}
