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

package spacegraph.space3d.phys.collision;

import jcog.math.v3;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.CollisionAlgorithm;
import spacegraph.space3d.phys.collision.broad.CollisionAlgorithmConstructionInfo;
import spacegraph.space3d.phys.collision.broad.DispatcherInfo;
import spacegraph.space3d.phys.collision.broad.Intersecter;
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.shape.TriangleCallback;
import spacegraph.space3d.phys.shape.TriangleShape;

/**
 * For each triangle in the concave mesh that overlaps with the AABB of a convex
 * (see {@link #convexBody} field), processTriangle is called.
 * 
 * @author jezek2
 */
class ConvexTriangleCallback extends TriangleCallback {

	
	
	private final Collidable convexBody;
	private final Collidable triBody;

	private final v3 aabbMin = new v3();
	private final v3 aabbMax = new v3();

	private ManifoldResult resultOut;

	private final Intersecter intersecter;
	private DispatcherInfo dispatchInfoPtr;
	private float collisionMarginTriangle;
	
	public int triangleCount;
	public final PersistentManifold manifoldPtr;
	
	public ConvexTriangleCallback(Intersecter intersecter, Collidable body0, Collidable body1, boolean isSwapped) {
		this.intersecter = intersecter;
		this.dispatchInfoPtr = null;

		convexBody = isSwapped ? body1 : body0;
		triBody = isSwapped ? body0 : body1;

		
		
		
		manifoldPtr = intersecter.getNewManifold(convexBody, triBody);

		clearCache();
	}
	
	public void destroy() {
		clearCache();
		intersecter.releaseManifold(manifoldPtr);
	}

	public void setTimeStepAndCounters(float collisionMarginTriangle, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		this.dispatchInfoPtr = dispatchInfo;
		this.collisionMarginTriangle = collisionMarginTriangle;
		this.resultOut = resultOut;

		
		Transform convexInTriangleSpace = new Transform();

		triBody.getWorldTransform(convexInTriangleSpace);
		convexInTriangleSpace.invert();
		convexInTriangleSpace.mul(convexBody.getWorldTransform(new Transform()));

		CollisionShape convexShape = convexBody.shape();
		
		convexShape.getAabb(convexInTriangleSpace, aabbMin, aabbMax);
		float extraMargin = collisionMarginTriangle;
		v3 extra = new v3();
		extra.set(extraMargin, extraMargin, extraMargin);

		aabbMax.add(extra);
		aabbMin.sub(extra);
	}

	private final CollisionAlgorithmConstructionInfo ci = new CollisionAlgorithmConstructionInfo();
	private final TriangleShape tm = new TriangleShape();
	
	@Override
	public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
		
		

		

		ci.intersecter1 = intersecter;

		Collidable ob = triBody;





























		

		if (convexBody.shape().isConvex()) {
			tm.init(triangle[0], triangle[1], triangle[2]);
			tm.setMargin(collisionMarginTriangle);

			CollisionShape tmpShape = ob.shape();
			ob.internalSetTemporaryCollisionShape(tm);

			CollisionAlgorithm colAlgo = ci.intersecter1.findAlgorithm(convexBody, triBody, manifoldPtr);
			
			

			resultOut.setShapeIdentifiers(-1, -1, partId, triangleIndex);
			
			
			colAlgo.processCollision(convexBody, triBody, dispatchInfoPtr, resultOut);
			
			Intersecter.freeCollisionAlgorithm(colAlgo);
			ob.internalSetTemporaryCollisionShape(tmpShape);
		}
	}

	public void clearCache() {
		intersecter.clearManifold(manifoldPtr);
	}

	public v3 getAabbMin(v3 out) {
		out.set(aabbMin);
		return out;
	}

	public v3 getAabbMax(v3 out) {
		out.set(aabbMax);
		return out;
	}
	
}
