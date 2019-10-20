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
import spacegraph.util.math.Matrix3f;

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
		var hx = halfExtent.x;
		var px = p.x;
		var py = p.y;
		var pz = p.z;
		var hy = halfExtent.y;
		var hz = halfExtent.z;
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
		var aabbHalfExtent = new v3();
		var aabbCenter = new v3();
		var source = new v3();
		var target = new v3();
		var r = new v3();
		var hitNormal = new v3();

		aabbHalfExtent.sub(aabbMax, aabbMin);
		aabbHalfExtent.scaled(0.5f);

		aabbCenter.add(aabbMax, aabbMin);
		aabbCenter.scaled(0.5f);

		source.sub(rayFrom, aabbCenter);
		target.sub(rayTo, aabbCenter);

		var sourceOutcode = outcode(source, aabbHalfExtent);
		var targetOutcode = outcode(target, aabbHalfExtent);
		if ((sourceOutcode & targetOutcode) == 0x0) {
			var lambda_exit = param[0];
			r.sub(target, source);

			hitNormal.set(0f, 0f, 0f);
			var bit = 1;

			var normSign = 1f;
			var lambda_enter = 0f;
			for (var j = 0; j < 2; j++) {
				for (var i = 0; i != 3; ++i) {
					if ((sourceOutcode & bit) != 0) {
						var lambda = (-VectorUtil.coord(source, i) - VectorUtil.coord(aabbHalfExtent, i) * normSign) / VectorUtil.coord(r, i);
						if (lambda_enter <= lambda) {
							lambda_enter = lambda;
							hitNormal.set(0f, 0f, 0f);
							VectorUtil.setCoord(hitNormal, i, normSign);
						}
					}
					else if ((targetOutcode & bit) != 0) {
						var lambda = (-VectorUtil.coord(source, i) - VectorUtil.coord(aabbHalfExtent, i) * normSign) / VectorUtil.coord(r, i);
						
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
		var result = false;
		var p1 = vertices[0];
		var p2 = vertices[1];
		var p3 = vertices[2];

		if (!(Math.min(Math.min(p1.x, p2.x), p3.x) > aabbMax.x))
			if (!(Math.max(Math.max(p1.x, p2.x), p3.x) < aabbMin.x))
				if (!(Math.min(Math.min(p1.z, p2.z), p3.z) > aabbMax.z))
					if (!(Math.max(Math.max(p1.z, p2.z), p3.z) < aabbMin.z))
						if (!(Math.min(Math.min(p1.y, p2.y), p3.y) > aabbMax.y))
							if (Math.max(Math.max(p1.y, p2.y), p3.y) >= aabbMin.y) result = true;

		return result;
	}

	public static void transformAabb(v3 halfExtents, float margin, Transform t, v3 aabbMinOut, v3 aabbMaxOut) {
		var halfExtentsWithMargin = new v3();
		halfExtentsWithMargin.x = halfExtents.x + margin;
		halfExtentsWithMargin.y = halfExtents.y + margin;
		halfExtentsWithMargin.z = halfExtents.z + margin;

		var abs_b = new Matrix3f(t.basis);
		MatrixUtil.absolute(abs_b);

		var tmp = new v3();

		var center = new v3(t);
		var extent = new v3();
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

		var localHalfExtents = new v3();
		localHalfExtents.sub(localAabbMax, localAabbMin);
		localHalfExtents.scaled(0.5f);

		localHalfExtents.x += margin;
		localHalfExtents.y += margin;
		localHalfExtents.z += margin;

		var localCenter = new v3();
		localCenter.add(localAabbMax, localAabbMin);
		localCenter.scaled(0.5f);

		var abs_b = new Matrix3f(trans.basis);
		MatrixUtil.absolute(abs_b);

		var center = new v3(localCenter);
		trans.transform(center);

		var extent = new v3();
		var tmp = new v3();

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
