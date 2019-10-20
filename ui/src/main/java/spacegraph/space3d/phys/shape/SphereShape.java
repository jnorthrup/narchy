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
import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.Transform;

/**
 * SphereShape implements an implicit sphere, centered around a local origin with radius.
 * 
 * @author jezek2
 */
public class SphereShape extends ConvexInternalShape {

	public SphereShape() {
		this(0.5f);
	}

	/** scale x unit sphere */
	public SphereShape(float radius) {
		setRadius(radius);
	}

	public void setRadius(float radius) {
		radius = Math.abs(radius);
		implicitShapeDimensions.x = radius;
		collisionMargin = radius;
	}










	@Override
	public void setLocalScaling(float x, float y, float z) {
		super.setLocalScaling(x, y, z);
		collisionMargin = x;
	}

	@Override
	public void setLocalScaling(v3 scaling) {
		setLocalScaling(scaling.x, scaling.y, scaling.z);
	}

	@Override
	public float getBoundingRadius() {
		
		return getRadius();
	}

	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 vec, v3 out) {
		out.set(0f, 0f, 0f);
		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		for (int i = 0; i < numVectors; i++) {
			supportVerticesOut[i].set(0f, 0f, 0f);
		}
	}

	@Override
	public void getAabb(Transform t, v3 aabbMin, v3 aabbMax) {
        float margin = getMargin();
        v3 extent = new v3(margin, margin, margin);

		v3 center = t;
		aabbMin.sub(center, extent);
		aabbMax.add(center, extent);
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.SPHERE_SHAPE_PROXYTYPE;
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {
        float elem = 0.4f * mass * getMargin() * getMargin();
		inertia.set(elem, elem, elem);
	}

	@Override
	public String getName() {
		return "SPHERE";
	}
	
	public float getRadius() {
		return implicitShapeDimensions.x * localScaling.x;
	}

	@Override
	public float getMargin() {
		
		
		return getRadius();
	}
	
}
