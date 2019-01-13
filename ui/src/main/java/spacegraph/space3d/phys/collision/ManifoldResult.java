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
import spacegraph.space3d.phys.collision.narrow.DiscreteCollisionDetectorInterface;
import spacegraph.space3d.phys.collision.narrow.ManifoldPoint;
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.math.Transform;

/**
 * ManifoldResult is helper class to manage contact results.
 * 
 * @author jezek2
 */
public class ManifoldResult extends DiscreteCollisionDetectorInterface.Result {
	
	private PersistentManifold manifoldPtr;

	
	private final Transform rootTransA = new Transform();
	private final Transform rootTransB = new Transform();
	private Collidable body0;
	private Collidable body1;
	private int partId0;
	private int partId1;
	private int index0;
	private int index1;

	public ManifoldResult() {
	}

	public ManifoldResult(Collidable body0, Collidable body1) {
		init(body0, body1);
	}

	public void init(Collidable body0, Collidable body1) {
		this.body0 = body0;
		this.body1 = body1;
		body0.getWorldTransform(this.rootTransA);
		body1.getWorldTransform(this.rootTransB);
	}

	public PersistentManifold getPersistentManifold() {
		return manifoldPtr;
	}

	public void setPersistentManifold(PersistentManifold manifoldPtr) {
		this.manifoldPtr = manifoldPtr;
	}

	@Override
    public void setShapeIdentifiers(int partId0, int index0, int partId1, int index1) {
		this.partId0 = partId0;
		this.partId1 = partId1;
		this.index0 = index0;
		this.index1 = index1;
	}

	@Override
    public void addContactPoint(v3 normalOnBInWorld, v3 pointInWorld, float depth, float breakingThresh) {
		assert (manifoldPtr != null);
		

		if (depth > breakingThresh) {
			return;
		}

		boolean isSwapped = manifoldPtr.getBody0() != body0;

		v3 pointA = new v3();
		pointA.scaleAdd(depth, normalOnBInWorld, pointInWorld);

		v3 localA = new v3();
		v3 localB = new v3();

		if (isSwapped) {
			rootTransB.invXform(pointA, localA);
			rootTransA.invXform(pointInWorld, localB);
		}
		else {
			rootTransA.invXform(pointA, localA);
			rootTransB.invXform(pointInWorld, localB);
		}

		ManifoldPoint newPt =new ManifoldPoint();
		newPt.init(localA, localB, normalOnBInWorld, depth);

		newPt.positionWorldOnA.set(pointA);
		newPt.positionWorldOnB.set(pointInWorld);

		int insertIndex = manifoldPtr.getCacheEntry(newPt, manifoldPtr.getContactBreakingThreshold());

		newPt.combinedFriction = calculateCombinedFriction(body0, body1);
		newPt.combinedRestitution = calculateCombinedRestitution(body0, body1);

		
		newPt.partId0 = partId0;
		newPt.partId1 = partId1;
		newPt.index0 = index0;
		newPt.index1 = index1;

		
		if (insertIndex >= 0) {
			
			manifoldPtr.replaceContactPoint(newPt, insertIndex);
		}
		else {
			insertIndex = manifoldPtr.addManifoldPoint(newPt);
		}

		
		if (manifoldPtr.globals.getContactAddedCallback() != null &&
				
				((body0.getCollisionFlags() & CollisionFlags.CUSTOM_MATERIAL_CALLBACK) != 0 ||
				(body1.getCollisionFlags() & CollisionFlags.CUSTOM_MATERIAL_CALLBACK) != 0)) {
			
			Collidable obj0 = isSwapped ? body1 : body0;
			Collidable obj1 = isSwapped ? body0 : body1;
			manifoldPtr.globals.getContactAddedCallback().contactAdded(manifoldPtr.getContactPoint(insertIndex), obj0, partId0, index0, obj1, partId1, index1);
		}

	}

	
	private static float calculateCombinedFriction(Collidable body0, Collidable body1) {
		float friction = body0.getFriction() * body1.getFriction();

		float MAX_FRICTION = 10f;
		if (friction < -MAX_FRICTION) {
			friction = -MAX_FRICTION;
		}
		if (friction > MAX_FRICTION) {
			friction = MAX_FRICTION;
		}
		return friction;
	}

	private static float calculateCombinedRestitution(Collidable body0, Collidable body1) {
		return body0.getRestitution() * body1.getRestitution();
	}

	public void refreshContactPoints() {
		assert (manifoldPtr != null);
		if (manifoldPtr.numContacts() == 0) {
			return;
		}

		boolean isSwapped = manifoldPtr.getBody0() != body0;

		if (isSwapped) {
			manifoldPtr.refreshContactPoints(rootTransB, rootTransA);
		}
		else {
			manifoldPtr.refreshContactPoints(rootTransA, rootTransB);
		}
	}
}
