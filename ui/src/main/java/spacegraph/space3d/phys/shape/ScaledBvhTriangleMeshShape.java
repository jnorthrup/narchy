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
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.Matrix3f;



/**
 * The ScaledBvhTriangleMeshShape allows to instance a scaled version of an existing
 * {@link BvhTriangleMeshShape}. Note that each {@link BvhTriangleMeshShape} still can
 * have its own local scaling, independent from this ScaledBvhTriangleMeshShape 'localScaling'.
 *
 * @author jezek2
 */
public class ScaledBvhTriangleMeshShape extends ConcaveShape {

	private final v3 localScaling = new v3();
	private final BvhTriangleMeshShape bvhTriMeshShape;

	public ScaledBvhTriangleMeshShape(BvhTriangleMeshShape childShape, v3 localScaling) {
		this.localScaling.set(localScaling);
		this.bvhTriMeshShape = childShape;
	}

	public BvhTriangleMeshShape getChildShape() {
		return bvhTriMeshShape;
	}

	@Override
	public void processAllTriangles(TriangleCallback callback, v3 aabbMin, v3 aabbMax) {
		ScaledTriangleCallback scaledCallback = new ScaledTriangleCallback(callback, localScaling);

		v3 invLocalScaling = new v3();
		invLocalScaling.set(1.f / localScaling.x, 1.f / localScaling.y, 1.f / localScaling.z);

		v3 scaledAabbMin = new v3();
		v3 scaledAabbMax = new v3();

		
		scaledAabbMin.x = (localScaling.x >= 0f ? aabbMin.x : aabbMax.x) * invLocalScaling.x;
		scaledAabbMin.y = (localScaling.y >= 0f ? aabbMin.y : aabbMax.y) * invLocalScaling.y;
		scaledAabbMin.z = (localScaling.z >= 0f ? aabbMin.z : aabbMax.z) * invLocalScaling.z;

		scaledAabbMax.x = (localScaling.x <= 0f ? aabbMin.x : aabbMax.x) * invLocalScaling.x;
		scaledAabbMax.y = (localScaling.y <= 0f ? aabbMin.y : aabbMax.y) * invLocalScaling.y;
		scaledAabbMax.z = (localScaling.z <= 0f ? aabbMin.z : aabbMax.z) * invLocalScaling.z;

		bvhTriMeshShape.processAllTriangles(scaledCallback, scaledAabbMin, scaledAabbMax);
	}

	@Override
	public void getAabb(Transform trans, v3 aabbMin, v3 aabbMax) {
		v3 localAabbMin = bvhTriMeshShape.getLocalAabbMin(new v3());
		v3 localAabbMax = bvhTriMeshShape.getLocalAabbMax(new v3());

		v3 tmpLocalAabbMin = new v3();
		v3 tmpLocalAabbMax = new v3();
		VectorUtil.mul(tmpLocalAabbMin, localAabbMin, localScaling);
		VectorUtil.mul(tmpLocalAabbMax, localAabbMax, localScaling);

		localAabbMin.x = (localScaling.x >= 0f) ? tmpLocalAabbMin.x : tmpLocalAabbMax.x;
		localAabbMin.y = (localScaling.y >= 0f) ? tmpLocalAabbMin.y : tmpLocalAabbMax.y;
		localAabbMin.z = (localScaling.z >= 0f) ? tmpLocalAabbMin.z : tmpLocalAabbMax.z;
		localAabbMax.x = (localScaling.x <= 0f) ? tmpLocalAabbMin.x : tmpLocalAabbMax.x;
		localAabbMax.y = (localScaling.y <= 0f) ? tmpLocalAabbMin.y : tmpLocalAabbMax.y;
		localAabbMax.z = (localScaling.z <= 0f) ? tmpLocalAabbMin.z : tmpLocalAabbMax.z;

		v3 localHalfExtents = new v3();
		localHalfExtents.sub(localAabbMax, localAabbMin);
		localHalfExtents.scaled(0.5f);

		float margin = bvhTriMeshShape.getMargin();
		localHalfExtents.x += margin;
		localHalfExtents.y += margin;
		localHalfExtents.z += margin;

		v3 localCenter = new v3();
		localCenter.add(localAabbMax, localAabbMin);
		localCenter.scaled(0.5f);

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

		aabbMin.sub(center, extent);
		aabbMax.add(center, extent);
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.SCALED_TRIANGLE_MESH_SHAPE_PROXYTYPE;
	}

	@Override
	public void setLocalScaling(v3 scaling) {
		localScaling.set(scaling);
	}

	@Override
	public v3 getLocalScaling(v3 out) {
		out.set(localScaling);
		return out;
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {
	}

	@Override
	public String getName() {
		return "SCALEDBVHTRIANGLEMESH";
	}

	

	private static class ScaledTriangleCallback extends TriangleCallback {
		private final TriangleCallback originalCallback;
		private final v3 localScaling;
		private final v3[] newTriangle = new v3[3];

		ScaledTriangleCallback(TriangleCallback originalCallback, v3 localScaling) {
			this.originalCallback = originalCallback;
			this.localScaling = localScaling;
			
			for (int i=0; i<newTriangle.length; i++) {
				newTriangle[i] = new v3();
			}
		}

		@Override
        public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
			VectorUtil.mul(newTriangle[0], triangle[0], localScaling);
			VectorUtil.mul(newTriangle[1], triangle[1], localScaling);
			VectorUtil.mul(newTriangle[2], triangle[2], localScaling);
			originalCallback.processTriangle(newTriangle, partId, triangleIndex);
		}
	}

}
