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
2007-09-09
btGeneric6DofConstraint Refactored by Francisco Le�n
email: projectileman@yahoo.com
http:
*/

package spacegraph.space3d.phys.constraint.generic;

import jcog.math.v3;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.math.VectorUtil;

/**
 * @author jezek2
 */
public class TranslationalLimitMotor {


	public final v3 lowerLimit = new v3();
	public final v3 upperLimit = new v3();
	public final v3 accumulatedImpulse = new v3();

	private final float limitSoftness;
	private final float damping;
	private final float restitution;


	private final boolean[] enableMotor = new boolean[3];
	private final v3 targetVelocity = new v3();
	private final v3 maxMotorForce = new v3();
	private final v3 maxLimitForce = new v3();
	private final v3 currentLimitError = new v3();
	public final v3 currentLinearDiff = new v3();
	private final int[] currentLimit = new int[3];


	public TranslationalLimitMotor() {
		lowerLimit.set(0f, 0f, 0f);
		upperLimit.set(0f, 0f, 0f);
		accumulatedImpulse.set(0f, 0f, 0f);

		limitSoftness = 0.7f;
		damping = 1.0f;
		restitution = 0.5f;

		targetVelocity.set(0f, 0f, 0f);
		maxMotorForce.set(0.1f, 0.1f, 0.1f);
		maxLimitForce.set(300.0f, 300.0f, 300.0f);

	}


	public TranslationalLimitMotor(TranslationalLimitMotor other) {
		lowerLimit.set(other.lowerLimit);
		upperLimit.set(other.upperLimit);
		accumulatedImpulse.set(other.accumulatedImpulse);

		limitSoftness = other.limitSoftness;
		damping = other.damping;
		restitution = other.restitution;
	}

	/**
	 * Test limit.<p>
	 * - free means upper &lt; lower,<br>
	 * - locked means upper == lower<br>
	 * - limited means upper &gt; lower<br>
	 * - limitIndex: first 3 are linear, next 3 are angular
	 */
	public boolean isLimited(int limitIndex) {
		return (VectorUtil.coord(upperLimit, limitIndex) >= VectorUtil.coord(lowerLimit, limitIndex));
	}

	/**
	 * Need apply correction?
	 */
	public boolean needApplyForces(int idx) {
		return !(currentLimit[idx] == 0 && !enableMotor[idx]);
	}

	public int testLimitValue(int limitIndex, float test_value) {
		float loLimit = VectorUtil.coord(lowerLimit, limitIndex);
		float hiLimit = VectorUtil.coord(upperLimit, limitIndex);
		if (loLimit > hiLimit) {
			currentLimit[limitIndex] = 0;
			VectorUtil.setCoord(currentLimitError, limitIndex, 0.f);
			return 0;
		}

		if (test_value < loLimit) {
			currentLimit[limitIndex] = 2;
			VectorUtil.setCoord(currentLimitError, limitIndex, test_value - loLimit);
			return 2;
		}
		if (test_value > hiLimit) {
			currentLimit[limitIndex] = 1;
			VectorUtil.setCoord(currentLimitError, limitIndex, test_value - hiLimit);
			return 1;
		}

		currentLimit[limitIndex] = 0;
		VectorUtil.setCoord(currentLimitError, limitIndex, 0.f);
		return 0;
	}


	public float solveLinearAxis(float timeStep, float jacDiagABInv, Body3D body1, v3 pointInA, Body3D body2, v3 pointInB, int limit_index, v3 axis_normal_on_a, v3 anchorPos) {
		v3 tmp = new v3();
		v3 tmpVec = new v3();


		v3 rel_pos1 = new v3();

		rel_pos1.sub(anchorPos, body1.getCenterOfMassPosition(tmpVec));

		v3 rel_pos2 = new v3();

		rel_pos2.sub(anchorPos, body2.getCenterOfMassPosition(tmpVec));

		v3 vel1 = body1.getVelocityInLocalPoint(rel_pos1, new v3());
		v3 vel2 = body2.getVelocityInLocalPoint(rel_pos2, new v3());
		v3 vel = new v3();
		vel.sub(vel1, vel2);

		float rel_vel = axis_normal_on_a.dot(vel);


		float target_velocity = VectorUtil.coord(this.targetVelocity, limit_index);
		float maxMotorForce = VectorUtil.coord(this.maxMotorForce, limit_index);

		float limErr = VectorUtil.coord(currentLimitError, limit_index);
		if (currentLimit[limit_index] != 0) {
			target_velocity = restitution * limErr / (timeStep);
			maxMotorForce = VectorUtil.coord(maxLimitForce, limit_index);
		}
		maxMotorForce *= timeStep;


		float motor_relvel = limitSoftness * (target_velocity - damping * rel_vel);
		if (motor_relvel < BulletGlobals.FLT_EPSILON && motor_relvel > -BulletGlobals.FLT_EPSILON) {
			return 0.0f;
		}


		float unclippedMotorImpulse = motor_relvel * jacDiagABInv;


		float clippedMotorImpulse;


		if (unclippedMotorImpulse > 0.0f) {
			clippedMotorImpulse = unclippedMotorImpulse > maxMotorForce ? maxMotorForce : unclippedMotorImpulse;
		} else {
			clippedMotorImpulse = unclippedMotorImpulse < -maxMotorForce ? -maxMotorForce : unclippedMotorImpulse;
		}

		float normalImpulse = clippedMotorImpulse;


		float lo = -1e30f;
		float hi = 1e30f;


		float oldNormalImpulse = VectorUtil.coord(accumulatedImpulse, limit_index);
		float sum = oldNormalImpulse + normalImpulse;
		VectorUtil.setCoord(accumulatedImpulse, limit_index, sum > hi ? 0f : sum < lo ? 0f : sum);
		normalImpulse = VectorUtil.coord(accumulatedImpulse, limit_index) - oldNormalImpulse;

		v3 impulse_vector = new v3();
		impulse_vector.scale(normalImpulse, axis_normal_on_a);
		body1.impulse(impulse_vector, rel_pos1);

		tmp.negated(impulse_vector);
		body2.impulse(tmp, rel_pos2);
		return normalImpulse;
	}

}
