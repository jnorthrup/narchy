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

package spacegraph.space3d.phys.math;

import jcog.math.v3;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.util.math.Matrix3f;
import spacegraph.util.math.Quat4f;

/**
 * Utility functions for transforms.
 * 
 * @author jezek2
 */
public enum TransformUtil {
	;

	private static final float SIMDSQRT12 = 0.7071067811865475244008443621048490f;
	private static final float ANGULAR_MOTION_THRESHOLD = 0.5f* BulletGlobals.SIMD_HALF_PI;
	
	private static float recipSqrt(float x) {
		return (float)(1.0 / Math.sqrt(x));  /* reciprocal square root */
	}

	public static void planeSpace1(v3 n, v3 p, v3 q) {
		float ny = n.y;
		float nz = n.z;
		float nx = n.x;
		if (Math.abs(nz) > SIMDSQRT12) {
			
			float a = ny * ny + nz * nz;
			float k = recipSqrt(a);
			p.set(0, -nz * k, ny * k);
			
			q.set(a * k, -nx * p.z, nx * p.y);
		}
		else {
			
			float a = nx * nx + ny * ny;
			float k = recipSqrt(a);
			p.set(-ny * k, nx * k, 0);
			
			q.set(-nz * p.y, nz * p.x, a * k);
		}
	}
	

	public static void integrateTransform(Transform curTrans, v3 linvel, v3 angvel, float timeStep, Transform predictedTransform) {
		predictedTransform.scaleAdd(timeStep, linvel, curTrans);






		
		

		v3 axis = new v3();
		float fAngle = angvel.length();

		
		if (fAngle * timeStep > ANGULAR_MOTION_THRESHOLD) {
			fAngle = ANGULAR_MOTION_THRESHOLD / timeStep;
		}

		if (fAngle < 0.001f) {
			
			axis.scale(0.5f * timeStep - (timeStep * timeStep * timeStep) * (0.020833333333f) * fAngle * fAngle, angvel);
		}
		else {
			
			axis.scale((float) Math.sin(0.5 * fAngle * timeStep) / fAngle, angvel);
		}
		Quat4f dorn = new Quat4f(axis.x, axis.y, axis.z, (float) Math.cos(0.5 * fAngle * timeStep));
		Quat4f orn0 = curTrans.getRotation(new Quat4f());

		Quat4f predictedOrn = new Quat4f();
		predictedOrn.mul(dorn, orn0);
		predictedOrn.normalize();

		predictedTransform.setRotation(predictedOrn);
	}

	public static void calculateVelocity(Transform transform0, Transform transform1, float timeStep, v3 linVel, v3 angVel) {
		linVel.sub(transform1, transform0);
		linVel.scaled(1f / timeStep);

		v3 axis = new v3();
		float[] angle = new float[1];
		calculateDiffAxisAngle(transform0, transform1, axis, angle);
		angVel.scale(angle[0] / timeStep, axis);
	}

	private static void calculateDiffAxisAngle(Transform transform0, Transform transform1, v3 axis, float[] angle) {






		Matrix3f tmp = new Matrix3f();
		tmp.set(transform0.basis);
		MatrixUtil.invert(tmp);

		Matrix3f dmat = new Matrix3f();
		dmat.mul(transform1.basis, tmp);

		Quat4f dorn = new Quat4f();
		MatrixUtil.getRotation(dmat, dorn);


		

		dorn.normalize();

		angle[0] = QuaternionUtil.getAngle(dorn);
		axis.set(dorn.x, dorn.y, dorn.z);
		
		

		
		float lenSq = axis.lengthSquared();
		if (lenSq < BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON) {
			axis.set(1f, 0f, 0f);
		} else {
			axis.scaled(1f / (float) Math.sqrt(lenSq));
		}
	}
	
}
