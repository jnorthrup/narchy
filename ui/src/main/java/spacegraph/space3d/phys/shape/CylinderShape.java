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

package spacegraph.space3d.phys.shape;

import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.v3;

/**
 * CylinderShape class implements a cylinder shape primitive, centered around
 * the origin. Its central axis aligned with the Y axis. {@link CylinderShapeX}
 * is aligned with the X axis and {@link CylinderShapeZ} around the Z axis.
 * 
 * @author jezek2
 */
public class CylinderShape extends BoxShape {

	int upAxis;

	public CylinderShape(v3 halfExtents) {
		super(halfExtents);
		upAxis = 1;
		recalcLocalAabb();
	}

	CylinderShape(v3 halfExtents, boolean unused) {
		super(halfExtents);
	}

	@Override
	public void getAabb(Transform t, v3 aabbMin, v3 aabbMax) {
		_PolyhedralConvexShape_getAabb(t, aabbMin, aabbMax);
	}

	static v3 cylinderLocalSupportX(v3 halfExtents, v3 v, v3 out) {
		return cylinderLocalSupport(halfExtents, v, 0, 1, 0, 2, out);
	}

	private static v3 cylinderLocalSupportY(v3 halfExtents, v3 v, v3 out) {
		return cylinderLocalSupport(halfExtents, v, 1, 0, 1, 2, out);
	}

	static v3 cylinderLocalSupportZ(v3 halfExtents, v3 v, v3 out) {
		return cylinderLocalSupport(halfExtents, v, 2, 0, 2, 1, out);
	}
	
	private static v3 cylinderLocalSupport(v3 halfExtents, v3 v, int cylinderUpAxis, int XX, int YY, int ZZ, v3 out) {
		
		

		float radius = VectorUtil.coord(halfExtents, XX);
		float halfHeight = VectorUtil.coord(halfExtents, cylinderUpAxis);

		float d;

		float s = (VectorUtil.coord(v, XX) * VectorUtil.coord(v, XX) + VectorUtil.coord(v, ZZ) * VectorUtil.coord(v, ZZ));
		if (s != 0f) {
			d = radius / (float) Math.sqrt(s);
			VectorUtil.setCoord(out, XX, VectorUtil.coord(v, XX) * d);
			VectorUtil.setCoord(out, YY, VectorUtil.coord(v, YY) < 0f ? -halfHeight : halfHeight);
			VectorUtil.setCoord(out, ZZ, VectorUtil.coord(v, ZZ) * d);
			return out;
		}
		else {
			VectorUtil.setCoord(out, XX, radius);
			VectorUtil.setCoord(out, YY, VectorUtil.coord(v, YY) < 0f ? -halfHeight : halfHeight);
			VectorUtil.setCoord(out, ZZ, 0f);
			return out;
		}
	}

	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 vec, v3 out) {
		return cylinderLocalSupportY(getHalfExtentsWithoutMargin(new v3()), vec, out);
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		for (int i = 0; i < numVectors; i++) {
			cylinderLocalSupportY(getHalfExtentsWithoutMargin(new v3()), vectors[i], supportVerticesOut[i]);
		}
	}

	@Override
	public v3 localGetSupportingVertex(v3 vec, v3 out) {
		v3 supVertex = out;
		localGetSupportingVertexWithoutMargin(vec, supVertex);

		if (getMargin() != 0f) {
			v3 vecnorm = new v3(vec);
			if (vecnorm.lengthSquared() < (BulletGlobals.SIMD_EPSILON * BulletGlobals.SIMD_EPSILON)) {
				vecnorm.set(-1f, -1f, -1f);
			}
			vecnorm.normalize();
			supVertex.scaleAdd(getMargin(), vecnorm, supVertex);
		}
		return out;
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.CYLINDER_SHAPE_PROXYTYPE;
	}

	public int getUpAxis() {
		return upAxis;
	}
	
	public float getRadius() {
		return getHalfExtentsWithMargin(new v3()).x;
	}

	@Override
	public String getName() {
		return "CylinderY";
	}
	
}
