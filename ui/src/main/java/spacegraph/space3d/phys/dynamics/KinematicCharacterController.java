///*
// * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
// *
// * Bullet Continuous Collision Detection and Physics Library
// * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
// *
// * This software is provided 'as-is', without any express or implied warranty.
// * In no event will the authors be held liable for any damages arising from
// * the use of this software.
// *
// * Permission is granted to anyone to use this software for any purpose,
// * including commercial applications, and to alter it and redistribute it
// * freely, subject to the following restrictions:
// *
// * 1. The origin of this software must not be misrepresented; you must not
// *    claim that you wrote the original software. If you use this software
// *    in a product, an acknowledgment in the product documentation would be
// *    appreciated but is not required.
// * 2. Altered source versions must be plainly marked as such, and must not be
// *    misrepresented as being the original software.
// * 3. This notice may not be removed or altered from any source distribution.
// */
//
//package spacegraph.phys.dynamics;
//
//import spacegraph.math.v3;
//import spacegraph.phys.BulletGlobals;
//import spacegraph.phys.Collidable;
//import spacegraph.phys.Collisions;
//import spacegraph.phys.collision.ClosestRay;
//import spacegraph.phys.collision.PairCachingGhostObject;
//import spacegraph.phys.collision.broad.BroadphasePair;
//import spacegraph.phys.collision.narrow.ManifoldPoint;
//import spacegraph.phys.collision.narrow.PersistentManifold;
//import spacegraph.phys.math.IDebugDraw;
//import spacegraph.phys.math.Transform;
//import spacegraph.phys.shape.ConvexShape;
//import spacegraph.phys.util.OArrayList;
//
///**
// * KinematicCharacterController is an object that supports a sliding motion in
// * a world. It uses a {@link GhostObject} and convex sweep test to test for upcoming
// * collisions. This is combined with discrete collision detection to recover
// * from penetrations.<p>
// *
// * Interaction between KinematicCharacterController and dynamic rigid bodies
// * needs to be explicity implemented by the user.
// *
// * @author tomrbryn
// */
//public class KinematicCharacterController extends ActionInterface {
//
//	private static final v3[] upAxisDirection = {
//		new v3(1.0f, 0.0f, 0.0f),
//		new v3(0.0f, 1.0f, 0.0f),
//		new v3(0.0f, 0.0f, 1.0f),
//	};
//
//	protected float halfHeight;
//
//	protected PairCachingGhostObject ghostObject;
//
//	// is also in ghostObject, but it needs to be convex, so we store it here
//	// to avoid upcast
//	protected ConvexShape convexShape;
//
//	protected float verticalVelocity;
//	protected float verticalOffset;
//
//	protected float fallSpeed;
//	protected float jumpSpeed;
//	protected float maxJumpHeight;
//
//	protected float maxSlopeRadians; // Slope angle that is set (used for returning the exact value)
//	protected float maxSlopeCosine; // Cosine equivalent of m_maxSlopeRadians (calculated once when set, for optimization)
//
//	protected float gravity;
//
//	protected float turnAngle;
//
//	protected float stepHeight;
//
//	protected float addedMargin; // @todo: remove this and fix the code
//
//	// this is the desired walk direction, set by the user
//	protected v3 walkDirection = new v3();
//	protected v3 normalizedDirection = new v3();
//
//	// some internal variables
//	protected v3 currentPosition = new v3();
//	protected float currentStepOffset;
//	protected v3 targetPosition = new v3();
//
//	// keep track of the contact manifolds
//	OArrayList<PersistentManifold> manifoldArray = new OArrayList<>();
//
//	protected boolean touchingContact;
//	protected v3 touchingNormal = new v3();
//
//	protected boolean wasOnGround;
//	protected boolean wasJumping;
//
//	protected boolean useGhostObjectSweepTest;
//	protected boolean useWalkDirection;
//	protected float velocityTimeInterval;
//	protected int upAxis;
//
//	protected Collidable me;
//
//	public KinematicCharacterController(PairCachingGhostObject ghostObject, ConvexShape convexShape, float stepHeight) {
//		this(ghostObject, convexShape, stepHeight, 1);
//	}
//
//	public KinematicCharacterController(PairCachingGhostObject ghostObject, ConvexShape convexShape, float stepHeight, int upAxis) {
//		this.upAxis = upAxis;
//		this.addedMargin = 0.02f;
//		this.walkDirection.set(0, 0, 0);
//		this.useGhostObjectSweepTest = true;
//		this.ghostObject = ghostObject;
//		this.stepHeight = stepHeight;
//		this.turnAngle = 0.0f;
//		this.convexShape = convexShape;
//		this.useWalkDirection = true;
//		this.velocityTimeInterval = 0.0f;
//		this.verticalVelocity = 0.0f;
//		this.verticalOffset = 0.0f;
//		this.gravity = 9.8f; // 1G acceleration
//		this.fallSpeed = 55.0f; // Terminal velocity of a sky diver in m/s.
//		this.jumpSpeed = 10.0f; // ?
//		this.wasOnGround = false;
//		setMaxSlope((float)((50.0f/180.0f) * Math.PI));
//	}
//
//	private PairCachingGhostObject getGhostObject() {
//		return ghostObject;
//	}
//
//	// ActionInterface interface
//	@Override
//    public void updateAction(Collisions collisions, float deltaTime) {
//		preStep(collisions);
//		playerStep(collisions, deltaTime);
//	}
//
//	// ActionInterface interface
//	@Override
//    public void debugDraw(IDebugDraw debugDrawer) {
//	}
//
//	public void setUpAxis(int axis) {
//		if (axis < 0) {
//			axis = 0;
//		}
//		if (axis > 2) {
//			axis = 2;
//		}
//		upAxis = axis;
//	}
//
//	/**
//	 * This should probably be called setPositionIncrementPerSimulatorStep. This
//	 * is neither a direction nor a velocity, but the amount to increment the
//	 * position each simulation iteration, regardless of dt.<p>
//	 *
//	 * This call will reset any velocity set by {@link #setVelocityForTimeInterval}.
//	 */
//	public void	setWalkDirection(v3 walkDirection) {
//		useWalkDirection = true;
//		this.walkDirection.set(walkDirection);
//		normalizedDirection.set(getNormalizedVector(walkDirection, new v3()));
//	}
//
//	/**
//	 * Caller provides a velocity with which the character should move for the
//	 * given time period. After the time period, velocity is reset to zero.
//	 * This call will reset any walk direction set by {@link #setWalkDirection}.
//	 * Negative time intervals will result in no motion.
//	 */
//	public void setVelocityForTimeInterval(v3 velocity, float timeInterval) {
//		useWalkDirection = false;
//		walkDirection.set(velocity);
//		normalizedDirection.set(getNormalizedVector(walkDirection, new v3()));
//		velocityTimeInterval = timeInterval;
//	}
//
//	public void reset() {
//	}
//
//	public void warp(v3 origin) {
//		Transform xform = new Transform();
//		xform.setIdentity();
//		xform.set(origin);
//		ghostObject.setWorldTransform(xform);
//	}
//
//	public void preStep(Collisions collisions) {
//		int numPenetrationLoops = 0;
//		touchingContact = false;
//		while (recoverFromPenetration(collisions)) {
//			numPenetrationLoops++;
//			touchingContact = true;
//			if (numPenetrationLoops > 4) {
//				//printf("character could not recover from penetration = %d\n", numPenetrationLoops);
//				break;
//			}
//		}
//
//		currentPosition.set(ghostObject.getWorldTransform(new Transform()));
//		targetPosition.set(currentPosition);
//		//printf("m_targetPosition=%f,%f,%f\n",m_targetPosition[0],m_targetPosition[1],m_targetPosition[2]);
//	}
//
//	public void playerStep(Collisions collisions, float dt) {
//		//printf("playerStep(): ");
//		//printf("  dt = %f", dt);
//
//		// quick check...
//		if (!useWalkDirection && velocityTimeInterval <= 0.0f) {
//			//printf("\n");
//			return; // no motion
//		}
//
//		wasOnGround = onGround();
//
//		// Update fall velocity.
//		verticalVelocity -= gravity * dt;
//		if(verticalVelocity > 0.0 && verticalVelocity > jumpSpeed)
//		{
//			verticalVelocity = jumpSpeed;
//		}
//		if(verticalVelocity < 0.0 && Math.abs(verticalVelocity) > Math.abs(fallSpeed))
//		{
//			verticalVelocity = -Math.abs(fallSpeed);
//		}
//		verticalOffset = verticalVelocity * dt;
//
//		Transform xform = ghostObject.getWorldTransform(new Transform());
//
//		//printf("walkDirection(%f,%f,%f)\n",walkDirection[0],walkDirection[1],walkDirection[2]);
//		//printf("walkSpeed=%f\n",walkSpeed);
//
//		stepUp(collisions);
//		if (useWalkDirection) {
//			//System.out.println("playerStep 3");
//			stepForwardAndStrafe(collisions, walkDirection);
//		}
//		else {
//			System.out.println("playerStep 4");
//			//printf("  time: %f", m_velocityTimeInterval);
//
//			// still have some time left for moving!
//			float dtMoving = (dt < velocityTimeInterval) ? dt : velocityTimeInterval;
//			velocityTimeInterval -= dt;
//
//			// how far will we move while we are moving?
//			v3 move = new v3();
//			move.scale(dtMoving, walkDirection);
//
//			//printf("  dtMoving: %f", dtMoving);
//
//			// okay, step
//			stepForwardAndStrafe(collisions, move);
//		}
//		stepDown(collisions, dt);
//
//		//printf("\n");
//
//		xform.set(currentPosition);
//		ghostObject.setWorldTransform(xform);
//	}
//
//	public void setFallSpeed(float fallSpeed) {
//		this.fallSpeed = fallSpeed;
//	}
//
//	public void setJumpSpeed(float jumpSpeed) {
//		this.jumpSpeed = jumpSpeed;
//	}
//
//	public void setMaxJumpHeight(float maxJumpHeight) {
//		this.maxJumpHeight = maxJumpHeight;
//	}
//
//	public boolean canJump() {
//		return onGround();
//	}
//
//	public void jump() {
//		if (!canJump()) return;
//
//		verticalVelocity = jumpSpeed;
//                wasJumping = true;
//
//		//#if 0
//		//currently no jumping.
//		//btTransform xform;
//		//m_rigidBody->getMotionState()->getWorldTransform (xform);
//		//btVector3 up = xform.getBasis()[1];
//		//up.normalize ();
//		//btScalar magnitude = (btScalar(1.0)/m_rigidBody->getInvMass()) * btScalar(8.0);
//		//m_rigidBody->applyCentralImpulse (up * magnitude);
//		//#endif
//	}
//
//	public void setGravity(float gravity) {
//		this.gravity = gravity;
//	}
//
//	public float getGravity() {
//		return gravity;
//	}
//
//	public void setMaxSlope(float slopeRadians) {
//		maxSlopeRadians = slopeRadians;
//		maxSlopeCosine = (float) Math.cos(slopeRadians);
//	}
//
//	public float getMaxSlope() {
//		return maxSlopeRadians;
//	}
//
//	public boolean onGround() {
//		return verticalVelocity == 0.0f && verticalOffset == 0.0f;
//	}
//
//	// static helper method
//	private static v3 getNormalizedVector(v3 v, v3 out) {
//		out.set(v);
//		out.normalize();
//		if (out.length() < BulletGlobals.SIMD_EPSILON) {
//			out.set(0, 0, 0);
//		}
//		return out;
//	}
//
//	/**
//	 * Returns the reflection direction of a ray going 'direction' hitting a surface
//	 * with normal 'normal'.<p>
//	 *
//	 * From: http://www-cs-students.stanford.edu/~adityagp/final/node3.html
//	 */
//	protected static v3 computeReflectionDirection(v3 direction, v3 normal, v3 out) {
//		// return direction - (btScalar(2.0) * direction.dot(normal)) * normal;
//		out.set(normal);
//		out.scale(-2.0f * direction.dot(normal));
//		out.add(direction);
//		return out;
//	}
//
//	/**
//	 * Returns the portion of 'direction' that is parallel to 'normal'
//	 */
//	protected static v3 parallelComponent(v3 direction, v3 normal, v3 out) {
//		//btScalar magnitude = direction.dot(normal);
//		//return normal * magnitude;
//		out.set(normal);
//		out.scale(direction.dot(normal));
//		return out;
//	}
//
//	/**
//	 * Returns the portion of 'direction' that is perpindicular to 'normal'
//	 */
//	protected static v3 perpindicularComponent(v3 direction, v3 normal, v3 out) {
//		//return direction - parallelComponent(direction, normal);
//		v3 perpendicular = parallelComponent(direction, normal, out);
//		perpendicular.scale(-1);
//		perpendicular.add(direction);
//		return perpendicular;
//	}
//
//	protected boolean recoverFromPenetration(Collisions collisions) {
//		boolean penetration = false;
//
//        collisions.intersecter.dispatchAllCollisionPairs(
//				ghostObject.getOverlappingPairCache(), collisions.getDispatchInfo(), collisions.intersecter);
//
//		currentPosition.set(ghostObject.getWorldTransform(new Transform()));
//
//		float maxPen = 0.0f;
//		for (int i = 0; i<ghostObject.getOverlappingPairCache().size(); i++) {
//			manifoldArray.clear();
//
//			//return array[index];
//			BroadphasePair collisionPair = ghostObject.getOverlappingPairCache().getOverlappingPairArray().get(i);
//                        //XXX: added no contact response
//                        if (!collisionPair.pProxy0.data.hasContactResponse()
//                                 || !collisionPair.pProxy1.data.hasContactResponse())
//                                 continue;
//			if (collisionPair.algorithm != null) {
//				collisionPair.algorithm.getAllContactManifolds(manifoldArray);
//			}
//
//			for (int j=0; j<manifoldArray.size(); j++) {
//				//return array[index];
//				PersistentManifold manifold = manifoldArray.get(j);
//				float directionSign = manifold.getBody0() == ghostObject? -1.0f : 1.0f;
//				for (int p=0; p<manifold.getNumContacts(); p++) {
//					ManifoldPoint pt = manifold.getContactPoint(p);
//
//					float dist = pt.distance1;
//					if (dist < 0.0f) {
//						if (dist < maxPen) {
//							maxPen = dist;
//							touchingNormal.set(pt.normalWorldOnB);//??
//							touchingNormal.scale(directionSign);
//						}
//
//						currentPosition.scaleAdd(directionSign * dist * 0.2f, pt.normalWorldOnB, currentPosition);
//
//						penetration = true;
//					}
//					else {
//						//printf("touching %f\n", dist);
//					}
//				}
//
//				//manifold->clearManifold();
//			}
//		}
//
//		Transform newTrans = ghostObject.getWorldTransform(new Transform());
//		newTrans.set(currentPosition);
//		ghostObject.setWorldTransform(newTrans);
//		//printf("m_touchingNormal = %f,%f,%f\n",m_touchingNormal[0],m_touchingNormal[1],m_touchingNormal[2]);
//
//		//System.out.println("recoverFromPenetration "+penetration+" "+touchingNormal);
//
//		return penetration;
//	}
//
//	protected void stepUp(Collisions world) {
//		// phase 1: up
//		Transform start = new Transform();
//		Transform end = new Transform();
//		targetPosition.scaleAdd(stepHeight + (verticalOffset > 0.0?verticalOffset:0.0f), upAxisDirection[upAxis], currentPosition);
//
//		start.setIdentity ();
//		end.setIdentity ();
//
//		/* FIXME: Handle penetration properly */
//		start.scaleAdd(convexShape.getMargin() + addedMargin, upAxisDirection[upAxis], currentPosition);
//		end.set(targetPosition);
//
//		// Find only sloped/flat surface hits, avoid wall and ceiling hits...
//		v3 up = new v3();
//		up.scale(-1f, upAxisDirection[upAxis]);
//		KinematicClosestNotMeConvexResultCallback callback = new KinematicClosestNotMeConvexResultCallback(ghostObject, up, 0.7071f);
//        callback.collisionFilterGroup = ghostObject.broadphase.collisionFilterGroup;
//        callback.collisionFilterMask = ghostObject.broadphase.collisionFilterMask;
//
//		if (useGhostObjectSweepTest) {
//			ghostObject.convexSweepTest(convexShape, start, end, callback, world.getDispatchInfo().allowedCcdPenetration);
//		}
//		else {
//			world.convexSweepTest(convexShape, start, end, callback);
//		}
//
//		if (callback.hasHit()) {
//                    // Only modify the position if the hit was a slope and not a wall or ceiling.
//                    if(callback.hitNormalWorld.dot(upAxisDirection[upAxis]) > 0.0){
//			// we moved up only a fraction of the step height
//			currentStepOffset = stepHeight * callback.closestHitFraction;
//			currentPosition.interpolate(currentPosition, targetPosition, callback.closestHitFraction);
//			verticalVelocity = 0.0f;
//			verticalOffset = 0.0f;
//                    }
//		}
//		else {
//			currentStepOffset = stepHeight;
//			currentPosition.set(targetPosition);
//		}
//	}
//
//	protected void updateTargetPositionBasedOnCollision (v3 hitNormal) {
//		updateTargetPositionBasedOnCollision(hitNormal, 0f, 1f);
//	}
//
//	protected void updateTargetPositionBasedOnCollision(v3 hitNormal, float tangentMag, float normalMag) {
//		v3 movementDirection = new v3();
//		movementDirection.sub(targetPosition, currentPosition);
//		float movementLength = movementDirection.length();
//		if (movementLength> BulletGlobals.SIMD_EPSILON) {
//			movementDirection.normalize();
//
//			v3 reflectDir = computeReflectionDirection(movementDirection, hitNormal, new v3());
//			reflectDir.normalize();
//
//			v3 parallelDir = parallelComponent(reflectDir, hitNormal, new v3());
//			v3 perpindicularDir = perpindicularComponent(reflectDir, hitNormal, new v3());
//
//			targetPosition.set(currentPosition);
//			if (false) //tangentMag != 0.0)
//			{
//				v3 parComponent = new v3();
//				parComponent.scale(tangentMag * movementLength, parallelDir);
//				//printf("parComponent=%f,%f,%f\n",parComponent[0],parComponent[1],parComponent[2]);
//				targetPosition.add(parComponent);
//			}
//
//			if (normalMag != 0.0f) {
//				v3 perpComponent = new v3();
//				perpComponent.scale(normalMag * movementLength, perpindicularDir);
//				//printf("perpComponent=%f,%f,%f\n",perpComponent[0],perpComponent[1],perpComponent[2]);
//				targetPosition.add(perpComponent);
//			}
//		}
//		else {
//			//printf("movementLength don't normalize a zero vector\n");
//		}
//	}
//
//	protected void stepForwardAndStrafe(Collisions collisions, v3 walkMove) {
//		// printf("m_normalizedDirection=%f,%f,%f\n",
//		// 	m_normalizedDirection[0],m_normalizedDirection[1],m_normalizedDirection[2]);
//		// phase 2: forward and strafe
//		Transform start = new Transform();
//		Transform end = new Transform();
//		targetPosition.add(currentPosition, walkMove);
//		start.setIdentity ();
//		end.setIdentity ();
//
//		float fraction = 1.0f;
//		v3 distance2Vec = new v3();
//		distance2Vec.sub(currentPosition, targetPosition);
//		float distance2 = distance2Vec.lengthSquared();
//		//printf("distance2=%f\n",distance2);
//
//		if (touchingContact) {
//			if (normalizedDirection.dot(touchingNormal) > 0.0f) {
//				updateTargetPositionBasedOnCollision(touchingNormal);
//			}
//		}
//
//		int maxIter = 10;
//
//		while (fraction > 0.01f && maxIter-- > 0) {
//			start.set(currentPosition);
//			end.set(targetPosition);
//                        v3 sweepDirNegative = new v3();
//                        sweepDirNegative.sub(currentPosition, targetPosition);
//
//			KinematicClosestNotMeConvexResultCallback callback = new KinematicClosestNotMeConvexResultCallback(ghostObject, sweepDirNegative, -1.0f);
//
//            callback.collisionFilterGroup = ghostObject.broadphase.collisionFilterGroup;
//            callback.collisionFilterMask = ghostObject.broadphase.collisionFilterMask;
//
//			float margin = convexShape.getMargin();
//			convexShape.setMargin(margin + addedMargin);
//
//			if (useGhostObjectSweepTest) {
//				ghostObject.convexSweepTest(convexShape, start, end, callback, collisions.getDispatchInfo().allowedCcdPenetration);
//			}
//			else {
//				collisions.convexSweepTest(convexShape, start, end, callback);
//			}
//
//			convexShape.setMargin(margin);
//
//			fraction -= callback.closestHitFraction;
//
//			if (callback.hasHit()) {
//				// we moved only a fraction
//				v3 hitDistanceVec = new v3();
//				hitDistanceVec.sub(callback.hitPointWorld, currentPosition);
////				float hitDistance = hitDistanceVec.length();
//
//				// if the distance is farther than the collision margin, move
//				//if (hitDistance > addedMargin) {
//				//	//printf("callback.m_closestHitFraction=%f\n",callback.m_closestHitFraction);
//				//	currentPosition.interpolate(currentPosition, targetPosition, callback.closestHitFraction);
//				//}
//
//				updateTargetPositionBasedOnCollision(callback.hitNormalWorld);
//
//				v3 currentDir = new v3();
//				currentDir.sub(targetPosition, currentPosition);
//				distance2 = currentDir.lengthSquared();
//				if (distance2 > BulletGlobals.SIMD_EPSILON) {
//					currentDir.normalize();
//					// see Quake2: "If velocity is against original velocity, stop ead to avoid tiny oscilations in sloping corners."
//					if (currentDir.dot(normalizedDirection) <= 0.0f) {
//						break;
//					}
//				}
//				else {
//					//printf("currentDir: don't normalize a zero vector\n");
//					break;
//				}
//			}
//			else {
//				// we moved whole way
//				currentPosition.set(targetPosition);
//			}
//
//			//if (callback.m_closestHitFraction == 0.f)
//			//    break;
//		}
//	}
//
//	protected void stepDown(Collisions collisions, float dt) {
//		Transform start = new Transform();
//		Transform end = new Transform();
//
//		// phase 3: down
////		float additionalDownStep = (wasOnGround /*&& !onGround()*/) ? stepHeight : 0.0f;
////		Vector3f step_drop = new Vector3f();
////		step_drop.scale(currentStepOffset + additionalDownStep, upAxisDirection[upAxis]);
////		float downVelocity = (additionalDownStep == 0.0f && verticalVelocity<0.0f?-verticalVelocity:0.0f) * dt;
////		Vector3f gravity_drop = new Vector3f();
////		gravity_drop.scale(downVelocity, upAxisDirection[upAxis]);
////		targetPosition.sub(step_drop);
////		targetPosition.sub(gravity_drop);
//
//                float downVelocity = (verticalVelocity<0.0f?-verticalVelocity:0.0f) * dt;
//                if(downVelocity > 0.0 && downVelocity < stepHeight
//			&& (wasOnGround || !wasJumping))
//		{
//			downVelocity = stepHeight;
//		}
//                v3 step_drop = new v3();
//                step_drop.scale(currentStepOffset + downVelocity, upAxisDirection[upAxis]);
//		targetPosition.sub(step_drop);
//
//		start.setIdentity ();
//		end.setIdentity ();
//
//		start.set(currentPosition);
//		end.set(targetPosition);
//
//		KinematicClosestNotMeConvexResultCallback callback = new KinematicClosestNotMeConvexResultCallback(ghostObject, upAxisDirection[upAxis], maxSlopeCosine);
//        callback.collisionFilterGroup = ghostObject.broadphase.collisionFilterGroup;
//        callback.collisionFilterMask = ghostObject.broadphase.collisionFilterMask;
//
//		if (useGhostObjectSweepTest) {
//			ghostObject.convexSweepTest(convexShape, start, end, callback, collisions.getDispatchInfo().allowedCcdPenetration);
//		}
//		else {
//			collisions.convexSweepTest(convexShape, start, end, callback);
//		}
//
//		if (callback.hasHit()) {
//			// we dropped a fraction of the height -> hit floor
//			currentPosition.interpolate(currentPosition, targetPosition, callback.closestHitFraction);
//			verticalVelocity = 0.0f;
//			verticalOffset = 0.0f;
//                        wasJumping = false;
//		}
//		else {
//			// we dropped the full height
//			currentPosition.set(targetPosition);
//		}
//	}
//
//	////////////////////////////////////////////////////////////////////////////
//
//	private static class KinematicClosestNotMeRayResultCallback extends ClosestRay {
//		protected Collidable me;
//
//		public KinematicClosestNotMeRayResultCallback(Collidable me) {
//			super(new v3(), new v3());
//			this.me = me;
//		}
//
//		@Override
//		public float addSingleResult(Collisions.LocalRayResult rayResult, boolean normalInWorldSpace) {
//			if (rayResult.collidable == me) {
//				return 1.0f;
//			}
//
//			return super.addSingleResult(rayResult, normalInWorldSpace);
//		}
//	}
//
//	////////////////////////////////////////////////////////////////////////////
//
//	private static class KinematicClosestNotMeConvexResultCallback extends Collisions.ClosestConvexResultCallback {
//		protected Collidable me;
//		protected final v3 up;
//		protected float minSlopeDot;
//
//		public KinematicClosestNotMeConvexResultCallback(Collidable me, final v3 up, float minSlopeDot) {
//			super(new v3(), new v3());
//			this.me = me;
//			this.up = up;
//			this.minSlopeDot = minSlopeDot;
//		}
//
//		@Override
//		public float addSingleResult(Collisions.LocalConvexResult convexResult, boolean normalInWorldSpace) {
//                        //XXX: no contact response
//                        if (!convexResult.hitCollidable.hasContactResponse())
//                           return 1.0f;
//                        if (convexResult.hitCollidable == me) {
//				return 1.0f;
//			}
//
//			v3 hitNormalWorld;
//			if (normalInWorldSpace) {
//				hitNormalWorld = convexResult.hitNormalLocal;
//			} else {
//				//need to transform normal into worldspace
//				hitNormalWorld = new v3();
//				convexResult.hitCollidable.getWorldTransform(new Transform()).basis.transform(convexResult.hitNormalLocal, hitNormalWorld);
//			}
//
//			float dotUp = up.dot(hitNormalWorld);
//			if (dotUp < minSlopeDot) {
//				return 1.0f;
//			}
//
//			return super.addSingleResult(convexResult, normalInWorldSpace);
//		}
//	}
//
//}
