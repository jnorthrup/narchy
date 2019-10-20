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
import spacegraph.space3d.phys.collision.broad.Intersecter;
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.shape.CompoundShape;
import spacegraph.space3d.phys.util.OArrayList;


/**
 * CompoundCollisionAlgorithm supports collision between {@link CompoundShape}s and
 * other collision shapes.
 * 
 * @author jezek2
 */
public class CompoundCollisionAlgorithm extends CollisionAlgorithm {

	private final OArrayList<CollisionAlgorithm> childCollisionAlgorithms = new OArrayList<>();
	private boolean isSwapped;
	
	private void init(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1, boolean isSwapped) {
		super.init(ci);

		this.isSwapped = isSwapped;

		var colObj = isSwapped ? body1 : body0;
		var otherObj = isSwapped ? body0 : body1;
		assert (colObj.shape().isCompound());

		var compoundShape = (CompoundShape) colObj.shape();
		var numChildren = compoundShape.size();


        for (var i = 0; i < numChildren; i++) {
			var tmpShape = colObj.shape();
			var childShape = compoundShape.getChildShape(i);
			colObj.internalSetTemporaryCollisionShape(childShape);
			childCollisionAlgorithms.add(ci.intersecter1.findAlgorithm(colObj, otherObj));
			colObj.internalSetTemporaryCollisionShape(tmpShape);
		}
	}

	@Override
	public void destroy() {
		var numChildren = childCollisionAlgorithms.size();
        for (var childCollisionAlgorithm : childCollisionAlgorithms) {


            Intersecter.freeCollisionAlgorithm(childCollisionAlgorithm);
        }
		childCollisionAlgorithms.clear();
	}

	@Override
	public void processCollision(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		var colObj = isSwapped ? body1 : body0;
		var otherObj = isSwapped ? body0 : body1;

		assert (colObj.shape().isCompound());
		var compoundShape = (CompoundShape) colObj.shape();


		var tmpTrans = new Transform();
		var orgTrans = new Transform();
		var childTrans = new Transform();
		var orgInterpolationTrans = new Transform();
		var newChildWorldTrans = new Transform();

		var numChildren = childCollisionAlgorithms.size();
        for (var i = 0; i < numChildren; i++) {

			var childShape = compoundShape.getChildShape(i);

			
			colObj.getWorldTransform(orgTrans);
			colObj.getInterpolationWorldTransform(orgInterpolationTrans);

			compoundShape.getChildTransform(i, childTrans);
			newChildWorldTrans.mul(orgTrans, childTrans);
			colObj.transform(newChildWorldTrans);
			colObj.setInterpolationWorldTransform(newChildWorldTrans);


			var tmpShape = colObj.shape();
			colObj.internalSetTemporaryCollisionShape(childShape);
            
            childCollisionAlgorithms.get(i).processCollision(colObj, otherObj, dispatchInfo, resultOut);
			
			colObj.internalSetTemporaryCollisionShape(tmpShape);
			colObj.transform(orgTrans);
			colObj.setInterpolationWorldTransform(orgInterpolationTrans);
		}
	}

	@Override
	public float calculateTimeOfImpact(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		var colObj = isSwapped ? body1 : body0;
		var otherObj = isSwapped ? body0 : body1;

		assert (colObj.shape().isCompound());

		var compoundShape = (CompoundShape) colObj.shape();


		var tmpTrans = new Transform();
		var orgTrans = new Transform();
		var childTrans = new Transform();
		var hitFraction = 1f;

		var numChildren = childCollisionAlgorithms.size();
        for (var i = 0; i < numChildren; i++) {

			var childShape = compoundShape.getChildShape(i);

			
			colObj.getWorldTransform(orgTrans);

			compoundShape.getChildTransform(i, childTrans);
			
			tmpTrans.set(orgTrans);
			tmpTrans.mul(childTrans);
			colObj.transform(tmpTrans);

			var tmpShape = colObj.shape();
			colObj.internalSetTemporaryCollisionShape(childShape);

			var frac = childCollisionAlgorithms.get(i).calculateTimeOfImpact(colObj, otherObj, dispatchInfo, resultOut);
			if (frac < hitFraction) {
				hitFraction = frac;
			}
			
			colObj.internalSetTemporaryCollisionShape(tmpShape);
			colObj.transform(orgTrans);
		}
		return hitFraction;
	}

	@Override
	public void getAllContactManifolds(OArrayList<PersistentManifold> manifoldArray) {
        for (var childCollisionAlgorithm : childCollisionAlgorithms) {

            childCollisionAlgorithm.getAllContactManifolds(manifoldArray);
        }
	}

	

	public static class CreateFunc extends CollisionAlgorithmCreateFunc {

		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1) {
			var algo = new CompoundCollisionAlgorithm();
			algo.init(ci, body0, body1, false);
			return algo;
		}

	}

	public static class SwappedCreateFunc extends CollisionAlgorithmCreateFunc {

		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1) {
			var algo = new CompoundCollisionAlgorithm();
			algo.init(ci, body0, body1, true);
			return algo;
		}

	}

}
