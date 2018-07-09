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

import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.CollisionAlgorithm;
import spacegraph.space3d.phys.collision.broad.CollisionAlgorithmConstructionInfo;
import spacegraph.space3d.phys.collision.broad.DispatcherInfo;
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.ConvexShape;
import spacegraph.space3d.phys.shape.StaticPlaneShape;
import spacegraph.space3d.phys.util.OArrayList;
import spacegraph.util.math.v3;

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

		Collidable convexObj = isSwapped ? col1 : col0;
		Collidable planeObj = isSwapped ? col0 : col1;

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

		Transform tmpTrans = new Transform();

		Collidable convexObj = isSwapped ? body1 : body0;
		Collidable planeObj = isSwapped ? body0 : body1;

		ConvexShape convexShape = (ConvexShape) convexObj.shape();
		StaticPlaneShape planeShape = (StaticPlaneShape) planeObj.shape();

		boolean hasCollision = false;
		v3 planeNormal = planeShape.getPlaneNormal(new v3());
		float planeConstant = planeShape.getPlaneConstant();

		Transform planeInConvex = new Transform();
		convexObj.getWorldTransform(planeInConvex);
		planeInConvex.inverse();
		planeInConvex.mul(planeObj.getWorldTransform(tmpTrans));

		Transform convexInPlaneTrans = new Transform();
		convexInPlaneTrans.inverse(planeObj.getWorldTransform(tmpTrans));
		convexInPlaneTrans.mul(convexObj.getWorldTransform(tmpTrans));

		v3 tmp = new v3();
		tmp.negate(planeNormal);
		planeInConvex.basis.transform(tmp);

		v3 vtx = convexShape.localGetSupportingVertex(tmp, new v3());
		v3 vtxInPlane = new v3(vtx);
		convexInPlaneTrans.transform(vtxInPlane);

		float distance = (planeNormal.dot(vtxInPlane) - planeConstant);

		v3 vtxInPlaneProjected = new v3();
		tmp.scale(distance, planeNormal);
		vtxInPlaneProjected.sub(vtxInPlane, tmp);

		v3 vtxInPlaneWorld = new v3(vtxInPlaneProjected);
		planeObj.getWorldTransform(tmpTrans).transform(vtxInPlaneWorld);

		float breakingThresh = manifoldPtr.getContactBreakingThreshold();
		hasCollision = distance < breakingThresh;
		resultOut.setPersistentManifold(manifoldPtr);
		if (hasCollision) {
			
			v3 normalOnSurfaceB = new v3(planeNormal);
			planeObj.getWorldTransform(tmpTrans).basis.transform(normalOnSurfaceB);

			v3 pOnB = new v3(vtxInPlaneWorld);
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
			ConvexPlaneCollisionAlgorithm algo = new ConvexPlaneCollisionAlgorithm();
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
