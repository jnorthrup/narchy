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


import spacegraph.util.math.Matrix3f;
import jcog.math.v3;

import static jcog.math.v3.v;

/**
 * Utility functions for axis aligned bounding boxes (AABB).
 * 
 * @author jezek2
 */
public class AabbUtil2 {

	public static void aabbExpand(v3 aabbMin, v3 aabbMax, v3 expansionMin, v3 expansionMax) {
		aabbMin.add(expansionMin);
		aabbMax.add(expansionMax);
	}

	private static int outcode(v3 p, v3 halfExtent) {
		float hx = halfExtent.x;
		float px = p.x;
		float py = p.y;
		float pz = p.z;
		float hy = halfExtent.y;
		float hz = halfExtent.z;
		return  (px < -hx ? 0x01 : 0x0) |
				(px > hx ? 0x08 : 0x0) |
				(py < -hy ? 0x02 : 0x0) |
				(py > hy ? 0x10 : 0x0) |
				(pz < -hz ? 0x4 : 0x0) |
				(pz > hz ? 0x20 : 0x0);
	}

	public static boolean rayAabb(v3 rayFrom, v3 rayTo, v3 aabbMin, v3 aabbMax, float[] param) {
		return rayAabb(rayFrom, rayTo, aabbMin, aabbMax, param, v() );
	}

	public static boolean rayAabb(v3 rayFrom, v3 rayTo, v3 aabbMin, v3 aabbMax, float[] param, v3 normal) {
		v3 aabbHalfExtent = new v3();
		v3 aabbCenter = new v3();
		v3 source = new v3();
		v3 target = new v3();
		v3 r = new v3();
		v3 hitNormal = new v3();

		aabbHalfExtent.sub(aabbMax, aabbMin);
		aabbHalfExtent.scale(0.5f);

		aabbCenter.add(aabbMax, aabbMin);
		aabbCenter.scale(0.5f);

		source.sub(rayFrom, aabbCenter);
		target.sub(rayTo, aabbCenter);

		int sourceOutcode = outcode(source, aabbHalfExtent);
		int targetOutcode = outcode(target, aabbHalfExtent);
		if ((sourceOutcode & targetOutcode) == 0x0) {
			float lambda_exit = param[0];
			r.sub(target, source);

			hitNormal.set(0f, 0f, 0f);
			int bit = 1;

			float normSign = 1f;
			float lambda_enter = 0f;
			for (int j = 0; j < 2; j++) {
				for (int i = 0; i != 3; ++i) {
					if ((sourceOutcode & bit) != 0) {
						float lambda = (-VectorUtil.coord(source, i) - VectorUtil.coord(aabbHalfExtent, i) * normSign) / VectorUtil.coord(r, i);
						if (lambda_enter <= lambda) {
							lambda_enter = lambda;
							hitNormal.set(0f, 0f, 0f);
							VectorUtil.setCoord(hitNormal, i, normSign);
						}
					}
					else if ((targetOutcode & bit) != 0) {
						float lambda = (-VectorUtil.coord(source, i) - VectorUtil.coord(aabbHalfExtent, i) * normSign) / VectorUtil.coord(r, i);
						
						lambda_exit = Math.min(lambda_exit, lambda);
					}
					bit <<= 1;
				}
				normSign = -1f;
			}
			if (lambda_enter <= lambda_exit) {
				param[0] = lambda_enter;
				normal.set(hitNormal);
				return true;
			}
		}
		return false;
	}

	/**
	 * Conservative test for overlap between two AABBs.
	 */
	public static boolean testAabbAgainstAabb2(v3 aabbMin1, v3 aabbMax1, v3 aabbMin2, v3 aabbMax2) {
		return !(aabbMin1.x > aabbMax2.x || aabbMax1.x < aabbMin2.x) &&
			   !(aabbMin1.z > aabbMax2.z || aabbMax1.z < aabbMin2.z) &&
			   !(aabbMin1.y > aabbMax2.y || aabbMax1.y < aabbMin2.y);
	}

	/**
	 * Conservative test for overlap between triangle and AABB.
	 */
	public static boolean testTriangleAgainstAabb2(v3[] vertices, v3 aabbMin, v3 aabbMax) {
		v3 p1 = vertices[0];
		v3 p2 = vertices[1];
		v3 p3 = vertices[2];

		if (Math.min(Math.min(p1.x, p2.x), p3.x) > aabbMax.x) return false;
		if (Math.max(Math.max(p1.x, p2.x), p3.x) < aabbMin.x) return false;

		if (Math.min(Math.min(p1.z, p2.z), p3.z) > aabbMax.z) return false;
		if (Math.max(Math.max(p1.z, p2.z), p3.z) < aabbMin.z) return false;

		if (Math.min(Math.min(p1.y, p2.y), p3.y) > aabbMax.y) return false;
		return Math.max(Math.max(p1.y, p2.y), p3.y) >= aabbMin.y;

	}

	public static void transformAabb(v3 halfExtents, float margin, Transform t, v3 aabbMinOut, v3 aabbMaxOut) {
		v3 halfExtentsWithMargin = new v3();
		halfExtentsWithMargin.x = halfExtents.x + margin;
		halfExtentsWithMargin.y = halfExtents.y + margin;
		halfExtentsWithMargin.z = halfExtents.z + margin;

		Matrix3f abs_b = new Matrix3f(t.basis);
		MatrixUtil.absolute(abs_b);

		v3 tmp = new v3();

		v3 center = new v3(t);
		v3 extent = new v3();
		abs_b.getRow(0, tmp);
		extent.x = tmp.dot(halfExtentsWithMargin);
		abs_b.getRow(1, tmp);
		extent.y = tmp.dot(halfExtentsWithMargin);
		abs_b.getRow(2, tmp);
		extent.z = tmp.dot(halfExtentsWithMargin);

		aabbMinOut.sub(center, extent);
		aabbMaxOut.add(center, extent);
	}

	public static void transformAabb(v3 localAabbMin, v3 localAabbMax, float margin, Transform trans, v3 aabbMinOut, v3 aabbMaxOut) {
		assert (localAabbMin.x <= localAabbMax.x);
		assert (localAabbMin.y <= localAabbMax.y);
		assert (localAabbMin.z <= localAabbMax.z);

		v3 localHalfExtents = new v3();
		localHalfExtents.sub(localAabbMax, localAabbMin);
		localHalfExtents.scale(0.5f);

		localHalfExtents.x += margin;
		localHalfExtents.y += margin;
		localHalfExtents.z += margin;

		v3 localCenter = new v3();
		localCenter.add(localAabbMax, localAabbMin);
		localCenter.scale(0.5f);

		Matrix3f abs_b = new Matrix3f(trans.basis);
		MatrixUtil.absolute(abs_b);

		v3 center = new v3(localCenter);
		trans.transform(center);

		v3 extent = new v3();
		v3 tmp = new v3();

		abs_b.getRow(0, tmp);
		extent.x = tmp.dot(localHalfExtents);
		abs_b.getRow(1, tmp);
		extent.y = tmp.dot(localHalfExtents);
		abs_b.getRow(2, tmp);
		extent.z = tmp.dot(localHalfExtents);

		aabbMinOut.sub(center, extent);
		aabbMaxOut.add(center, extent);
	}

}
