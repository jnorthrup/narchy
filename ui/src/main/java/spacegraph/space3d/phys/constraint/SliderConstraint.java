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

/*
Added by Roman Ponomarev (rponom@gmail.com)
April 04, 2008

TODO:
 - add clamping od accumulated impulse to improve stability
 - add conversion for ODE constraint solver
*/

package spacegraph.space3d.phys.constraint;

import jcog.math.v3;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.solve.JacobianEntry;
import spacegraph.util.math.Matrix3f;



/**
 * UNTESTED
 * @author jezek2
 */
public class SliderConstraint extends TypedConstraint {

	private static final float SLIDER_CONSTRAINT_DEF_SOFTNESS    = 1.0f;
	private static final float SLIDER_CONSTRAINT_DEF_DAMPING     = 1.0f;
	private static final float SLIDER_CONSTRAINT_DEF_RESTITUTION = 0.7f;

	private final Transform frameInA = new Transform();
	private final Transform frameInB = new Transform();
	
	private final boolean useLinearReferenceFrameA;
	
	private float lowerLinLimit;
	private float upperLinLimit;
	
	private float lowerAngLimit;
	private float upperAngLimit;
	
	
	
	
	
	
	private float softnessDirLin;
	private float restitutionDirLin;
	private float dampingDirLin;
	private float softnessDirAng;
	private float restitutionDirAng;
	private float dampingDirAng;
	private float softnessLimLin;
	private float restitutionLimLin;
	private float dampingLimLin;
	private float softnessLimAng;
	private float restitutionLimAng;
	private float dampingLimAng;
	private float softnessOrthoLin;
	private float restitutionOrthoLin;
	private float dampingOrthoLin;
	private float softnessOrthoAng;
	private float restitutionOrthoAng;
	private float dampingOrthoAng;

	
	private boolean solveLinLim;
	private boolean solveAngLim;

	private final JacobianEntry[] jacLin = { new JacobianEntry(), new JacobianEntry(), new JacobianEntry() };
	private final float[] jacLinDiagABInv = new float[3];

	private final JacobianEntry[] jacAng = { new JacobianEntry(), new JacobianEntry(), new JacobianEntry() };

	private float timeStep;
	private final Transform calculatedTransformA = new Transform();
	private final Transform calculatedTransformB = new Transform();

	private final v3 sliderAxis = new v3();
	private final v3 realPivotAInW = new v3();
	private final v3 realPivotBInW = new v3();
	private final v3 projPivotInW = new v3();
	private final v3 delta = new v3();
	private final v3 depth = new v3();
	private final v3 relPosA = new v3();
	private final v3 relPosB = new v3();

	private float linPos;

	private float angDepth;
	private float kAngle;

	private boolean poweredLinMotor;
	private float targetLinMotorVelocity;
	private float maxLinMotorForce;
	private float accumulatedLinMotorImpulse;

	private boolean poweredAngMotor;
	private float targetAngMotorVelocity;
	private float maxAngMotorForce;
	private float accumulatedAngMotorImpulse;

    public SliderConstraint() {
		super(TypedConstraintType.SLIDER_CONSTRAINT_TYPE);
		useLinearReferenceFrameA = true;
		initParams();
	}

    public SliderConstraint(Body3D rbA, Body3D rbB, Transform frameInA, Transform frameInB , boolean useLinearReferenceFrameA) {
		super(TypedConstraintType.SLIDER_CONSTRAINT_TYPE, rbA, rbB);
        this.frameInA.set(frameInA);
        this.frameInB.set(frameInB);
		this.useLinearReferenceFrameA = useLinearReferenceFrameA;
		initParams();
	}

	private void initParams() {
		lowerLinLimit = 1f;
		upperLinLimit = -1f;
		lowerAngLimit = 0f;
		upperAngLimit = 0f;
		softnessDirLin = SLIDER_CONSTRAINT_DEF_SOFTNESS;
		restitutionDirLin = SLIDER_CONSTRAINT_DEF_RESTITUTION;
		dampingDirLin = 0f;
		softnessDirAng = SLIDER_CONSTRAINT_DEF_SOFTNESS;
		restitutionDirAng = SLIDER_CONSTRAINT_DEF_RESTITUTION;
		dampingDirAng = 0f;
		softnessOrthoLin = SLIDER_CONSTRAINT_DEF_SOFTNESS;
		restitutionOrthoLin = SLIDER_CONSTRAINT_DEF_RESTITUTION;
		dampingOrthoLin = SLIDER_CONSTRAINT_DEF_DAMPING;
		softnessOrthoAng = SLIDER_CONSTRAINT_DEF_SOFTNESS;
		restitutionOrthoAng = SLIDER_CONSTRAINT_DEF_RESTITUTION;
		dampingOrthoAng = SLIDER_CONSTRAINT_DEF_DAMPING;
		softnessLimLin = SLIDER_CONSTRAINT_DEF_SOFTNESS;
		restitutionLimLin = SLIDER_CONSTRAINT_DEF_RESTITUTION;
		dampingLimLin = SLIDER_CONSTRAINT_DEF_DAMPING;
		softnessLimAng = SLIDER_CONSTRAINT_DEF_SOFTNESS;
		restitutionLimAng = SLIDER_CONSTRAINT_DEF_RESTITUTION;
		dampingLimAng = SLIDER_CONSTRAINT_DEF_DAMPING;

		poweredLinMotor = false;
		targetLinMotorVelocity = 0f;
		maxLinMotorForce = 0f;
		accumulatedLinMotorImpulse = 0f;

		poweredAngMotor = false;
		targetAngMotorVelocity = 0f;
		maxAngMotorForce = 0f;
		accumulatedAngMotorImpulse = 0f;
	}

	@Override
	public void buildJacobian() {
		if (useLinearReferenceFrameA) {
			buildJacobianInt(rbA, rbB, frameInA, frameInB);
		}
		else {
			buildJacobianInt(rbB, rbA, frameInB, frameInA);
		}
	}

	@Override
	public void solveConstraint(float timeStep) {
		this.timeStep = timeStep;
		if (useLinearReferenceFrameA) {
			solveConstraintInt(rbA, rbB);
		}
		else {
			solveConstraintInt(rbB, rbA);
		}
	}
	
	public Transform getCalculatedTransformA(Transform out) {
		out.set(calculatedTransformA);
		return out;
	}
	
	public Transform getCalculatedTransformB(Transform out) {
		out.set(calculatedTransformB);
		return out;
	}
	
	public Transform getFrameOffsetA(Transform out) {
		out.set(frameInA);
		return out;
	}

	public Transform getFrameOffsetB(Transform out) {
		out.set(frameInB);
		return out;
	}
	
	public float getLowerLinLimit() {
		return lowerLinLimit;
	}

	public void setLowerLinLimit(float lowerLimit) {
		this.lowerLinLimit = lowerLimit;
	}

	public float getUpperLinLimit() {
		return upperLinLimit;
	}

	public void setUpperLinLimit(float upperLimit) {
		this.upperLinLimit = upperLimit;
	}

	public float getLowerAngLimit() {
		return lowerAngLimit;
	}

	public void setLowerAngLimit(float lowerLimit) {
		this.lowerAngLimit = lowerLimit;
	}

	public float getUpperAngLimit() {
		return upperAngLimit;
	}

	public void setUpperAngLimit(float upperLimit) {
		this.upperAngLimit = upperLimit;
	}

	public boolean getUseLinearReferenceFrameA() {
		return useLinearReferenceFrameA;
	}
	
	public float getSoftnessDirLin() {
		return softnessDirLin;
	}

	public float getRestitutionDirLin() {
		return restitutionDirLin;
	}

	public float getDampingDirLin() {
		return dampingDirLin;
	}

	public float getSoftnessDirAng() {
		return softnessDirAng;
	}

	public float getRestitutionDirAng() {
		return restitutionDirAng;
	}

	public float getDampingDirAng() {
		return dampingDirAng;
	}

	public float getSoftnessLimLin() {
		return softnessLimLin;
	}

	public float getRestitutionLimLin() {
		return restitutionLimLin;
	}

	public float getDampingLimLin() {
		return dampingLimLin;
	}

	public float getSoftnessLimAng() {
		return softnessLimAng;
	}

	public float getRestitutionLimAng() {
		return restitutionLimAng;
	}

	public float getDampingLimAng() {
		return dampingLimAng;
	}

	public float getSoftnessOrthoLin() {
		return softnessOrthoLin;
	}

	public float getRestitutionOrthoLin() {
		return restitutionOrthoLin;
	}

	public float getDampingOrthoLin() {
		return dampingOrthoLin;
	}

	public float getSoftnessOrthoAng() {
		return softnessOrthoAng;
	}

	public float getRestitutionOrthoAng() {
		return restitutionOrthoAng;
	}

	public float getDampingOrthoAng() {
		return dampingOrthoAng;
	}
	
	public void setSoftnessDirLin(float softnessDirLin) {
		this.softnessDirLin = softnessDirLin;
	}

	public void setRestitutionDirLin(float restitutionDirLin) {
		this.restitutionDirLin = restitutionDirLin;
	}

	public void setDampingDirLin(float dampingDirLin) {
		this.dampingDirLin = dampingDirLin;
	}

	public void setSoftnessDirAng(float softnessDirAng) {
		this.softnessDirAng = softnessDirAng;
	}

	public void setRestitutionDirAng(float restitutionDirAng) {
		this.restitutionDirAng = restitutionDirAng;
	}

	public void setDampingDirAng(float dampingDirAng) {
		this.dampingDirAng = dampingDirAng;
	}

	public void setSoftnessLimLin(float softnessLimLin) {
		this.softnessLimLin = softnessLimLin;
	}

	public void setRestitutionLimLin(float restitutionLimLin) {
		this.restitutionLimLin = restitutionLimLin;
	}

	public void setDampingLimLin(float dampingLimLin) {
		this.dampingLimLin = dampingLimLin;
	}

	public void setSoftnessLimAng(float softnessLimAng) {
		this.softnessLimAng = softnessLimAng;
	}

	public void setRestitutionLimAng(float restitutionLimAng) {
		this.restitutionLimAng = restitutionLimAng;
	}

	public void setDampingLimAng(float dampingLimAng) {
		this.dampingLimAng = dampingLimAng;
	}

	public void setSoftnessOrthoLin(float softnessOrthoLin) {
		this.softnessOrthoLin = softnessOrthoLin;
	}

	public void setRestitutionOrthoLin(float restitutionOrthoLin) {
		this.restitutionOrthoLin = restitutionOrthoLin;
	}

	public void setDampingOrthoLin(float dampingOrthoLin) {
		this.dampingOrthoLin = dampingOrthoLin;
	}

	public void setSoftnessOrthoAng(float softnessOrthoAng) {
		this.softnessOrthoAng = softnessOrthoAng;
	}

	public void setRestitutionOrthoAng(float restitutionOrthoAng) {
		this.restitutionOrthoAng = restitutionOrthoAng;
	}

	public void setDampingOrthoAng(float dampingOrthoAng) {
		this.dampingOrthoAng = dampingOrthoAng;
	}

	public void setPoweredLinMotor(boolean onOff) {
		this.poweredLinMotor = onOff;
	}

	public boolean getPoweredLinMotor() {
		return poweredLinMotor;
	}

	public void setTargetLinMotorVelocity(float targetLinMotorVelocity) {
		this.targetLinMotorVelocity = targetLinMotorVelocity;
	}

	public float getTargetLinMotorVelocity() {
		return targetLinMotorVelocity;
	}

	public void setMaxLinMotorForce(float maxLinMotorForce) {
		this.maxLinMotorForce = maxLinMotorForce;
	}

	public float getMaxLinMotorForce() {
		return maxLinMotorForce;
	}

	public void setPoweredAngMotor(boolean onOff) {
		this.poweredAngMotor = onOff;
	}

	public boolean getPoweredAngMotor() {
		return poweredAngMotor;
	}

	public void setTargetAngMotorVelocity(float targetAngMotorVelocity) {
		this.targetAngMotorVelocity = targetAngMotorVelocity;
	}

	public float getTargetAngMotorVelocity() {
		return targetAngMotorVelocity;
	}

	public void setMaxAngMotorForce(float maxAngMotorForce) {
		this.maxAngMotorForce = maxAngMotorForce;
	}

	public float getMaxAngMotorForce() {
		return this.maxAngMotorForce;
	}

	public float getLinearPos() {
		return this.linPos;
	}

	

	public boolean getSolveLinLimit() {
		return solveLinLim;
	}

	public float getLinDepth() {
		return depth.x;
	}

	public boolean getSolveAngLimit() {
		return solveAngLim;
	}

	public float getAngDepth() {
		return angDepth;
	}
	
	
	
	private void buildJacobianInt(Body3D rbA, Body3D rbB, Transform frameInA, Transform frameInB) {
		Transform tmpTrans = new Transform();
		Transform tmpTrans1 = new Transform();
		Transform tmpTrans2 = new Transform();
		v3 tmp = new v3();
		v3 tmp2 = new v3();

		
		calculatedTransformA.mul(rbA.getCenterOfMassTransform(tmpTrans), frameInA);
		calculatedTransformB.mul(rbB.getCenterOfMassTransform(tmpTrans), frameInB);
		realPivotAInW.set(calculatedTransformA);
		realPivotBInW.set(calculatedTransformB);
		calculatedTransformA.basis.getColumn(0, tmp);
		sliderAxis.set(tmp); 
		delta.sub(realPivotBInW, realPivotAInW);
		projPivotInW.scaleAdd(sliderAxis.dot(delta), sliderAxis, realPivotAInW);
		relPosA.sub(projPivotInW, rbA.getCenterOfMassPosition(tmp));
		relPosB.sub(realPivotBInW, rbB.getCenterOfMassPosition(tmp));
		v3 normalWorld = new v3();

		
		for (int i=0; i<3; i++) {
			calculatedTransformA.basis.getColumn(i, normalWorld);

			Matrix3f mat1 = rbA.getCenterOfMassTransform(tmpTrans1).basis;
			mat1.transpose();

			Matrix3f mat2 = rbB.getCenterOfMassTransform(tmpTrans2).basis;
			mat2.transpose();

			jacLin[i].init(
					mat1,
					mat2,
					relPosA,
					relPosB,
					normalWorld,
					rbA.getInvInertiaDiagLocal(tmp),
					rbA.getInvMass(),
					rbB.getInvInertiaDiagLocal(tmp2),
					rbB.getInvMass());
            jacLinDiagABInv[i] = 1f / jacLin[i].Adiag;
			VectorUtil.setCoord(depth, i, delta.dot(normalWorld));
		}
		testLinLimits();

		
		for (int i=0; i<3; i++) {
			calculatedTransformA.basis.getColumn(i, normalWorld);

			Matrix3f mat1 = rbA.getCenterOfMassTransform(tmpTrans1).basis;
			mat1.transpose();

			Matrix3f mat2 = rbB.getCenterOfMassTransform(tmpTrans2).basis;
			mat2.transpose();

			jacAng[i].init(
					normalWorld,
					mat1,
					mat2,
					rbA.getInvInertiaDiagLocal(tmp),
					rbB.getInvInertiaDiagLocal(tmp2));
		}
		testAngLimits();

		v3 axisA = new v3();
		calculatedTransformA.basis.getColumn(0, axisA);
		kAngle = 1f / (rbA.computeAngularImpulseDenominator(axisA) + rbB.computeAngularImpulseDenominator(axisA));
		
		accumulatedLinMotorImpulse = 0f;
		accumulatedAngMotorImpulse = 0f;
	}
	
	private void solveConstraintInt(Body3D rbA, Body3D rbB) {
		v3 tmp = new v3();

		
		v3 velA = rbA.getVelocityInLocalPoint(relPosA, new v3());
		v3 velB = rbB.getVelocityInLocalPoint(relPosB, new v3());
		v3 vel = new v3();
		vel.sub(velA, velB);

		v3 impulse_vector = new v3();

		for (int i=0; i<3; i++) {
			v3 normal = jacLin[i].linearJointAxis;
			float rel_vel = normal.dot(vel);
			
			float depth = VectorUtil.coord(this.depth, i);
			
			float softness = (i != 0)? softnessOrthoLin : (solveLinLim? softnessLimLin : softnessDirLin);
			float restitution = (i != 0)? restitutionOrthoLin : (solveLinLim? restitutionLimLin : restitutionDirLin);
			float damping = (i != 0)? dampingOrthoLin : (solveLinLim? dampingLimLin : dampingDirLin);
			
			float normalImpulse = softness * (restitution * depth / timeStep - damping * rel_vel) * jacLinDiagABInv[i];
			impulse_vector.scale(normalImpulse, normal);
			rbA.impulse(impulse_vector, relPosA);
			tmp.negated(impulse_vector);
			rbB.impulse(tmp, relPosB);

			if (poweredLinMotor && (i == 0)) {
				
				if (accumulatedLinMotorImpulse < maxLinMotorForce) {
					float desiredMotorVel = targetLinMotorVelocity;
					float motor_relvel = desiredMotorVel + rel_vel;
					normalImpulse = -motor_relvel * jacLinDiagABInv[i];
					
					float new_acc = accumulatedLinMotorImpulse + Math.abs(normalImpulse);
					if (new_acc > maxLinMotorForce) {
						new_acc = maxLinMotorForce;
					}
					float del = new_acc - accumulatedLinMotorImpulse;
					normalImpulse = normalImpulse < 0f ? -del : del;
					accumulatedLinMotorImpulse = new_acc;
					
					impulse_vector.scale(normalImpulse, normal);
					rbA.impulse(impulse_vector, relPosA);
					tmp.negated(impulse_vector);
					rbB.impulse(tmp, relPosB);
				}
			}
		}

		
		
		v3 axisA = new v3();
		calculatedTransformA.basis.getColumn(0, axisA);
		v3 axisB = new v3();
		calculatedTransformB.basis.getColumn(0, axisB);

		v3 angVelA = rbA.getAngularVelocity(new v3());
		v3 angVelB = rbB.getAngularVelocity(new v3());

		v3 angVelAroundAxisA = new v3();
		angVelAroundAxisA.scale(axisA.dot(angVelA), axisA);
		v3 angVelAroundAxisB = new v3();
		angVelAroundAxisB.scale(axisB.dot(angVelB), axisB);

		v3 angAorthog = new v3();
		angAorthog.sub(angVelA, angVelAroundAxisA);
		v3 angBorthog = new v3();
		angBorthog.sub(angVelB, angVelAroundAxisB);
		v3 velrelOrthog = new v3();
		velrelOrthog.sub(angAorthog, angBorthog);

		
		float len = velrelOrthog.length();
		if (len > 0.00001f) {
			v3 normal = new v3();
			normal.normalize(velrelOrthog);
			float denom = rbA.computeAngularImpulseDenominator(normal) + rbB.computeAngularImpulseDenominator(normal);
			velrelOrthog.scaled((1f / denom) * dampingOrthoAng * softnessOrthoAng);
		}

		
		v3 angularError = new v3();
		angularError.cross(axisA, axisB);
		angularError.scaled(1f / timeStep);
		float len2 = angularError.length();
		if (len2 > 0.00001f) {
			v3 normal2 = new v3();
			normal2.normalize(angularError);
			float denom2 = rbA.computeAngularImpulseDenominator(normal2) + rbB.computeAngularImpulseDenominator(normal2);
			angularError.scaled((1f / denom2) * restitutionOrthoAng * softnessOrthoAng);
		}

		
		tmp.negated(velrelOrthog);
		tmp.add(angularError);
		rbA.torqueImpulse(tmp);
		tmp.sub(velrelOrthog, angularError);
		rbB.torqueImpulse(tmp);
		float impulseMag;

		
		if (solveAngLim) {
			tmp.sub(angVelB, angVelA);
			impulseMag = tmp.dot(axisA) * dampingLimAng + angDepth * restitutionLimAng / timeStep;
			impulseMag *= kAngle * softnessLimAng;
		}
		else {
			tmp.sub(angVelB, angVelA);
			impulseMag = tmp.dot(axisA) * dampingDirAng + angDepth * restitutionDirAng / timeStep;
			impulseMag *= kAngle * softnessDirAng;
		}
		v3 impulse = new v3();
		impulse.scale(impulseMag, axisA);
		rbA.torqueImpulse(impulse);
		tmp.negated(impulse);
		rbB.torqueImpulse(tmp);

		
		if (poweredAngMotor) {
			if (accumulatedAngMotorImpulse < maxAngMotorForce) {
				v3 velrel = new v3();
				velrel.sub(angVelAroundAxisA, angVelAroundAxisB);
				float projRelVel = velrel.dot(axisA);

				float desiredMotorVel = targetAngMotorVelocity;
				float motor_relvel = desiredMotorVel - projRelVel;

				float angImpulse = kAngle * motor_relvel;
				
				float new_acc = accumulatedAngMotorImpulse + Math.abs(angImpulse);
				if (new_acc > maxAngMotorForce) {
					new_acc = maxAngMotorForce;
				}
				float del = new_acc - accumulatedAngMotorImpulse;
				angImpulse = angImpulse < 0f ? -del : del;
				accumulatedAngMotorImpulse = new_acc;

				
				v3 motorImp = new v3();
				motorImp.scale(angImpulse, axisA);
				rbA.torqueImpulse(motorImp);
				tmp.negated(motorImp);
				rbB.torqueImpulse(tmp);
			}
		}
	}
	
	
	
	public void calculateTransforms() {
		Transform tmpTrans = new Transform();

		if (useLinearReferenceFrameA) {
			calculatedTransformA.mul(rbA.getCenterOfMassTransform(tmpTrans), frameInA);
			calculatedTransformB.mul(rbB.getCenterOfMassTransform(tmpTrans), frameInB);
		}
		else {
			calculatedTransformA.mul(rbB.getCenterOfMassTransform(tmpTrans), frameInB);
			calculatedTransformB.mul(rbA.getCenterOfMassTransform(tmpTrans), frameInA);
		}
		realPivotAInW.set(calculatedTransformA);
		realPivotBInW.set(calculatedTransformB);
		calculatedTransformA.basis.getColumn(0, sliderAxis); 
		delta.sub(realPivotBInW, realPivotAInW);
		projPivotInW.scaleAdd(sliderAxis.dot(delta), sliderAxis, realPivotAInW);
		v3 normalWorld = new v3();
		
		for (int i=0; i<3; i++) {
			calculatedTransformA.basis.getColumn(i, normalWorld);
			VectorUtil.setCoord(depth, i, delta.dot(normalWorld));
		}
	}

	private void testLinLimits() {
		solveLinLim = false;
		linPos = depth.x;
		if (lowerLinLimit <= upperLinLimit) {
			if (depth.x > upperLinLimit) {
				depth.x -= upperLinLimit;
				solveLinLim = true;
			}
			else if (depth.x < lowerLinLimit) {
				depth.x -= lowerLinLimit;
				solveLinLim = true;
			}
			else {
				depth.x = 0f;
			}
		}
		else {
			depth.x = 0f;
		}
	}
	
	private void testAngLimits() {
		angDepth = 0f;
		solveAngLim = false;
		if (lowerAngLimit <= upperAngLimit) {
			v3 axisA0 = new v3();
			calculatedTransformA.basis.getColumn(1, axisA0);
			v3 axisA1 = new v3();
			calculatedTransformA.basis.getColumn(2, axisA1);
			v3 axisB0 = new v3();
			calculatedTransformB.basis.getColumn(1, axisB0);

			float rot = (float) Math.atan2(axisB0.dot(axisA1), axisB0.dot(axisA0));
			if (rot < lowerAngLimit) {
				angDepth = rot - lowerAngLimit;
				solveAngLim = true;
			}
			else if (rot > upperAngLimit) {
				angDepth = rot - upperAngLimit;
				solveAngLim = true;
			}
		}
	}
	
	
	
	public v3 getAncorInA(v3 out) {
		Transform tmpTrans = new Transform();

		v3 ancorInA = out;
		ancorInA.scaleAdd((lowerLinLimit + upperLinLimit) * 0.5f, sliderAxis, realPivotAInW);
		rbA.getCenterOfMassTransform(tmpTrans);
		tmpTrans.invert();
		tmpTrans.transform(ancorInA);
		return ancorInA;
	}

	public v3 getAncorInB(v3 out) {
		v3 ancorInB = out;
		ancorInB.set(frameInB);
		return ancorInB;
	}

}
