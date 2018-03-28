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
//import spacegraph.phys.Collidable;
//import spacegraph.phys.DiscreteDynamics;
//import spacegraph.phys.Dynamics;
//import spacegraph.phys.Dynamic;
//import spacegraph.phys.collision.CollisionConfiguration;
//import spacegraph.phys.collision.broad.Broadphase;
//import spacegraph.phys.collision.broad.DispatcherInfo;
//import spacegraph.phys.collision.broad.Intersecter;
//import spacegraph.phys.collision.narrow.PersistentManifold;
//import spacegraph.phys.solve.Constrainer;
//import spacegraph.phys.solve.ContactSolverInfo;
//import spacegraph.phys.math.IDebugDraw;
//import spacegraph.phys.math.Transform;
//import spacegraph.phys.util.OArrayList;
//
///**
// * SimpleDynamicsWorld serves as unit-test and to verify more complicated and
// * optimized dynamics worlds. Please use {@link DiscreteDynamics} instead
// * (or ContinuousDynamicsWorld once it is finished).
// *
// * @author jezek2
// */
//@Deprecated public class SimpleDynamics<X> extends Dynamics<X> {
//
//	protected Constrainer constrainer;
//	protected boolean ownsConstraintSolver;
//	protected final v3 gravity = new v3(0f, 0f, -10f);
//
//	public SimpleDynamics(Intersecter intersecter, Broadphase pairCache, Constrainer constrainer, CollisionConfiguration collisionConfiguration) {
//		super(intersecter, pairCache, collisionConfiguration);
//		this.constrainer = constrainer;
//		this.ownsConstraintSolver = false;
//	}
//
//	protected void predictUnconstraintMotion(float timeStep) {
//		Transform tmpTrans = new Transform();
//
//		for (int i = 0; i < objects.size(); i++) {
//            //return array[index];
//            Collidable colObj = objects.get(i);
//			Dynamic body = Dynamic.upcast(colObj);
//			if (body != null) {
//				if (!body.isStaticObject()) {
//					if (body.isActive()) {
//						body.applyGravity();
//						body.integrateVelocities(timeStep);
//						body.applyDamping(timeStep);
//						body.predictIntegratedTransform(timeStep, body.getInterpolationWorldTransform(tmpTrans));
//					}
//				}
//			}
//		}
//	}
//
//	protected void integrateTransforms(float timeStep) {
//		Transform predictedTrans = new Transform();
//		for (int i = 0; i < objects.size(); i++) {
//            //return array[index];
//            Collidable colObj = objects.get(i);
//			Dynamic body = Dynamic.upcast(colObj);
//			if (body != null) {
//				if (body.isActive() && (!body.isStaticObject())) {
//					body.predictIntegratedTransform(timeStep, predictedTrans);
//					body.proceedToTransform(predictedTrans);
//				}
//			}
//		}
//	}
//
//	/**
//	 * maxSubSteps/fixedTimeStep for interpolation is currently ignored for SimpleDynamicsWorld, use DiscreteDynamicsWorld instead.
//	 */
//	@Override
//	public int stepSimulation(float timeStep, int maxSubSteps, float fixedTimeStep) {
//		// apply gravity, predict motion
//		predictUnconstraintMotion(timeStep);
//
//		DispatcherInfo dispatchInfo = getDispatchInfo();
//		dispatchInfo.timeStep = timeStep;
//		dispatchInfo.stepCount = 0;
//
//		// perform collision detection
//		performDiscreteCollisionDetection();
//
//		// solve contact constraints
//		int numManifolds = intersecter1.getNumManifolds();
//		if (numManifolds != 0)
//		{
//			OArrayList<PersistentManifold> manifoldPtr = intersecter1.getInternalManifoldPointer();
//
//			ContactSolverInfo infoGlobal = new ContactSolverInfo();
//			infoGlobal.timeStep = timeStep;
//			constrainer.prepareSolve(0,numManifolds);
//			constrainer.solveGroup(null,0,manifoldPtr, 0, numManifolds, null,0,0,infoGlobal/*, m_stackAlloc*/, intersecter1);
//			constrainer.allSolved(infoGlobal /*, m_stackAlloc*/);
//		}
//
//		// integrate transforms
//		integrateTransforms(timeStep);
//
//		updateAabbs();
//
//		synchronizeMotionStates();
//
//		clearForces();
//
//		return 1;
//	}
//
//	public void clearForces() {
//		// todo: iterate over awake simulation islands!
//
//		for (int i = 0; i < objects.size(); i++) {
//            //return array[index];
//            Collidable colObj = objects.get(i);
//
//			Dynamic body = Dynamic.upcast(colObj);
//			if (body != null) {
//				body.clearForces();
//			}
//		}
//	}
//
//	@Override
//	public void setGravity(v3 gravity) {
//		this.gravity.set(gravity);
//		for (int i = 0; i < objects.size(); i++) {
//            //return array[index];
//            Collidable colObj = objects.get(i);
//			Dynamic body = Dynamic.upcast(colObj);
//			if (body != null) {
//				body.setGravity(gravity);
//			}
//		}
//	}
//
//	@Override
//	public v3 getGravity(v3 out) {
//		out.set(gravity);
//		return out;
//	}
//
//	@Override
//	public void addRigidBody(Dynamic body) {
//		body.setGravity(gravity);
//
//		if (body.shape() != null) {
//			add(body);
//		}
//	}
//
//
//	@Override
//	public void updateAabbs() {
//		Transform tmpTrans = new Transform();
//		Transform predictedTrans = new Transform();
//		v3 minAabb = new v3(), maxAabb = new v3();
//
//		for (int i = 0; i < objects.size(); i++) {
//            //return array[index];
//            Collidable colObj = objects.get(i);
//			Dynamic body = Dynamic.upcast(colObj);
//			if (body != null) {
//				if (body.isActive() && (!body.isStaticObject())) {
//					colObj.shape().getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb);
//					Broadphase bp = getBroadphase();
//					bp.setAabb(body.broadphase(), minAabb, maxAabb, intersecter1);
//				}
//			}
//		}
//	}
//
//	public void synchronizeMotionStates() {
//		Transform tmpTrans = new Transform();
//
//		// todo: iterate over awake simulation islands!
//		for (int i = 0; i < objects.size(); i++) {
//            //return array[index];
//            Collidable colObj = objects.get(i);
//			Dynamic body = Dynamic.upcast(colObj);
//			if (body != null && body.getMotionState() != null) {
//				if (body.getActivationState() != Collidable.ISLAND_SLEEPING) {
//					body.getMotionState().setWorldTransform(body.getWorldTransform(tmpTrans));
//				}
//			}
//		}
//	}
//
//	@Override
//	public void setConstrainer(Constrainer solver) {
//		if (ownsConstraintSolver) {
//			//btAlignedFree(m_constraintSolver);
//		}
//
//		ownsConstraintSolver = false;
//		constrainer = solver;
//	}
//
//	@Override
//	public Constrainer getConstrainer() {
//		return constrainer;
//	}
//
//	@Override
//	public void debugDrawWorld(IDebugDraw draw) {
//		// TODO: throw new UnsupportedOperationException("Not supported yet.");
//	}
//
//}
