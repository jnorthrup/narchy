/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
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

package spacegraph.space3d.phys;

import org.jetbrains.annotations.Nullable;
import spacegraph.space3d.phys.collision.CollidableType;
import spacegraph.space3d.phys.collision.CollisionFlags;
import spacegraph.space3d.phys.collision.broad.Broadphasing;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.util.math.v3;

/**
 * CollisionObject can be used to manage collision detection objects.
 * It maintains all information that is needed for a collision detection: {@link CollisionShape},
 * {@link Transform} and {@link Broadphasing AABB proxy}. It can be added to {@link Collisions}.
 * 
 * @author jezek2
 */
public class Collidable<X> {

	//protected final BulletStack stack = BulletStack.get();

	// island management, m_activationState1
	public static final int ACTIVE_TAG = 1;
	public static final int ISLAND_SLEEPING = 2;
	public static final int WANTS_DEACTIVATION = 3;
	public static final int DISABLE_DEACTIVATION = 4;
	public static final int DISABLE_SIMULATION = 5;
	public final Transform transform;

	///m_interpolationWorldTransform is used for CCD and interpolation
	///it can be either previous or future (predicted) transform
	public final Transform interpolationWorldTransform = new Transform();
	//those two are experimental: just added for bullet time effect, so you can still apply impulses (directly modifying velocities) 
	//without destroying the continuous interpolated motion (which uses this interpolation velocities)
//	protected final v3 interpolationLinearVelocity = new v3();
//	protected final v3 interpolationAngularVelocity = new v3();

	/** broadphase handle */
	public Broadphasing broadphase;

	protected CollisionShape collisionShape;

	public short mask, group;

	// rootCollisionShape is temporarily used to store the original collision shape
	// The collisionShape might be temporarily replaced by a child collision shape during collision detection purposes
	// If it is null, the collisionShape is not temporarily replaced.
	//protected CollisionShape rootCollisionShape;

	protected int collisionFlags;
	protected int islandTag1;
	protected int companionId;
	protected int state;
	protected float deactivationTime;
	protected float friction;
	protected float restitution;

	///users can point to their objects, m_userPointer is not used by Bullet, see setUserPointer/getUserPointer
	protected X data;

	// internalType is reserved to distinguish Bullet's CollisionObject, RigidBody, SoftBody etc.
	// do not assign your own internalType unless you write a new dynamics object class.
	protected final CollidableType internalType;

	///time of impact calculation
	protected float hitFraction;
	///Swept sphere radius (0.0 by default), see btConvexConvexAlgorithm::
	protected float ccdSweptSphereRadius;

	/// Don't do continuous collision detection if the motion (in one step) is less then ccdMotionThreshold
	protected float ccdMotionThreshold;
	/// If some object should have elaborate collision filtering by sub-classes
	protected boolean checkCollideWith;



	public Collidable(CollidableType type, Transform t) {
		this.internalType = type;
		this.collisionFlags = CollisionFlags.STATIC_OBJECT;
		this.islandTag1 = -1;
		this.companionId = -1;
		this.state = 1;
		this.friction = 0.5f;
		this.hitFraction = 1f;
		this.transform = t;
	}

	public boolean checkCollideWithOverride(Collidable co) {
		return true;
	}

	public final boolean mergesSimulationIslands() {
		///static objects, kinematic and object without contact response don't merge islands
		return ((collisionFlags & (CollisionFlags.STATIC_OBJECT | CollisionFlags.KINEMATIC_OBJECT | CollisionFlags.NO_CONTACT_RESPONSE)) == 0);
	}

	public boolean isStaticObject() {
		return (collisionFlags & CollisionFlags.STATIC_OBJECT) != 0;
	}

	public boolean isKinematicObject() {
		return (collisionFlags & CollisionFlags.KINEMATIC_OBJECT) != 0;
	}

	public boolean isStaticOrKinematicObject() {
		return (collisionFlags & (CollisionFlags.KINEMATIC_OBJECT | CollisionFlags.STATIC_OBJECT)) != 0;
	}

	public boolean hasContactResponse() {
		return (collisionFlags & CollisionFlags.NO_CONTACT_RESPONSE) == 0;
	}

	public CollisionShape shape() {
		return collisionShape;
	}

	public void setCollisionShape(CollisionShape collisionShape) {
		this.collisionShape = collisionShape;
		//this.rootCollisionShape = collisionShape;
	}

//	public CollisionShape getRootCollisionShape() {
//		return rootCollisionShape;
//	}

	/**
	 * Avoid using this internal API call.
	 * internalSetTemporaryCollisionShape is used to temporary replace the actual collision shape by a child collision shape.
	 */
	public void internalSetTemporaryCollisionShape(CollisionShape collisionShape) {
		this.collisionShape = collisionShape;
	}

	public final int getActivationState() {
		return state;
	}

	public void setActivationState(int newState) {
		if ((state != DISABLE_DEACTIVATION) && (state != DISABLE_SIMULATION)) {
			this.state = newState;
		}
	}

	public float getDeactivationTime() {
		return deactivationTime;
	}

	public void setDeactivationTime(float deactivationTime) {
		this.deactivationTime = deactivationTime;
	}

	public void forceActivationState(int newState) {
		this.state = newState;
	}

	public void activate() {
		activate(false);
	}

	public void activate(boolean forceActivation) {
		if (forceActivation || (collisionFlags & (CollisionFlags.STATIC_OBJECT | CollisionFlags.KINEMATIC_OBJECT)) == 0) {
			setActivationState(ACTIVE_TAG);
			deactivationTime = 0f;
		}
	}

	public final boolean isActive() {
		int s = this.state;
		return ((s != ISLAND_SLEEPING) && (s != DISABLE_SIMULATION));
	}

	public float getRestitution() {
		return restitution;
	}

	public void setRestitution(float restitution) {
		this.restitution = restitution;
	}

	public float getFriction() {
		return friction;
	}

	public void setFriction(float friction) {
		this.friction = friction;
	}

	// reserved for Bullet internal usage
	public CollidableType getInternalType() {
		return internalType;
	}

	public Transform getWorldTransform(Transform out) {
		out.set(transform);
		return out;
	}

	public void transform(Transform transform) {
		this.transform.set(transform);
	}

	public void broadphase(@Nullable Broadphasing broadphaseHandle) {
		if ((broadphaseHandle!=null && this.broadphase !=null) || (broadphaseHandle==null && this.broadphase ==null))
			throw new RuntimeException();
		this.broadphase = broadphaseHandle;
	}

	public final Transform getInterpolationWorldTransform(Transform out) {
		out.set(interpolationWorldTransform);
		return out;
	}

	public void setInterpolationWorldTransform(Transform interpolationWorldTransform) {
		this.interpolationWorldTransform.set(interpolationWorldTransform);
	}

//	public void setInterpolationLinearVelocity(v3 linvel) {
//		interpolationLinearVelocity.set(linvel);
//	}
//
//	public void setInterpolationAngularVelocity(v3 angvel) {
//		interpolationAngularVelocity.set(angvel);
//	}
//


	public final int tag() {
		return islandTag1;
	}

	public void setIslandTag(int islandTag) {
		this.islandTag1 = islandTag;
	}

	public final int getCompanionId() {
		return companionId;
	}

	public void setCompanionId(int companionId) {
		this.companionId = companionId;
	}

	public float getHitFraction() {
		return hitFraction;
	}

	public void setHitFraction(float hitFraction) {
		this.hitFraction = hitFraction;
	}

	public int getCollisionFlags() {
		return collisionFlags;
	}

	public void setCollisionFlags(int collisionFlags) {
		this.collisionFlags = collisionFlags;
	}

	// Swept sphere radius (0.0 by default), see btConvexConvexAlgorithm::
	public float getCcdSweptSphereRadius() {
		return ccdSweptSphereRadius;
	}

	// Swept sphere radius (0.0 by default), see btConvexConvexAlgorithm::
	public void setCcdSweptSphereRadius(float ccdSweptSphereRadius) {
		this.ccdSweptSphereRadius = ccdSweptSphereRadius;
	}

	public float getCcdMotionThreshold() {
		return ccdMotionThreshold;
	}

	public float getCcdSquareMotionThreshold() {
		return ccdMotionThreshold * ccdMotionThreshold;
	}

	// Don't do continuous collision detection if the motion (in one step) is less then ccdMotionThreshold
	public void setCcdMotionThreshold(float ccdMotionThreshold) {
		// JAVA NOTE: fixed bug with usage of ccdMotionThreshold*ccdMotionThreshold
		this.ccdMotionThreshold = ccdMotionThreshold;
	}

	public X data() {
		return data;
	}

	public void setData(X x) {
		this.data = x;
	}

	public boolean checkCollideWith(Collidable co) {
		if (checkCollideWith) {
			return checkCollideWithOverride(co);
		}

		return true;
	}

	public v3 getWorldOrigin() {
		return transform;
	}

	@Override
	public String toString() {
		return "CollisionObject{" +
				collisionShape +
				" @ " + transform +
				'}';
	}

	public void setGravity(v3 gravity) {

	}
}
