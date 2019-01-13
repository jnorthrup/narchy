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
import spacegraph.util.math.Quat4f;

/**
 * Utility functions for quaternions.
 * 
 * @author jezek2
 */
public class QuaternionUtil {

	public static float getAngle(Quat4f q) {
		float s = 2f * (float) Math.acos(q.w);
		return s;
	}
	
	public static void setRotation(Quat4f q, v3 axis, float angle) {
		float d = axis.length();
		assert (d != 0f);
		float s = (float) Math.sin(angle * 0.5f) / d;
		q.set(axis.x * s, axis.y * s, axis.z * s, (float) Math.cos(angle * 0.5f));
	}
	
	
	public static Quat4f shortestArcQuat(v3 v0, v3 v1, Quat4f out) {
		v3 c = new v3();
		c.cross(v0, v1);
		float d = v0.dot(v1);

		if (d < -1.0 + BulletGlobals.FLT_EPSILON) {
			
			out.set(0.0f, 1.0f, 0.0f, 0.0f);
			return out;
		}

		float s = (float) Math.sqrt((1.0f + d) * 2.0f);
		float rs = 1.0f / s;

		out.set(c.x * rs, c.y * rs, c.z * rs, s * 0.5f);
		return out;
	}
	
	private static void mul(Quat4f q, v3 w) {
		float rx = q.w * w.x + q.y * w.z - q.z * w.y;
		float ry = q.w * w.y + q.z * w.x - q.x * w.z;
		float rz = q.w * w.z + q.x * w.y - q.y * w.x;
		float rw = -q.x * w.x - q.y * w.y - q.z * w.z;
		q.set(rx, ry, rz, rw);
	}
	
	public static v3 quatRotate(Quat4f rotation, v3 v, v3 out) {
		Quat4f q = new Quat4f(rotation);
		mul(q, v);

		Quat4f tmp = new Quat4f();
		inverse(tmp, rotation);
		q.mul(tmp);
		
		out.set(q.x, q.y, q.z);
		return out;
	}
	
	public static void inverse(Quat4f q) {
		q.x = -q.x;
		q.y = -q.y;
		q.z = -q.z;
	}
	
	private static void inverse(Quat4f q, Quat4f src) {
		q.x = -src.x;
		q.y = -src.y;
		q.z = -src.z;
		q.w = src.w;
	}

	public static void setEuler(Quat4f q, float yaw, float pitch, float roll) {
		float halfYaw = yaw * 0.5f;
		float halfPitch = pitch * 0.5f;
		float halfRoll = roll * 0.5f;
		float cosYaw = (float) Math.cos(halfYaw);
		float sinYaw = (float) Math.sin(halfYaw);
		float cosPitch = (float) Math.cos(halfPitch);
		float sinPitch = (float) Math.sin(halfPitch);
		float cosRoll = (float) Math.cos(halfRoll);
		float sinRoll = (float) Math.sin(halfRoll);
		q.x = cosRoll * sinPitch * cosYaw + sinRoll * cosPitch * sinYaw;
		q.y = cosRoll * cosPitch * sinYaw - sinRoll * sinPitch * cosYaw;
		q.z = sinRoll * cosPitch * cosYaw - cosRoll * sinPitch * sinYaw;
		q.w = cosRoll * cosPitch * cosYaw + sinRoll * sinPitch * sinYaw;
	}

}
