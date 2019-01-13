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
import spacegraph.space3d.phys.Collisions;
import spacegraph.space3d.phys.collision.broad.Broadphasing;
import spacegraph.space3d.phys.collision.broad.Intersecter;
import spacegraph.space3d.phys.collision.narrow.VoronoiSimplexSolver;
import spacegraph.space3d.phys.math.AabbUtil2;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.TransformUtil;
import spacegraph.space3d.phys.shape.ConvexShape;
import spacegraph.space3d.phys.util.OArrayList;
import spacegraph.util.math.Quat4f;

import java.util.Collection;

/**
 * GhostObject can keep track of all objects that are overlapping. By default, this
 * overlap is based on the AABB. This is useful for creating a character controller,
 * collision sensors/triggers, explosions etc.
 *
 * @author tomrbryn
 */
public class GhostObject extends Collidable {

	final OArrayList<Collidable> overlappingObjects = new OArrayList<>();

	GhostObject() {
		super(CollidableType.GHOST_OBJECT, new Transform());
	}

	/**
	 * This method is mainly for expert/internal use only.
	 */
	public void addOverlappingObjectInternal(Broadphasing otherProxy, Broadphasing thisProxy) {
		Collidable otherObject = otherProxy.data;
		assert(otherObject != null);

		
		int index = overlappingObjects.indexOf(otherObject);
		if (index == -1) {
			
			overlappingObjects.add(otherObject);
		}
	}

	/**
	 * This method is mainly for expert/internal use only.
	 */
	public void removeOverlappingObjectInternal(Broadphasing otherProxy, Intersecter intersecter, Broadphasing thisProxy) {
		Collidable otherObject = otherProxy.data;
		assert(otherObject != null);

		OArrayList<Collidable> o = this.overlappingObjects;
		int index = o.indexOf(otherObject);
		if (index != -1) {
            
			int num = o.size();
			o.setFast(index, o.get(num - 1));
			o.removeFast(num -1);
		}
	}

	public void convexSweepTest(ConvexShape castShape, Transform convexFromWorld, Transform convexToWorld, Collisions.ConvexResultCallback resultCallback, float allowedCcdPenetration) {
		Transform convexFromTrans = new Transform();
		Transform convexToTrans = new Transform();

		convexFromTrans.set(convexFromWorld);
		convexToTrans.set(convexToWorld);

		v3 castShapeAabbMin = new v3();
		v3 castShapeAabbMax = new v3();

		
        v3 linVel = new v3();
        v3 angVel = new v3();
        TransformUtil.calculateVelocity(convexFromTrans, convexToTrans, 1f, linVel, angVel);
        Transform R = new Transform();
        R.setIdentity();
        R.setRotation(convexFromTrans.getRotation(new Quat4f()));
        castShape.calculateTemporalAabb(R, linVel, angVel, 1f, castShapeAabbMin, castShapeAabbMax);

        Transform tmpTrans = new Transform();


        for (Collidable collidable : overlappingObjects) {

            if (resultCallback.needsCollision(collidable.broadphase)) {

                v3 collisionObjectAabbMin = new v3();
                v3 collisionObjectAabbMax = new v3();
                collidable.shape().getAabb(collidable.getWorldTransform(tmpTrans), collisionObjectAabbMin, collisionObjectAabbMax);
                AabbUtil2.aabbExpand(collisionObjectAabbMin, collisionObjectAabbMax, castShapeAabbMin, castShapeAabbMax);
                float[] hitLambda = {1f};
                v3 hitNormal = new v3();
                if (AabbUtil2.rayAabb(convexFromWorld, convexToWorld, collisionObjectAabbMin, collisionObjectAabbMax, hitLambda, hitNormal)) {
                    Collisions.objectQuerySingle(castShape, convexFromTrans, convexToTrans,
                            collidable,
                            collidable.shape(),
                            collidable.getWorldTransform(tmpTrans),
                            resultCallback,
                            allowedCcdPenetration);
                }
            }
        }
	}

	public void rayTest(v3 rayFromWorld, v3 rayToWorld, Collisions.RayResultCallback resultCallback) {
		Transform rayFromTrans = new Transform();
		rayFromTrans.setIdentity();
		rayFromTrans.set(rayFromWorld);
		Transform rayToTrans = new Transform();
		rayToTrans.setIdentity();
		rayToTrans.set(rayToWorld);

		Transform tmpTrans = new Transform();

		VoronoiSimplexSolver solver = new VoronoiSimplexSolver();

        for (Collidable collidable : overlappingObjects) {

            if (resultCallback.needsCollision(collidable.broadphase)) {
                Collisions.rayTestSingle(rayFromTrans, rayToTrans,
                        collidable,
                        collidable.shape(),
                        collidable.getWorldTransform(tmpTrans),
                        solver,
                        resultCallback);
            }
        }
	}

	public int getNumOverlappingObjects() {
		return overlappingObjects.size();
	}

	public Collidable getOverlappingObject(int index) {
        return overlappingObjects.get(index);
        
    }

	public Collection<Collidable> getOverlappingPairs() {
		return overlappingObjects;
	}

	
	
	

	public static GhostObject upcast(Collidable colObj) {
		if (colObj.getInternalType() == CollidableType.GHOST_OBJECT) {
			return (GhostObject)colObj;
		}
		
		return null;
	}
	
}
