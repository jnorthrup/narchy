/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
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

package spacegraph.phys.math;

import spacegraph.math.Matrix3f;
import spacegraph.math.Quat4f;
import spacegraph.math.v3;
import spacegraph.phys.BulletGlobals;

/**
 * Utility functions for transforms.
 * 
 * @author jezek2
 */
public class TransformUtil {
	
	public static final float SIMDSQRT12 = 0.7071067811865475244008443621048490f;
	public static final float ANGULAR_MOTION_THRESHOLD = 0.5f* BulletGlobals.SIMD_HALF_PI;
	
	public static float recipSqrt(float x) {
		return (float)(1.0 / Math.sqrt(x));  /* reciprocal square root */
	}

	public static void planeSpace1(v3 n, v3 p, v3 q) {
		float ny = n.y;
		float nz = n.z;
		float nx = n.x;
		if (Math.abs(nz) > SIMDSQRT12) {
			// choose p in y-z plane
			float a = ny * ny + nz * nz;
			float k = recipSqrt(a);
			p.set(0, -nz * k, ny * k);
			// set q = n x p
			q.set(a * k, -nx * p.z, nx * p.y);
		}
		else {
			// choose p in x-y plane
			float a = nx * nx + ny * ny;
			float k = recipSqrt(a);
			p.set(-ny * k, nx * k, 0);
			// set q = n x p
			q.set(-nz * p.y, nz * p.x, a * k);
		}
	}
	

	public static void integrateTransform(Transform curTrans, v3 linvel, v3 angvel, float timeStep, Transform predictedTransform) {
		predictedTransform.scaleAdd(timeStep, linvel, curTrans);
//	//#define QUATERNION_DERIVATIVE
//	#ifdef QUATERNION_DERIVATIVE
//		btQuaternion predictedOrn = curTrans.getRotation();
//		predictedOrn += (angvel * predictedOrn) * (timeStep * btScalar(0.5));
//		predictedOrn.normalize();
//	#else
		// Exponential map
		// google for "Practical Parameterization of Rotations Using the Exponential Map", F. Sebastian Grassia

		v3 axis = new v3();
		float fAngle = angvel.length();

		// limit the angular motion
		if (fAngle * timeStep > ANGULAR_MOTION_THRESHOLD) {
			fAngle = ANGULAR_MOTION_THRESHOLD / timeStep;
		}

		if (fAngle < 0.001f) {
			// use Taylor's expansions of sync function
			axis.scale(0.5f * timeStep - (timeStep * timeStep * timeStep) * (0.020833333333f) * fAngle * fAngle, angvel);
		}
		else {
			// sync(fAngle) = sin(c*fAngle)/t
			axis.scale((float) Math.sin(0.5 * fAngle * timeStep) / fAngle, angvel);
		}
		Quat4f dorn = new Quat4f(axis.x, axis.y, axis.z, (float) Math.cos(0.5 * fAngle * timeStep));
		Quat4f orn0 = curTrans.getRotation(new Quat4f());

		Quat4f predictedOrn = new Quat4f();
		predictedOrn.mul(dorn, orn0);
		predictedOrn.normalize();
//  #endif
		predictedTransform.setRotation(predictedOrn);
	}

	public static void calculateVelocity(Transform transform0, Transform transform1, float timeStep, v3 linVel, v3 angVel) {
		linVel.sub(transform1, transform0);
		linVel.scale(1f / timeStep);

		v3 axis = new v3();
		float[] angle = new float[1];
		calculateDiffAxisAngle(transform0, transform1, axis, angle);
		angVel.scale(angle[0] / timeStep, axis);
	}

	public static void calculateDiffAxisAngle(Transform transform0, Transform transform1, v3 axis, float[] angle) {
// #ifdef USE_QUATERNION_DIFF
//		btQuaternion orn0 = transform0.getRotation();
//		btQuaternion orn1a = transform1.getRotation();
//		btQuaternion orn1 = orn0.farthest(orn1a);
//		btQuaternion dorn = orn1 * orn0.inverse();
// #else
		Matrix3f tmp = new Matrix3f();
		tmp.set(transform0.basis);
		MatrixUtil.invert(tmp);

		Matrix3f dmat = new Matrix3f();
		dmat.mul(transform1.basis, tmp);

		Quat4f dorn = new Quat4f();
		MatrixUtil.getRotation(dmat, dorn);
// #endif

		// floating point inaccuracy can lead to w component > 1..., which breaks 

		dorn.normalize();

		angle[0] = QuaternionUtil.getAngle(dorn);
		axis.set(dorn.x, dorn.y, dorn.z);
		// TODO: probably not needed
		//axis[3] = btScalar(0.);

		// check for axis length
		float lenSq = axis.lengthSquared();
		if (lenSq < BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON) {
			axis.set(1f, 0f, 0f);
		} else {
			axis.scale(1f / (float) Math.sqrt(lenSq));
		}
	}
	
}
