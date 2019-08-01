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
import spacegraph.util.math.Vector4f;

/**
 * Utility functions for vectors.
 * 
 * @author jezek2
 */
public class VectorUtil {

	public static int maxAxis(v3 v) {
		int maxIndex = -1;
		float maxVal = -1e30f;
		if (v.x > maxVal) {
			maxIndex = 0;
			maxVal = v.x;
		}
		if (v.y > maxVal) {
			maxIndex = 1;
			maxVal = v.y;
		}
		if (v.z > maxVal) {
			maxIndex = 2;
			maxVal = v.z;
		}

		return maxIndex;
	}
	
	private static int maxAxis4(Vector4f v) {
		int maxIndex = -1;
		float maxVal = -1e30f;
		if (v.x > maxVal) {
			maxIndex = 0;
			maxVal = v.x;
		}
		if (v.y > maxVal) {
			maxIndex = 1;
			maxVal = v.y;
		}
		if (v.z > maxVal) {
			maxIndex = 2;
			maxVal = v.z;
		}
		if (v.w > maxVal) {
			maxIndex = 3;
			maxVal = v.w;
		}

		return maxIndex;
	}

	public static int closestAxis4(Vector4f vec) {
		Vector4f tmp = new Vector4f(vec);
		tmp.absolute();
		return maxAxis4(tmp);
	}
	
	public static float coord(v3 vec, int num) {
		switch (num) {
			case 0: return vec.x;
			case 1: return vec.y;
			case 2: return vec.z;
			default: throw new InternalError();
		}
	}
	
	public static void setCoord(v3 vec, int num, float value) {
		switch (num) {
			case 0: vec.x = value; break;
			case 1: vec.y = value; break;
			case 2: vec.z = value; break;
			default: throw new InternalError();
		}
	}

	public static void mulCoord(v3 vec, int num, float value) {
		switch (num) {
			case 0: vec.x *= value; break;
			case 1: vec.y *= value; break;
			case 2: vec.z *= value; break;
			default: throw new InternalError();
		}
	}

	public static void lerp(v3 ab, v3 a, v3 b, float x) {
		float y = 1f - x;
		ab.set(
		y * a.x + x * b.x,
		y * a.y + x * b.y,
		y * a.z + x * b.z);

		
		
	}

	public static void add(v3 dest, v3 v1, v3 v2) {
		dest.x = v1.x + v2.x;
		dest.y = v1.y + v2.y;
		dest.z = v1.z + v2.z;
	}

	public static void sub(v3 dest, v3 v1, v3 v2) {
		dest.x = v1.x - v2.x;
		dest.y = v1.y - v2.y;
		dest.z = v1.z - v2.z;
	}

	
	public static void add(v3 dest, v3 v1, v3 v2, v3 v3) {
		dest.x = v1.x + v2.x + v3.x;
		dest.y = v1.y + v2.y + v3.y;
		dest.z = v1.z + v2.z + v3.z;
	}
	
	public static void add(v3 dest, v3 v1, v3 v2, v3 v3, v3 v4) {
		dest.x = v1.x + v2.x + v3.x + v4.x;
		dest.y = v1.y + v2.y + v3.y + v4.y;
		dest.z = v1.z + v2.z + v3.z + v4.z;
	}
	
	public static void mul(v3 dest, v3 v1, v3 v2) {
		dest.x = v1.x * v2.x;
		dest.y = v1.y * v2.y;
		dest.z = v1.z * v2.z;
	}

	public static void mul(v3 dest, v3 v1, float s) {
		dest.x = v1.x * s;
		dest.y = v1.y * s;
		dest.z = v1.z * s;
	}

	
	public static void div(v3 dest, v3 v1, v3 v2) {
		dest.x = v1.x / v2.x;
		dest.y = v1.y / v2.y;
		dest.z = v1.z / v2.z;
	}
	
	public static void setMin(v3 a, v3 b) {
		a.x = Math.min(a.x, b.x);
		a.y = Math.min(a.y, b.y);
		a.z = Math.min(a.z, b.z);
	}
	
	public static void setMax(v3 a, v3 b) {
		a.x = Math.max(a.x, b.x);
		a.y = Math.max(a.y, b.y);
		a.z = Math.max(a.z, b.z);
	}
	
	public static float dot3(Vector4f v0, v3 v1) {
		return (v0.x*v1.x + v0.y*v1.y + v0.z*v1.z);
	}

	public static float dot3(Vector4f v0, Vector4f v1) {
		return (v0.x*v1.x + v0.y*v1.y + v0.z*v1.z);
	}

	public static float dot3(v3 v0, Vector4f v1) {
		return (v0.x*v1.x + v0.y*v1.y + v0.z*v1.z);
	}

	public static float lengthSquared3(Vector4f v) {
		return (v.x*v.x + v.y*v.y + v.z*v.z);
	}

	public static void normalize3(Vector4f v) {
		float norm = (float)(1.0/ Math.sqrt(v.x*v.x + v.y*v.y + v.z*v.z));
		v.x *= norm;
		v.y *= norm;
		v.z *= norm;
	}









	
}
