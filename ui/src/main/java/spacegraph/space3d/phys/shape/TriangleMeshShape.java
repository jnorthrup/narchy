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
import spacegraph.space3d.phys.math.AabbUtil2;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.Matrix3f;

/**
 * Concave triangle mesh abstract class. Use {@link BvhTriangleMeshShape} as concrete
 * implementation.
 *
 * @author jezek2
 */
public abstract class TriangleMeshShape extends ConcaveShape {

	final v3 localAabbMin = new v3();
	final v3 localAabbMax = new v3();
	final StridingMeshInterface meshInterface;

	/**
	 * TriangleMeshShape constructor has been disabled/protected, so that users will not mistakenly use this class.
	 * Don't use btTriangleMeshShape but use btBvhTriangleMeshShape instead!
	 */
    TriangleMeshShape(StridingMeshInterface meshInterface) {
		this.meshInterface = meshInterface;

		
		
	}

	private v3 localGetSupportingVertex(v3 vec, v3 out) {
		v3 tmp = new v3();

		v3 supportVertex = out;

		Transform ident = new Transform();
		ident.setIdentity();

		SupportVertexCallback supportCallback = new SupportVertexCallback(vec, ident);

		v3 aabbMax = new v3();
		aabbMax.set(1e30f, 1e30f, 1e30f);
		tmp.negated(aabbMax);

		processAllTriangles(supportCallback, tmp, aabbMax);

		supportCallback.getSupportVertexLocal(supportVertex);

		return out;
	}

	public v3 localGetSupportingVertexWithoutMargin(v3 vec, v3 out) {
		assert (false);
		return localGetSupportingVertex(vec, out);
	}

	void recalcLocalAabb() {
		for (int i = 0; i < 3; i++) {
			v3 vec = new v3();
			vec.set(0f, 0f, 0f);
			VectorUtil.setCoord(vec, i, 1f);
			v3 tmp = localGetSupportingVertex(vec, new v3());
			VectorUtil.setCoord(localAabbMax, i, VectorUtil.coord(tmp, i) + collisionMargin);
			VectorUtil.setCoord(vec, i, -1f);
			localGetSupportingVertex(vec, tmp);
			VectorUtil.setCoord(localAabbMin, i, VectorUtil.coord(tmp, i) - collisionMargin);
		}
	}

	@Override
	public void getAabb(Transform trans, v3 aabbMin, v3 aabbMax) {
		v3 tmp = new v3();

		v3 localHalfExtents = new v3();
		localHalfExtents.sub(localAabbMax, localAabbMin);
		localHalfExtents.scaled(0.5f);

		v3 localCenter = new v3();
		localCenter.add(localAabbMax, localAabbMin);
		localCenter.scaled(0.5f);

		Matrix3f abs_b = new Matrix3f(trans.basis);
		MatrixUtil.absolute(abs_b);

		v3 center = new v3(localCenter);
		trans.transform(center);

		v3 extent = new v3();
		abs_b.getRow(0, tmp);
		extent.x = tmp.dot(localHalfExtents);
		abs_b.getRow(1, tmp);
		extent.y = tmp.dot(localHalfExtents);
		abs_b.getRow(2, tmp);
		extent.z = tmp.dot(localHalfExtents);

		float m = getMargin();
		extent.add(m, m, m);

		aabbMin.sub(center, extent);
		aabbMax.add(center, extent);
	}

	@Override
	public void processAllTriangles(TriangleCallback callback, v3 aabbMin, v3 aabbMax) {
		FilteredCallback filterCallback = new FilteredCallback(callback, aabbMin, aabbMax);

		meshInterface.internalProcessAllTriangles(filterCallback, aabbMin, aabbMax);
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {
		
		assert (false);
		inertia.zero();
	}


	@Override
	public void setLocalScaling(v3 scaling) {
		meshInterface.setScaling(scaling);
		recalcLocalAabb();
	}

	@Override
	public v3 getLocalScaling(v3 out) {
		return meshInterface.getScaling(out);
	}

	public StridingMeshInterface getMeshInterface() {
		return meshInterface;
	}

	public v3 getLocalAabbMin(v3 out) {
		out.set(localAabbMin);
		return out;
	}

	public v3 getLocalAabbMax(v3 out) {
		out.set(localAabbMax);
		return out;
	}

	@Override
	public String getName() {
		return "TRIANGLEMESH";
	}

	

	private static class SupportVertexCallback extends TriangleCallback {
		private final v3 supportVertexLocal = new v3();
		final Transform worldTrans = new Transform();
		float maxDot = -1e30f;
		final v3 supportVecLocal = new v3();

		SupportVertexCallback(v3 supportVecWorld, Transform trans) {
			this.worldTrans.set(trans);
			MatrixUtil.transposeTransform(supportVecLocal, supportVecWorld, worldTrans.basis);
		}

		@Override
        public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
			for (int i = 0; i < 3; i++) {
				float dot = supportVecLocal.dot(triangle[i]);
				if (dot > maxDot) {
					maxDot = dot;
					supportVertexLocal.set(triangle[i]);
				}
			}
		}

		public v3 getSupportVertexWorldSpace(v3 out) {
			out.set(supportVertexLocal);
			worldTrans.transform(out);
			return out;
		}

		v3 getSupportVertexLocal(v3 out) {
			out.set(supportVertexLocal);
			return out;
		}
	}

	private static class FilteredCallback extends InternalTriangleIndexCallback {
		final TriangleCallback callback;
		final v3 aabbMin = new v3();
		final v3 aabbMax = new v3();

		FilteredCallback(TriangleCallback callback, v3 aabbMin, v3 aabbMax) {
			this.callback = callback;
			this.aabbMin.set(aabbMin);
			this.aabbMax.set(aabbMax);
		}

		@Override
        public void internalProcessTriangleIndex(v3[] triangle, int partId, int triangleIndex) {
			if (AabbUtil2.testTriangleAgainstAabb2(triangle, aabbMin, aabbMax)) {
				
				callback.processTriangle(triangle, partId, triangleIndex);
			}
		}
	}

}
