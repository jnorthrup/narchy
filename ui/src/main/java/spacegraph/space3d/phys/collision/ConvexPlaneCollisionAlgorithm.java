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
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.ConvexShape;
import spacegraph.space3d.phys.shape.StaticPlaneShape;
import spacegraph.space3d.phys.util.OArrayList;

/**
 * ConvexPlaneCollisionAlgorithm provides convex/plane collision detection.
 * 
 * @author jezek2
 */
public class ConvexPlaneCollisionAlgorithm extends CollisionAlgorithm {

	private boolean ownManifold;
	private PersistentManifold manifoldPtr;
	private boolean isSwapped;
	
	private void init(PersistentManifold mf, CollisionAlgorithmConstructionInfo ci, Collidable col0, Collidable col1, boolean isSwapped) {
		super.init(ci);
		this.ownManifold = false;
		this.manifoldPtr = mf;
		this.isSwapped = isSwapped;

		var convexObj = isSwapped ? col1 : col0;
		var planeObj = isSwapped ? col0 : col1;

		if (manifoldPtr == null && intersecter.needsCollision(convexObj, planeObj)) {
			manifoldPtr = intersecter.getNewManifold(convexObj, planeObj);
			ownManifold = true;
		}
	}

	@Override
	public void destroy() {
		if (ownManifold) {
			if (manifoldPtr != null) {
				intersecter.releaseManifold(manifoldPtr);
			}
			manifoldPtr = null;
		}
	}

	@Override
	public void processCollision(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		if (manifoldPtr == null) {
			return;
		}

		var tmpTrans = new Transform();

		var convexObj = isSwapped ? body1 : body0;
		var planeObj = isSwapped ? body0 : body1;

		var convexShape = (ConvexShape) convexObj.shape();
		var planeShape = (StaticPlaneShape) planeObj.shape();

		var planeNormal = planeShape.getPlaneNormal(new v3());
		var planeConstant = planeShape.getPlaneConstant();

		var planeInConvex = new Transform();
		convexObj.getWorldTransform(planeInConvex);
		planeInConvex.invert();
		planeInConvex.mul(planeObj.getWorldTransform(tmpTrans));

		var convexInPlaneTrans = new Transform();
		convexInPlaneTrans.invert(planeObj.getWorldTransform(tmpTrans));
		convexInPlaneTrans.mul(convexObj.getWorldTransform(tmpTrans));

		var tmp = new v3();
		tmp.negated(planeNormal);
		planeInConvex.basis.transform(tmp);

		var vtx = convexShape.localGetSupportingVertex(tmp, new v3());
		var vtxInPlane = new v3(vtx);
		convexInPlaneTrans.transform(vtxInPlane);

		var distance = (planeNormal.dot(vtxInPlane) - planeConstant);

		var vtxInPlaneProjected = new v3();
		tmp.scale(distance, planeNormal);
		vtxInPlaneProjected.sub(vtxInPlane, tmp);

		var vtxInPlaneWorld = new v3(vtxInPlaneProjected);
		planeObj.getWorldTransform(tmpTrans).transform(vtxInPlaneWorld);

		var breakingThresh = manifoldPtr.getContactBreakingThreshold();
        resultOut.setPersistentManifold(manifoldPtr);
		var hasCollision = distance < breakingThresh;
        if (hasCollision) {

			var normalOnSurfaceB = new v3(planeNormal);
			planeObj.getWorldTransform(tmpTrans).basis.transform(normalOnSurfaceB);

			var pOnB = new v3(vtxInPlaneWorld);
			resultOut.addContactPoint(normalOnSurfaceB, pOnB, distance, breakingThresh);
		}
		if (ownManifold) {
			if (manifoldPtr.numContacts() != 0) {
				resultOut.refreshContactPoints();
			}
		}
	}

	@Override
	public float calculateTimeOfImpact(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		
		return 1f;
	}

	@Override
	public void getAllContactManifolds(OArrayList<PersistentManifold> manifoldArray) {
		if (manifoldPtr != null && ownManifold) {
			manifoldArray.add(manifoldPtr);
		}
	}

	

	public static class CreateFunc extends CollisionAlgorithmCreateFunc {

		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1) {
			var algo = new ConvexPlaneCollisionAlgorithm();
			if (!swapped) {
				algo.init(null, ci, body0, body1, false);
			}
			else {
				algo.init(null, ci, body0, body1, true);
			}
			return algo;
		}

    }
	
}
