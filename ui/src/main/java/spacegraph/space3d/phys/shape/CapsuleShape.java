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

import jcog.math.v3;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.Matrix3f;

/**
 * CapsuleShape represents a capsule around the Y axis, there is also the
 * {@link CapsuleShapeX} aligned around the X axis and {@link CapsuleShapeZ} around
 * the Z axis.<p>
 *
 * The total height is height+2*radius, so the height is just the height between
 * the center of each "sphere" of the capsule caps.<p>
 *
 * CapsuleShape is a convex hull of two spheres. The {@link MultiSphereShape} is
 * a more general collision shape that takes the convex hull of multiple sphere,
 * so it can also represent a capsule when just using two spheres.
 * 
 * @author jezek2
 */
public class CapsuleShape extends ConvexInternalShape {
	
	int upAxis;

	
	CapsuleShape() {
	}
	
	public CapsuleShape(float radius, float height) {
		upAxis = 1;
		implicitShapeDimensions.set(radius, 0.5f * height, radius);
	}

	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 vec0, v3 out) {
		v3 supVec = out;
		supVec.set(0f, 0f, 0f);

		float maxDot = -1e30f;

		v3 vec = new v3(vec0);
		float lenSqr = vec.lengthSquared();
		if (lenSqr < 0.0001f) {
			vec.set(1f, 0f, 0f);
		}
		else {
			float rlen = 1f / (float) Math.sqrt(lenSqr);
			vec.scaled(rlen);
		}

		v3 vtx = new v3();

        float radius = getRadius();

		v3 tmp1 = new v3();
		v3 tmp2 = new v3();
		v3 pos = new v3();

        pos.set(0f, 0f, 0f);
        VectorUtil.setCoord(pos, upAxis, getHalfHeight());

        VectorUtil.mul(tmp1, vec, localScaling);
        tmp1.scaled(radius);
        tmp2.scale(getMargin(), vec);
        vtx.add(pos, tmp1);
        vtx.sub(tmp2);
        float newDot = vec.dot(vtx);
        if (newDot > maxDot) {
            maxDot = newDot;
            supVec.set(vtx);
        }
        pos.set(0f, 0f, 0f);
		VectorUtil.setCoord(pos, upAxis, -getHalfHeight());

		VectorUtil.mul(tmp1, vec, localScaling);
		tmp1.scaled(radius);
		tmp2.scale(getMargin(), vec);
		vtx.add(pos, tmp1);
		vtx.sub(tmp2);
		newDot = vec.dot(vtx);
		if (newDot > maxDot) {
            maxDot = newDot;
            supVec.set(vtx);
        }

		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {
		

		Transform ident = new Transform();
		ident.setIdentity();

		float radius = getRadius();

		v3 halfExtents = new v3();
		halfExtents.set(radius, radius, radius);
		VectorUtil.setCoord(halfExtents, upAxis, radius + getHalfHeight());

		float margin = BulletGlobals.CONVEX_DISTANCE_MARGIN;

		float lx = 2f * (halfExtents.x + margin);
		float ly = 2f * (halfExtents.y + margin);
		float lz = 2f * (halfExtents.z + margin);
		float x2 = lx * lx;
		float y2 = ly * ly;
		float z2 = lz * lz;
		float scaledmass = mass * 0.08333333f;

		inertia.x = scaledmass * (y2 + z2);
		inertia.y = scaledmass * (x2 + z2);
		inertia.z = scaledmass * (x2 + y2);
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.CAPSULE_SHAPE_PROXYTYPE;
	}
	
	@Override
	public void getAabb(Transform t, v3 aabbMin, v3 aabbMax) {
		v3 tmp = new v3();

		v3 halfExtents = new v3();
		halfExtents.set(getRadius(), getRadius(), getRadius());
		VectorUtil.setCoord(halfExtents, upAxis, getRadius() + getHalfHeight());

		halfExtents.x += getMargin();
		halfExtents.y += getMargin();
		halfExtents.z += getMargin();

		Matrix3f abs_b = new Matrix3f();
		abs_b.set(t.basis);
		MatrixUtil.absolute(abs_b);

		v3 center = t;
		v3 extent = new v3();

		abs_b.getRow(0, tmp);
		extent.x = tmp.dot(halfExtents);
		abs_b.getRow(1, tmp);
		extent.y = tmp.dot(halfExtents);
		abs_b.getRow(2, tmp);
		extent.z = tmp.dot(halfExtents);

		aabbMin.sub(center, extent);
		aabbMax.add(center, extent);
	}

	@Override
	public String getName() {
		return "CapsuleShape";
	}
	
	public int getUpAxis() {
		return upAxis;
	}
	
	public float getRadius() {
		int radiusAxis = (upAxis + 2) % 3;
		return VectorUtil.coord(implicitShapeDimensions, radiusAxis);
	}

	public float getHalfHeight() {
		return VectorUtil.coord(implicitShapeDimensions, upAxis);
	}

}
