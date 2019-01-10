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

import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.util.math.v3;

/**
 * MinkowskiSumShape is only for advanced users. This shape represents implicit
 * based minkowski sum of two convex implicit shapes.
 * 
 * @author jezek2
 */
public class MinkowskiSumShape extends ConvexInternalShape {

	private final Transform transA = new Transform();
	private final Transform transB = new Transform();
	private final ConvexShape shapeA;
	private final ConvexShape shapeB;

	public MinkowskiSumShape(ConvexShape shapeA, ConvexShape shapeB) {
		this.shapeA = shapeA;
		this.shapeB = shapeB;
		this.transA.setIdentity();
		this.transB.setIdentity();
	}
	
	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 vec, v3 out) {
		v3 tmp = new v3();
		v3 supVertexA = new v3();
		v3 supVertexB = new v3();

		
		tmp.negated(vec);
		MatrixUtil.transposeTransform(tmp, tmp, transA.basis);
		shapeA.localGetSupportingVertexWithoutMargin(tmp, supVertexA);
		transA.transform(supVertexA);

		
		MatrixUtil.transposeTransform(tmp, vec, transB.basis);
		shapeB.localGetSupportingVertexWithoutMargin(tmp, supVertexB);
		transB.transform(supVertexB);

		
		out.sub(supVertexA, supVertexB);
		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		
		for (int i = 0; i < numVectors; i++) {
			localGetSupportingVertexWithoutMargin(vectors[i], supportVerticesOut[i]);
		}
	}

	@Override
	public void getAabb(Transform t, v3 aabbMin, v3 aabbMax) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.MINKOWSKI_SUM_SHAPE_PROXYTYPE;
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {
		assert (false);
		inertia.set(0, 0, 0);
	}

	@Override
	public String getName() {
		return "MinkowskiSum";
	}
	
	@Override
	public float getMargin() {
		return shapeA.getMargin() + shapeB.getMargin();
	}

	public void setTransformA(Transform transA) {
		this.transA.set(transA);
	}

	public void setTransformB(Transform transB) {
		this.transB.set(transB);
	}

	public void getTransformA(Transform dest) {
		dest.set(transA);
	}

	public void getTransformB(Transform dest) {
		dest.set(transB);
	}

}
