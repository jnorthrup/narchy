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

package spacegraph.space3d.phys;

import jcog.Util;
import jcog.math.v3;
import spacegraph.space3d.phys.collision.CollidableType;
import spacegraph.space3d.phys.collision.CollisionFlags;
import spacegraph.space3d.phys.collision.broad.Broadphasing;
import spacegraph.space3d.phys.constraint.TypedConstraint;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.TransformUtil;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.util.OArrayList;
import spacegraph.util.math.Matrix3f;
import spacegraph.util.math.Quat4f;

import static jcog.Util.unitize;
import static jcog.math.v3.v;


/**
 * RigidBody is the main class for rigid body objects. It is derived from
 * {@link Collidable}, so it keeps reference to {@link CollisionShape}.<p>
 * 
 * It is recommended for performance and memory use to share {@link CollisionShape}
 * objects whenever possible.<p>
 * 
 * There are 3 types of rigid bodies:<br>
 * <ol>
 * <li>Dynamic rigid bodies, with positive mass. Motion is controlled by rigid body dynamics.</li>
 * <li>Fixed objects with zero mass. They are not moving (basically collision objects).</li>
 * <li>Kinematic objects, which are objects without mass, but the user can move them. There
 *     is on-way interaction, and Bullet calculates a velocity based on the timestep and
 *     previous and current world transform.</li>
 * </ol>
 * 
 * Bullet automatically deactivates dynamic rigid bodies, when the velocity is below
 * a threshold for a given time.<p>
 * 
 * Deactivated (sleeping) rigid bodies don't take any processing time, except a minor
 * broadphase collision detection impact (to allow active objects to activate/wake up
 * sleeping objects).
 * 
 * @author jezek2
 */
public class Body3D<X> extends Collidable<X> {

	private static final float MAX_ANGVEL = BulletGlobals.SIMD_HALF_PI;
	
	public final Matrix3f invInertiaTensorWorld = new Matrix3f();
	public final v3 linearVelocity = new v3();
	public final v3 angularVelocity = new v3();
	private float inverseMass;
	private float angularFactor;

	private final v3 gravity = new v3();
	public final v3 invInertiaLocal = new v3();
	private final v3 totalForce = new v3();
	private final v3 totalTorque = new v3();


	private float linearDamping;
	private float angularDamping;







	private float linearSleepingThreshold;
	private float angularSleepingThreshold;




	
	private final OArrayList<TypedConstraint> constraintRefs = new OArrayList<>();

	
	public int contactSolverType;
	public int frictionSolverType;
	

	public Body3D(float mass, Transform t, CollisionShape collisionShape) {
		this(mass, t, collisionShape, v());
	}

	private Body3D(float mass, Transform t, CollisionShape collisionShape, v3 localInertia) {
		super(CollidableType.RIGID_BODY, t);

		linearVelocity.set(0f, 0f, 0f);
		angularVelocity.set(0f, 0f, 0f);
		angularFactor = 1f;
		gravity.set(0f, 0f, 0f);
		totalForce.set(0f, 0f, 0f);
		totalTorque.set(0f, 0f, 0f);


        linearDamping = 0f;
		angularDamping = 0.5f;
		float linearSleepingThreshold = 0.8f;
        this.linearSleepingThreshold = linearSleepingThreshold;
		float angularSleepingThreshold = 1.0f;
        this.angularSleepingThreshold = angularSleepingThreshold;
		
		contactSolverType = 0;
		frictionSolverType = 0;

		/**
		 * Additional damping can help avoiding lowpass jitter motion, help stability for ragdolls etc.
		 * Such damping is undesirable, so once the overall simulation quality of the rigid body dynamics
		 * system has improved, this should become obsolete.
		 */



















		interpolationWorldTransform.set(transform);




		/** Best simulation results when friction is non-zero. */
		friction = 0.5f;

		/** Best simulation results using zero restitution. */
		restitution = 0f;

		setCollisionShape(collisionShape);

		setMass(mass, localInertia);
		setDamping((float) 0, (float) 0);
		updateInertiaTensor();
	}
	
	public void destroy(Collisions world) {
		
		
		assert (constraintRefs.isEmpty());
		forceActivationState(Collidable.DISABLE_SIMULATION);
		data = null;

		Broadphasing bp = broadphase;
		if (bp != null) {
			
			
			

			world.broadphase.getOverlappingPairCache().cleanProxyFromPairs(bp, world.intersecter);
			world.broadphase.destroyProxy(bp, world.intersecter);
			broadphase(null);
		} /*else {
        	
			throw new RuntimeException(collidable + " missing broadphase");
		}*/











	}


	public void proceedToTransform(Transform newTrans) {
		setCenterOfMassTransform(newTrans);
	}
	
	/**
	 * To keep collision detection and dynamics separate we don't store a rigidbody pointer,
	 * but a rigidbody is derived from CollisionObject, so we can safely perform an upcast.
	 */
	public static <X> Body3D<X> ifDynamic(Collidable<X> colObj) {
		return colObj.getInternalType() == CollidableType.RIGID_BODY ? (Body3D) colObj : null;
	}

	public static <X> Body3D<X> ifDynamicAndActive(Collidable<X> colObj) {
		Body3D<X> d = ifDynamic(colObj);
		return ((d == null) || !d.isActive()) ? null : d;
	}

	/**
	 * Continuous collision detection needs prediction.
	 */
	public void predictIntegratedTransform(float timeStep, Transform predictedTransform) {
		TransformUtil.integrateTransform(transform, linearVelocity, angularVelocity, timeStep, predictedTransform);
	}


	public void saveKinematicState(float timeStep) {
		if (!isKinematicObject())
			return;

		
		if (timeStep != 0f) {




			

			TransformUtil.calculateVelocity(interpolationWorldTransform, transform, timeStep, linearVelocity, angularVelocity);


			interpolationWorldTransform.set(transform);
		
		}
	}
	
	public void applyGravity() {
		if (isStaticOrKinematicObject())
			return;

		force(gravity);
	}
	
	@Override
    public void setGravity(v3 acceleration) {
		if (inverseMass != 0f) {
			gravity.scale(1f / inverseMass, acceleration);
		}
	}

	public v3 getGravity(v3 out) {
		out.set(gravity);
		return out;
	}

	public void setDamping(float lin_damping, float ang_damping) {
		linearDamping = unitize(lin_damping);
		angularDamping = unitize(ang_damping);
	}

	public float getLinearDamping() {
		return linearDamping;
	}

	public float getAngularDamping() {
		return angularDamping;
	}

	public float getLinearSleepingThreshold() {
		return linearSleepingThreshold;
	}

	public float getAngularSleepingThreshold() {
		return angularSleepingThreshold;
	}

	/**
	 * Damps the velocity, using the given linearDamping and angularDamping.
	 */
	public void applyDamping(float timeStep) {
		
		

		
		
		
		
		
		if (linearDamping > (float) 0)
			linearVelocity.scaled((float) Math.pow((double) (1f - linearDamping), (double) timeStep));
		if (angularDamping > (float) 0)
			angularVelocity.scaled((float) Math.pow((double) (1f - angularDamping), (double) timeStep));
		






































	}

	public void setMass(float mass, v3 inertia) {
		setMass(mass);

		invInertiaLocal.set(inertia.x != 0f ? 1f / inertia.x : 0f,
				inertia.y != 0f ? 1f / inertia.y : 0f,
				inertia.z != 0f ? 1f / inertia.z : 0f);
	}

	public void setMass(float mass) {
		if (mass == 0f) {
			collisionFlags |= CollisionFlags.STATIC_OBJECT;
			inverseMass = 0f;
		}
		else {
			collisionFlags &= (~CollisionFlags.STATIC_OBJECT);
			inverseMass = 1f / mass;
		}
	}

	public float mass() {
		return 1f/inverseMass;
	}

	public float getInvMass() {
		return inverseMass;
	}

	public Matrix3f getInvInertiaTensorWorld(Matrix3f out) {
		out.set(invInertiaTensorWorld);
		return out;
	}
	
	public void integrateVelocities(float step) {
		if (isStaticOrKinematicObject()) {
			return;
		}

		linearVelocity.scaleAdd(inverseMass * step, totalForce, linearVelocity);

		invInertiaTensorWorld.transform(totalTorque);
		angularVelocity.scaleAdd(step, totalTorque, angularVelocity);


		float angvel = angularVelocity.lengthSquared();
		if (angvel > Util.sqr(MAX_ANGVEL/step)) {
			angularVelocity.scaled((MAX_ANGVEL / step) / angvel);
		}
	}

	public void setCenterOfMassTransform(Transform xform) {
		interpolationWorldTransform.set(isStaticOrKinematicObject() ? transform : xform);


		transform.set(xform);
		updateInertiaTensor();
	}

	private void force(v3 forcetoCenter) {
		totalForce.add(forcetoCenter);
		activate();
	}
	
	public v3 getInvInertiaDiagLocal(v3 out) {
		out.set(invInertiaLocal);
		return out;
	}

	public void setInvInertiaDiagLocal(v3 diagInvInertia) {
		invInertiaLocal.set(diagInvInertia);
	}

	public void setSleepingThresholds(float linear, float angular) {
		linearSleepingThreshold = linear;
		angularSleepingThreshold = angular;
	}

	public void torque(v3 torque) {
		totalTorque.add(torque);
	}










	/** applied to the center */
	public void impulse(v3 impulse) {
		linearVelocity.scaleAdd(inverseMass, impulse, linearVelocity);
		activate();
	}


	public void torqueImpulse(v3 torque) {
		v3 tmp = new v3(torque);
		invInertiaTensorWorld.transform(tmp);
		angularVelocity.add(tmp);
		activate();
	}


	public void impulse(v3 impulse, v3 rel_pos) {
		if (inverseMass != 0f) {
			impulse(impulse);
			if (angularFactor != 0f) {
				v3 tmp = new v3();
				tmp.cross(rel_pos, impulse);
				tmp.scaled(angularFactor);
				torqueImpulse(tmp);
			}
		}
	}

	/**
	 * Optimization for the iterative solver: avoid calculating constant terms involving inertia, normal, relative position.
	 */
	public void internalApplyImpulse(v3 linearComponent, v3 angularComponent, float impulseMagnitude) {
		if (inverseMass != 0f) {
			linearVelocity.scaleAdd(impulseMagnitude, linearComponent, linearVelocity);
			if (angularFactor != 0f) {
				angularVelocity.scaleAdd(impulseMagnitude * angularFactor, angularComponent, angularVelocity);
			}
		}
	}

	public void clearForces() {
		totalForce.set(0f, 0f, 0f);
		totalTorque.set(0f, 0f, 0f);
	}
	
	private void updateInertiaTensor() {
		Matrix3f mat1 = new Matrix3f();
		MatrixUtil.scale(mat1, transform.basis, invInertiaLocal);

		Matrix3f mat2 = new Matrix3f(transform.basis);
		mat2.transpose();

		invInertiaTensorWorld.mul(mat1, mat2);
	}

	@Deprecated public v3 getCenterOfMassPosition(v3 out) {
		out.set(transform);
		return out;
	}

	@Deprecated public Quat4f getOrientation(Quat4f out) {
		MatrixUtil.getRotation(transform.basis, out);
		return out;
	}
	
	@Deprecated public Transform getCenterOfMassTransform(Transform out) {
		out.set(transform);
		return out;
	}


	public v3 getLinearVelocity(v3 out) {
		out.set(linearVelocity);
		return out;
	}

	public v3 getAngularVelocity(v3 out) {
		out.set(angularVelocity);
		return out;
	}


	public void setLinearVelocity(float x, float y, float z) {
		linearVelocity.set(x, y, z);
	}

	public void setLinearVelocity(v3 lin_vel) {
		assert (collisionFlags != CollisionFlags.STATIC_OBJECT);
		linearVelocity.set(lin_vel);
	}

	public void setAngularVelocity(v3 ang_vel) {
		assert (collisionFlags != CollisionFlags.STATIC_OBJECT);
		angularVelocity.set(ang_vel);
	}

	public v3 getVelocityInLocalPoint(v3 rel_pos, v3 out) {

		v3 vec = out;
		vec.cross(angularVelocity, rel_pos);
		vec.add(linearVelocity);
		return out;

		
		
	}

	public void translate(v3 v) {
		transform.add(v);
	}


	public void getAabb(v3 aabbMin, v3 aabbMax) {
		shape().getAabb(transform, aabbMin, aabbMax);
	}

	public float computeImpulseDenominator(v3 pos, v3 normal) {
		v3 r0 = new v3();
		r0.sub(pos, transform);

		v3 c0 = new v3();
		c0.cross(r0, normal);

		v3 tmp = new v3();
		MatrixUtil.transposeTransform(tmp, c0, invInertiaTensorWorld);

		v3 vec = new v3();
		vec.cross(tmp, r0);

		return inverseMass + normal.dot(vec);
	}

	public float computeAngularImpulseDenominator(v3 axis) {
		v3 vec = new v3();
		MatrixUtil.transposeTransform(vec, axis, invInertiaTensorWorld);
		return axis.dot(vec);
	}

	public void updateDeactivation(float timeStep) {
		int state = getActivationState();
		if ((state == ISLAND_SLEEPING) || (state == DISABLE_DEACTIVATION)) {
			return;
		}

		if ((linearVelocity.lengthSquared() < linearSleepingThreshold * linearSleepingThreshold) &&
				(angularVelocity.lengthSquared() < angularSleepingThreshold * angularSleepingThreshold)) {
			deactivationTime += timeStep;
		}
		else {
			deactivationTime = 0f;
			setActivationState(0);
		}
	}

	public boolean wantsSleeping(float theDeactivationTime) {
		int state = getActivationState();
		if (state == DISABLE_DEACTIVATION) {
			return false;
		}


		if ((state == ISLAND_SLEEPING) || (state == WANTS_DEACTIVATION)) {
			return true;
		}

		return deactivationTime > theDeactivationTime;
	}













	public void setAngularFactor(float angFac) {
		angularFactor = angFac;
	}

	public float getAngularFactor() {
		return angularFactor;
	}








	@Override
	public boolean checkCollideWithOverride(Collidable co) {

		Body3D otherRb = ifDynamic(co);
		if (otherRb == null) {
			return true;
		}

		for (TypedConstraint c : constraintRefs) {
			if (c.getRigidBodyA() == otherRb || c.getRigidBodyB() == otherRb) {
				return false;
			}
		}
		return true;
	}

	public void addConstraintRef(TypedConstraint c) {
		int index = constraintRefs.indexOf(c);
		if (index == -1) {
			constraintRefs.add(c);
		}

		checkCollideWith = true;
	}
	
	public void removeConstraintRef(TypedConstraint c) {
		constraintRefs.remove(c);
		checkCollideWith = !constraintRefs.isEmpty();
	}

	public TypedConstraint getConstraintRef(int index) {
		return constraintRefs.get(index);
		
	}

	public void velAdd(v3 delta) {
		linearVelocity.add(delta);
		activate();
	}









}
