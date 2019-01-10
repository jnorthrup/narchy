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

import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.util.math.v3;

/**
 * Rotation limit structure for generic joints.
 * 
 * @author jezek2
 */
public class RotationalLimitMotor {
	
	

	public float loLimit; 
	public float hiLimit; 
	private float targetVelocity;
	private float maxMotorForce;
	private float maxLimitForce;
	private float damping;
	private float limitSoftness;
	private float ERP;
	private float bounce;
	private boolean enableMotor;
	
	private float currentLimitError;
	private int currentLimit;
	public float accumulatedImpulse;

	public RotationalLimitMotor() {
    	accumulatedImpulse = 0.f;
        targetVelocity = 0;
        maxMotorForce = 0.1f;
        maxLimitForce = 300.0f;
        loLimit = -BulletGlobals.SIMD_INFINITY;
        hiLimit = BulletGlobals.SIMD_INFINITY;
        ERP = 0.5f;
        bounce = 0.0f;
        damping = 1.0f;
        limitSoftness = 0.5f;
        currentLimit = 0;
        currentLimitError = 0;
        enableMotor = false;
	}
	
	public RotationalLimitMotor(RotationalLimitMotor limot) {
		targetVelocity = limot.targetVelocity;
		maxMotorForce = limot.maxMotorForce;
		limitSoftness = limot.limitSoftness;
		loLimit = limot.loLimit;
		hiLimit = limot.hiLimit;
		ERP = limot.ERP;
		bounce = limot.bounce;
		currentLimit = limot.currentLimit;
		currentLimitError = limot.currentLimitError;
		enableMotor = limot.enableMotor;
	}

	/**
	 * Is limited?
	 */
    public boolean isLimited()
    {
        return loLimit < hiLimit;
    }

	/**
	 * Need apply correction?
	 */
    public boolean needApplyTorques()
    {
        return !(currentLimit == 0 && !enableMotor);
    }

	/**
	 * Calculates error. Calculates currentLimit and currentLimitError.
	 */
	public int testLimitValue(float test_value) {
		if (loLimit > hiLimit) {
			currentLimit = 0; 
			return 0;
		}

		if (test_value < loLimit) {
			currentLimit = 1; 
			currentLimitError = test_value - loLimit;
			return 1;
		}
        if (test_value > hiLimit) {
            currentLimit = 2; 
            currentLimitError = test_value - hiLimit;
            return 2;
        }

        currentLimit = 0; 
		return 0;
	}

	/**
	 * Apply the correction impulses for two bodies.
	 */

	public float solveAngularLimits(float timeStep, v3 axis, float jacDiagABInv, Body3D body0, Body3D body1) {
		if (!needApplyTorques()) {
			return 0.0f;
		}

		float target_velocity = this.targetVelocity;
		float maxMotorForce = this.maxMotorForce;

		
		if (currentLimit != 0) {
			target_velocity = -ERP * currentLimitError / (timeStep);
			maxMotorForce = maxLimitForce;
		}

		maxMotorForce *= timeStep;

		
		v3 vel_diff = body0.getAngularVelocity(new v3());
		if (body1 != null) {
			vel_diff.sub(body1.getAngularVelocity(new v3()));
		}

		float rel_vel = axis.dot(vel_diff);

		
		float motor_relvel = limitSoftness * (target_velocity - damping * rel_vel);

		if (motor_relvel < BulletGlobals.FLT_EPSILON && motor_relvel > -BulletGlobals.FLT_EPSILON) {
			return 0.0f; 
		}

		
		float unclippedMotorImpulse = (1 + bounce) * motor_relvel * jacDiagABInv;

		
		float clippedMotorImpulse;

		
		if (unclippedMotorImpulse > 0.0f) {
			clippedMotorImpulse = unclippedMotorImpulse > maxMotorForce ? maxMotorForce : unclippedMotorImpulse;
		}
		else {
			clippedMotorImpulse = unclippedMotorImpulse < -maxMotorForce ? -maxMotorForce : unclippedMotorImpulse;
		}

		
		float lo = -1e30f;
		float hi = 1e30f;

		float oldaccumImpulse = accumulatedImpulse;
		float sum = oldaccumImpulse + clippedMotorImpulse;
		accumulatedImpulse = sum > hi ? 0f : sum < lo ? 0f : sum;

		clippedMotorImpulse = accumulatedImpulse - oldaccumImpulse;

		v3 motorImp = new v3();
		motorImp.scale(clippedMotorImpulse, axis);

		body0.torqueImpulse(motorImp);
		if (body1 != null) {
			motorImp.negated();
			body1.torqueImpulse(motorImp);
		}

		return clippedMotorImpulse;
	}
	
}
