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
import spacegraph.space3d.phys.math.VectorUtil;

/**
 * BvhTriangleMeshShape is a static-triangle mesh shape with several optimizations,
 * such as bounding volume hierarchy. It is recommended to enable useQuantizedAabbCompression
 * for better memory usage.<p>
 *
 * It takes a triangle mesh as input, for example a {@link TriangleMesh} or
 * {@link TriangleIndexVertexArray}. The BvhTriangleMeshShape class allows for
 * triangle mesh deformations by a refit or partialRefit method.<p>
 *
 * Instead of building the bounding volume hierarchy acceleration structure, it is
 * also possible to serialize (save) and deserialize (load) the structure from disk.
 * See ConcaveDemo for an example.
 * 
 * @author jezek2
 */
public class BvhTriangleMeshShape extends TriangleMeshShape {

	private OptimizedBvh bvh;
	private boolean useQuantizedAabbCompression;
	private boolean ownsBvh;

	public BvhTriangleMeshShape() {
		super(null);
		this.bvh = null;
		this.ownsBvh = false;
	}

	public BvhTriangleMeshShape(StridingMeshInterface meshInterface, boolean useQuantizedAabbCompression) {
		this(meshInterface, useQuantizedAabbCompression, true);
	}

	private BvhTriangleMeshShape(StridingMeshInterface meshInterface, boolean useQuantizedAabbCompression, boolean buildBvh) {
		super(meshInterface);
		this.bvh = null;
		this.useQuantizedAabbCompression = useQuantizedAabbCompression;
		this.ownsBvh = false;

		
		

		v3 bvhAabbMin = new v3(), bvhAabbMax = new v3();
		meshInterface.calculateAabbBruteForce(bvhAabbMin, bvhAabbMax);

		if (buildBvh) {
			bvh = new OptimizedBvh();
			bvh.build(meshInterface, useQuantizedAabbCompression, bvhAabbMin, bvhAabbMax);
			ownsBvh = true;

			
			recalcLocalAabb();
		}

		
	}

	/**
	 * Optionally pass in a larger bvh aabb, used for quantization. This allows for deformations within this aabb.
	 */
	public BvhTriangleMeshShape(StridingMeshInterface meshInterface, boolean useQuantizedAabbCompression, v3 bvhAabbMin, v3 bvhAabbMax) {
		this(meshInterface, useQuantizedAabbCompression, bvhAabbMin, bvhAabbMax, true);
	}

	/**
	 * Optionally pass in a larger bvh aabb, used for quantization. This allows for deformations within this aabb.
	 */
    private BvhTriangleMeshShape(StridingMeshInterface meshInterface, boolean useQuantizedAabbCompression, v3 bvhAabbMin, v3 bvhAabbMax, boolean buildBvh) {
		super(meshInterface);

		this.bvh = null;
		this.useQuantizedAabbCompression = useQuantizedAabbCompression;
		this.ownsBvh = false;

		
		

		if (buildBvh) {
			bvh = new OptimizedBvh();

			bvh.build(meshInterface, useQuantizedAabbCompression, bvhAabbMin, bvhAabbMax);
			ownsBvh = true;
		}

		
		recalcLocalAabb();
		
	}

	public boolean getOwnsBvh() {
		return ownsBvh;
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.TRIANGLE_MESH_SHAPE_PROXYTYPE;
	}

	public void performRaycast(TriangleCallback callback, v3 raySource, v3 rayTarget) {
        MyNodeOverlapCallback myNodeCallback = new MyNodeOverlapCallback(callback, meshInterface);

		bvh.reportRayOverlappingNodex(myNodeCallback, raySource, rayTarget);

	}

	public void performConvexcast(TriangleCallback callback, v3 raySource, v3 rayTarget, v3 aabbMin, v3 aabbMax) {
        MyNodeOverlapCallback myNodeCallback = new MyNodeOverlapCallback(callback, meshInterface);

		bvh.reportBoxCastOverlappingNodex(myNodeCallback, raySource, rayTarget, aabbMin, aabbMax);
	}

	/**
	 * Perform bvh tree traversal and report overlapping triangles to 'callback'.
	 */
	@Override
	public void processAllTriangles(TriangleCallback callback, v3 aabbMin, v3 aabbMax) {


        MyNodeOverlapCallback myNodeCallback = new MyNodeOverlapCallback(callback, meshInterface);

		bvh.reportAabbOverlappingNodex(myNodeCallback, aabbMin, aabbMax);
		
	}

	public void refitTree(v3 aabbMin, v3 aabbMax) {
		
		
		bvh.refit(meshInterface);

		recalcLocalAabb();
	}

	/**
	 * For a fast incremental refit of parts of the tree. Note: the entire AABB of the tree will become more conservative, it never shrinks.
	 */
	public void partialRefitTree(v3 aabbMin, v3 aabbMax) {
		OptimizedBvh.refitPartial(meshInterface,aabbMin,aabbMax );

		VectorUtil.setMin(localAabbMin, aabbMin);
		VectorUtil.setMax(localAabbMax, aabbMax);
	}

	@Override
	public String getName() {
		return "BVHTRIANGLEMESH";
	}

	@Override
	public void setLocalScaling(v3 scaling) {
        v3 tmp = new v3();
		tmp.sub(getLocalScaling(new v3()), scaling);

		if (tmp.lengthSquared() > BulletGlobals.SIMD_EPSILON) {
			super.setLocalScaling(scaling);
			/*
			if (ownsBvh)
			{
			m_bvh->~btOptimizedBvh();
			btAlignedFree(m_bvh);
			}
			*/
			
			bvh = new OptimizedBvh();
			
			bvh.build(meshInterface, useQuantizedAabbCompression, localAabbMin, localAabbMax);
			ownsBvh = true;
		}
	}

	public OptimizedBvh getOptimizedBvh() {
		return bvh;
	}

	public void setOptimizedBvh(OptimizedBvh bvh) {
        v3 scaling = new v3();
		scaling.set(1f, 1f, 1f);
		setOptimizedBvh(bvh, scaling);
	}

	private void setOptimizedBvh(OptimizedBvh bvh, v3 scaling) {
		assert (this.bvh == null);
		assert (!ownsBvh);

		this.bvh = bvh;
		ownsBvh = false;


        v3 tmp = new v3();
		tmp.sub(getLocalScaling(new v3()), scaling);

		if (tmp.lengthSquared() > BulletGlobals.SIMD_EPSILON) {
			super.setLocalScaling(scaling);
		}
	}

	public boolean usesQuantizedAabbCompression() {
		return useQuantizedAabbCompression;
	}

	

	protected static class MyNodeOverlapCallback extends NodeOverlapCallback {
		final StridingMeshInterface meshInterface;
		final TriangleCallback callback;

		private final v3[] triangle/*[3]*/ = { new v3(), new v3(), new v3() };

		MyNodeOverlapCallback(TriangleCallback callback, StridingMeshInterface meshInterface) {
			this.meshInterface = meshInterface;
			this.callback = callback;
		}
		
		@Override
        public void processNode(int nodeSubPart, int nodeTriangleIndex) {
            VertexData data = meshInterface.getLockedReadOnlyVertexIndexBase(nodeSubPart);

            v3 meshScaling = meshInterface.getScaling(new v3());

			data.getTriangle(nodeTriangleIndex*3, meshScaling, triangle);

			/* Perform ray vs. triangle collision here */
			callback.processTriangle(triangle, nodeSubPart, nodeTriangleIndex);
			
			meshInterface.unLockReadOnlyVertexBase(nodeSubPart);
		}
	}
	
}
