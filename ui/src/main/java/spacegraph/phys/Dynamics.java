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

package spacegraph.phys;

import jcog.list.FasterList;
import jcog.util.Flip;
import org.jetbrains.annotations.Nullable;
import spacegraph.Spatial;
import spacegraph.math.Matrix3f;
import spacegraph.math.v3;
import spacegraph.phys.collision.Islands;
import spacegraph.phys.collision.broad.*;
import spacegraph.phys.collision.narrow.PersistentManifold;
import spacegraph.phys.constraint.BroadConstraint;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.*;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.SphereShape;
import spacegraph.phys.solve.Constrainer;
import spacegraph.phys.solve.ContactSolverInfo;
import spacegraph.phys.solve.SequentialImpulseConstrainer;
import spacegraph.phys.util.Animated;
import spacegraph.phys.util.OArrayList;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static spacegraph.math.v3.v;
import static spacegraph.phys.Collidable.ISLAND_SLEEPING;
import static spacegraph.phys.Dynamic.ifDynamic;

/**
 * DynamicsWorld is the interface class for several dynamics implementation,
 * basic, discrete, parallel, and continuous etc.
 *
 * @author jezek2
 */
public class Dynamics<X> extends Collisions<X> {

    private static final Comparator<TypedConstraint> sortConstraintOnIslandPredicate = (lhs, rhs) -> {
        return (lhs == rhs) ? 0
                :
                ((getConstraintIslandId(lhs) < getConstraintIslandId(rhs)) ? -1 : +1);
    };

    protected final Constrainer constrainer;
    protected final Islands islands;
    protected final List<TypedConstraint> constraints = new FasterList();
    @Nullable protected v3 gravity;


    final Flip<List<Collidable>> coll = new Flip(FasterList::new);
    private List<Collidable> collidable = new FasterList();

    public final FasterList<BroadConstraint> broadConstraints = new FasterList<>(0);
    final FasterList<TypedConstraint> sortedConstraints = new FasterList<>(0);
    final InplaceSolverIslandCallback solverCallback = new InplaceSolverIslandCallback();


    public final ContactSolverInfo solverInfo = new ContactSolverInfo();
    //for variable timesteps
    protected float localTime = 1f / 60f;
    protected boolean ownsIslandManager;
    protected boolean ownsConstrainer;
    //protected OArrayList<RaycastVehicle> vehicles = new OArrayList<RaycastVehicle>();
    private final List<Animated> animations = new FasterList();
//    protected int profileTimings;
    private float dt;
    private final Iterable<Spatial<X>> spatials;


    public Dynamics(Intersecter intersecter, Broadphase broadphase, Iterable<Spatial<X>> spatials) {
        this(intersecter, broadphase, spatials, null);
    }

    public Dynamics(Intersecter intersecter, Broadphase broadphase, Iterable<Spatial<X>> spatials, Constrainer constrainer) {
        super(intersecter, broadphase);
        this.spatials = spatials;
        islands = new Islands();
        ownsIslandManager = true;
        if (constrainer == null) {
            this.constrainer = new SequentialImpulseConstrainer(this);
            this.ownsConstrainer = true;
        } else {
            this.constrainer = constrainer;
            this.ownsConstrainer = false;
        }

    }

    public static Dynamic newBody(float mass, CollisionShape shape, Transform t, int group, int mask) {

        Dynamic body = new Dynamic(mass, t, shape);
        body.group = (short) group;
        body.mask = (short) mask;
        if (mass != 0f) { // rigidbody is dynamic if and only if mass is non zero, otherwise static
            shape.calculateLocalInertia(mass, v());
        }

        return body;
    }


    public final int update(float dt, int maxSubSteps) {
        return update(dt, maxSubSteps, 1f / 60f);
    }

    protected void synchronizeMotionStates(boolean clear) {
//        Transform interpolatedTransform = new Transform();
//
//        Transform tmpTrans = new Transform();
//        v3 tmpLinVel = new v3();
//        v3 tmpAngVel = new v3();

        // todo: iterate over awake simulation islands!
        for (int i = 0, collidableSize = collidable.size(); i < collidableSize; i++) {
            Collidable ccc = collidable.get(i);

            Dynamic body = ifDynamic(ccc);
            if (body == null) {
                continue;
            }

//            if (body.getMotionState() != null && !body.isStaticOrKinematicObject()) {
//                // we need to call the update at least once, even for sleeping objects
//                // otherwise the 'graphics' transform never updates properly
//                // so todo: add 'dirty' flag
//                //if (body->getActivationState() != ISLAND_SLEEPING)
//                TransformUtil.integrateTransform(
//                        body.getInterpolationWorldTransform(tmpTrans),
//                        body.getInterpolationLinearVelocity(tmpLinVel),
//                        body.getInterpolationAngularVelocity(tmpAngVel),
//                        localTime * body.getHitFraction(), interpolatedTransform);
//                body.getMotionState().setWorldTransform(interpolatedTransform);
//            }

            if (clear) {
                body.clearForces();
            }
        }


    }

    private int update(float timeStep, int maxSubSteps, float fixedTimeStep) {
        curDT = timeStep;

        BulletGlobals.the.set(this);

        //long t0 = System.nanoTime();
        updateAnimations();

        //startProfiling(timeStep);



        //BulletStats.pushProfile("stepSimulation");
        try {
            int numSimulationSubSteps = 0;

            if (maxSubSteps != 0) {
                // fixed timestep with interpolation
                localTime += timeStep;
                if (localTime >= fixedTimeStep) {
                    numSimulationSubSteps = (int) (localTime / fixedTimeStep);
                    localTime -= numSimulationSubSteps * fixedTimeStep;
                }
            } else {
                //variable timestep
                fixedTimeStep = timeStep;
                localTime = timeStep;
                if (ScalarUtil.fuzzyZero(timeStep)) {
                    numSimulationSubSteps = 0;
                    maxSubSteps = 0;
                } else {
                    numSimulationSubSteps = 1;
                    maxSubSteps = 1;
                }
            }

            this.dt = fixedTimeStep;

            updateObjects();

            if (numSimulationSubSteps != 0) {

                // clamp the number of substeps, to prevent simulation grinding spiralling down to a halt
                int clampedSimulationSteps = Math.min(numSimulationSubSteps, maxSubSteps);

                for (int i = 0; i < clampedSimulationSteps; i++) {
                    internalSingleStepSimulation(fixedTimeStep);
                    synchronizeMotionStates(i == clampedSimulationSteps - 1);
                }
            }

            //synchronizeMotionStates(true);

            CProfileManager.incrementFrameCounter();

            return numSimulationSubSteps;
        } catch (Throwable t) {
            t.printStackTrace();
            return 1;
        } /*finally {
            //BulletStats.popProfile();

            //BulletStats.stepSimulationTime = (System.nanoTime() - t0) / 1000000;
        }*/
    }



    protected final void updateObjects() {

        List<Collidable> nextCollidables = coll.write();
        nextCollidables.clear();

        final short[] i = {0};
        spatials.forEach((s) -> {

//            if (s.preactive()) {
            s.order = i[0]++;

            s.update(this);

            s.forEachBody(c -> {

                Dynamic d = ifDynamic(c);
                if (d != null) {

                    on(d);

                    nextCollidables.add(d);

                    if (d.getActivationState() != Collidable.ISLAND_SLEEPING)
                        d.saveKinematicState(dt); // to calculate velocities next frame

                    if (gravity != null) {
                        if (!d.isStaticOrKinematicObject())
                            d.setGravity(gravity);

                        if (d.isActive())
                            d.applyGravity();
                    }


                }
            });

            List<TypedConstraint> cc = s.constraints();
            if (cc!=null)
                cc.forEach(this::addConstraint);

//            } else {
//                if (s.hide()) {
//                    s.constraints().forEach(this::removeConstraint);
//                    s.forEachBody(this::removing);
//                }
//
//            }

        });

        //System.out.println(nextCollidables.size() + " " + pairs().size());


        //List<Collidable> prevCollidables = collidable;
        this.collidable = coll.commit();

    }

//    @Override
//    public final void forEachCollidable(IntObjectProcedure<Collidable<X>> each) {
//        this.collidable.forEachWithIndexProc(each::value);
//    }

    @Override
    public final List<Collidable> collidables() {
        return collidable;
    }

//    public void debugDrawWorld(IDebugDraw debugDrawer) {
//
//        if (debugDrawer != null && (debugDrawer.getDebugMode() & DebugDrawModes.DRAW_CONTACT_POINTS) != 0) {
//            int numManifolds = intersecter.manifoldCount();
//            v3 color = new v3();
//            color.set(0f, 0f, 0f);
//            for (int i = 0; i < numManifolds; i++) {
//                PersistentManifold contactManifold = intersecter.manifold(i);
//                //btCollisionObject* obA = static_cast<btCollisionObject*>(contactManifold->getBody0());
//                //btCollisionObject* obB = static_cast<btCollisionObject*>(contactManifold->getBody1());
//
//                int numContacts = contactManifold.getNumContacts();
//                for (int j = 0; j < numContacts; j++) {
//                    ManifoldPoint cp = contactManifold.getContactPoint(j);
//                    debugDrawer.drawContactPoint(cp.positionWorldOnB, cp.normalWorldOnB, cp.distance1, cp.lifeTime, color);
//                }
//            }
//        }
//
//        if (debugDrawer != null && (debugDrawer.getDebugMode() & (DebugDrawModes.DRAW_WIREFRAME | DebugDrawModes.DRAW_AABB)) != 0) {
//            int i;
//
//            Transform tmpTrans = new Transform();
//            v3 minAabb = new v3();
//            v3 maxAabb = new v3();
//            v3 colorvec = new v3();
//
//            for (Collidable colObj : collidable) {
//                if (debugDrawer != null && (debugDrawer.getDebugMode() & DebugDrawModes.DRAW_WIREFRAME) != 0) {
//                    v3 color = new v3();
//                    color.set(255f, 255f, 255f);
//                    switch (colObj.getActivationState()) {
//                        case Collidable.ACTIVE_TAG:
//                            color.set(255f, 255f, 255f);
//                            break;
//                        case Collidable.ISLAND_SLEEPING:
//                            color.set(0f, 255f, 0f);
//                            break;
//                        case Collidable.WANTS_DEACTIVATION:
//                            color.set(0f, 255f, 255f);
//                            break;
//                        case Collidable.DISABLE_DEACTIVATION:
//                            color.set(255f, 0f, 0f);
//                            break;
//                        case Collidable.DISABLE_SIMULATION:
//                            color.set(255f, 255f, 0f);
//                            break;
//                        default:
//                            color.set(255f, 0f, 0f);
//                    }
//
//                    debugDrawObject(debugDrawer, colObj.getWorldTransform(tmpTrans), colObj.shape(), color);
//                }
//                if (debugDrawer != null && (debugDrawer.getDebugMode() & DebugDrawModes.DRAW_AABB) != 0) {
//                    colorvec.set(1f, 0f, 0f);
//                    colObj.shape().getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb);
//                    debugDrawer.drawAabb(minAabb, maxAabb, colorvec);
//                }
//            }
//
//            v3 wheelColor = new v3();
//            v3 wheelPosWS = new v3();
//            v3 axle = new v3();
//            v3 tmp = new v3();
//
////            for (i = 0; i < vehicles.size(); i++) {
////                //return array[index];
////                for (int v = 0; v < vehicles.get(i).getNumWheels(); v++) {
////                    wheelColor.set(0, 255, 255);
////                    //return array[index];
////                    if (vehicles.get(i).getWheelInfo(v).raycastInfo.isInContact) {
////                        wheelColor.set(0, 0, 255);
////                    } else {
////                        wheelColor.set(255, 0, 255);
////                    }
////
////                    //return array[index];
////                    wheelPosWS.set(vehicles.get(i).getWheelInfo(v).worldTransform);
////
////                    //return array[index];
////                    //return array[index];
////                    //return array[index];
////                    //return array[index];
////                    //return array[index];
////                    //return array[index];
////                    axle.set(
////                            vehicles.get(i).getWheelInfo(v).worldTransform.basis.get(0, vehicles.get(i).getRightAxis()),
////                            vehicles.get(i).getWheelInfo(v).worldTransform.basis.get(1, vehicles.get(i).getRightAxis()),
////                            vehicles.get(i).getWheelInfo(v).worldTransform.basis.get(2, vehicles.get(i).getRightAxis()));
////
////
////                    //m_vehicles[i]->getWheelInfo(v).m_raycastInfo.m_wheelAxleWS
////                    //debug wheels (cylinders)
////                    tmp.add(wheelPosWS, axle);
////                    debugDrawer.drawLine(wheelPosWS, tmp, wheelColor);
////                    //return array[index];
////                    debugDrawer.drawLine(wheelPosWS, vehicles.get(i).getWheelInfo(v).raycastInfo.contactPointWS, wheelColor);
////                }
////            }
//
//        }
//    }

    public final void addConstraint(TypedConstraint constraint) {
        addConstraint(constraint, false);
    }






    protected void updateActivationState(float timeStep) {

        // disable deactivation
		float deactivationTime = isDeactivationDisabled() ? 0 : getDeactivationTime();

        collidable.forEach(colObj -> {
            Dynamic body = ifDynamic(colObj);
            if (body != null) {
                body.updateDeactivation(timeStep);

                if (deactivationTime > 0 && body.wantsSleeping(deactivationTime)) {
                    if (body.isStaticOrKinematicObject()) {
                        body.setActivationState(Collidable.ISLAND_SLEEPING);
                    } else {
                        switch (body.getActivationState()) {
                            case Collidable.ACTIVE_TAG:
                                body.setActivationState(Collidable.WANTS_DEACTIVATION);
                                break;
                            case ISLAND_SLEEPING:
                                body.angularVelocity.zero();
                                body.linearVelocity.zero();
                                break;
                        }

                    }
                } else {
                    if (body.getActivationState() != Collidable.DISABLE_DEACTIVATION) {
                        body.setActivationState(Collidable.ACTIVE_TAG);
                    }
                }
            }
        });
    }

    public void addConstraint(TypedConstraint constraint, boolean disableCollisionsBetweenLinkedBodies) {
        synchronized (constraints) {
            constraints.add(constraint);
            if (disableCollisionsBetweenLinkedBodies) {
                constraint.getRigidBodyA().addConstraintRef(constraint);
                constraint.getRigidBodyB().addConstraintRef(constraint);
            }
        }
    }

    public void removeConstraint(TypedConstraint constraint) {
        synchronized (constraints) {
            constraints.remove(constraint);
            constraint.getRigidBodyA().removeConstraintRef(constraint);
            constraint.getRigidBodyB().removeConstraintRef(constraint);
        }
    }

    private void internalSingleStepSimulation(float timeStep) {

            // apply gravity, predict motion
            predictUnconstraintMotion(timeStep);

            DispatcherInfo dispatchInfo = getDispatchInfo();
            dispatchInfo.timeStep = timeStep;
            dispatchInfo.stepCount = 0;

            solveCollisions();

            solveConstraints(timeStep, solverInfo);

            solveBroadConstraints(timeStep);

            integrateTransforms(timeStep);

            updateActivationState(timeStep);

    }


    public void addBroadConstraint(BroadConstraint b) {
        broadConstraints.add(b);
    }

    protected final void solveBroadConstraints(float timeStep) {
        for (BroadConstraint b : broadConstraints) {
            b.solve(broadphase, collidable, timeStep);
        }
    }

    public void setGravity(@Nullable v3 gravity) {
        this.gravity = gravity;
    }


//    public void removeVehicle(RaycastVehicle vehicle) {
//        vehicles.remove(vehicle);
//    }

    private static int getConstraintIslandId(TypedConstraint lhs) {
        int rcolObj0 = lhs.getRigidBodyA().tag();
        int rcolObj1 = lhs.getRigidBodyB().tag();
        return rcolObj0 >= 0 ? rcolObj0 : rcolObj1;
    }

    /**
     * solve contact and other joint constraints
     */
    protected void solveConstraints(float timeStep, ContactSolverInfo solverInfo) {

        calculateSimulationIslands();

        solverInfo.timeStep = timeStep;

        // sorted version of all btTypedConstraint, based on islandId

        if (!constraints.isEmpty()) {
            sortedConstraints.clear();
            sortedConstraints.addAll(constraints);

            //Collections.sort(sortedConstraints, sortConstraintOnIslandPredicate);
            MiscUtil.quickSort(sortedConstraints, sortConstraintOnIslandPredicate);
        }

        int num = sortedConstraints.size();
        solverCallback.init(solverInfo,
                constrainer,
                sortedConstraints,
                num,
                /*,m_stackAlloc*/ intersecter);

        // solve all the constraints for this island
        islands.buildAndProcessIslands(intersecter, collidable, solverCallback);

        constrainer.allSolved(solverInfo /*, m_stackAlloc*/);
    }

    protected void calculateSimulationIslands() {

        islands.updateActivationState(this);

        forEachConstraint((TypedConstraint constraint) -> {
            Dynamic colObj0 = constraint.getRigidBodyA();
            if (colObj0 == null || !colObj0.isActive() || colObj0.isStaticOrKinematicObject())
                return;

            Dynamic colObj1 = constraint.getRigidBodyB();
            if (colObj1 == null || !colObj1.isActive() || colObj1.isStaticOrKinematicObject())
                return;

            islands.find.unite(colObj0.tag(), colObj1.tag());
        });


        // Store the island id in each body
        islands.storeIslandActivationState(this);
    }

    public void forEachConstraint(Consumer<TypedConstraint> e) {
        constraints.forEach(e);
    }

    protected void integrateTransforms(float timeStep) {

        v3 tmp = new v3();
        Transform predictedTrans = new Transform();
        SphereShape tmpSphere = new SphereShape(1);

        for (int i = 0, collidableSize = collidable.size(); i < collidableSize; i++) {
            Collidable colObj = collidable.get(i);
            Dynamic body = ifDynamic(colObj);
            if (body != null) {
                body.setHitFraction(1f);

                if (body.isActive() && (!body.isStaticOrKinematicObject())) {
                    body.predictIntegratedTransform(timeStep, predictedTrans);

                    Transform BW = body.transform;

                    tmp.sub(predictedTrans, BW);
                    float squareMotion = tmp.lengthSquared();

                    float motionThresh = body.getCcdSquareMotionThreshold();

                    if (motionThresh != 0f && motionThresh < squareMotion) {

                        if (body.shape().isConvex()) {
                            BulletStats.gNumClampedCcdMotions++;

                            ClosestNotMeConvexResultCallback sweepResults = new ClosestNotMeConvexResultCallback(body, BW, predictedTrans, broadphase.getOverlappingPairCache(), intersecter);
                            //ConvexShape convexShape = (ConvexShape)body.getCollisionShape();


                            tmpSphere.setRadius(body.getCcdSweptSphereRadius()); //btConvexShape* convexShape = static_cast<btConvexShape*>(body->getCollisionShape());

                            Broadphasing bph = body.broadphase;
                            sweepResults.collisionFilterGroup = bph.collisionFilterGroup;
                            sweepResults.collisionFilterMask = bph.collisionFilterMask;

                            convexSweepTest(tmpSphere, BW, predictedTrans, sweepResults);
                            // JAVA NOTE: added closestHitFraction test to prevent objects being stuck
                            if (sweepResults.hasHit() && (sweepResults.closestHitFraction > 0.0001f)) {
                                body.setHitFraction(sweepResults.closestHitFraction);
                                body.predictIntegratedTransform(timeStep * body.getHitFraction(), predictedTrans);
                                body.setHitFraction(0f);
                                //System.out.printf("clamped integration to hit fraction = %f\n", sweepResults.closestHitFraction);
                            }
                        }

                    }

                    body.proceedToTransform(predictedTrans);
                }
            }
        }
    }

    protected void predictUnconstraintMotion(float timeStep) {

        collidables().forEach((colObj) -> {
            Dynamic body = ifDynamic(colObj);
            if (body != null && !body.isStaticOrKinematicObject() && body.isActive()) {
                body.integrateVelocities(timeStep);
                body.applyDamping(timeStep);
                body.predictIntegratedTransform(timeStep, body.interpolationWorldTransform);
            }
        });
    }

    protected static void startProfiling(float timeStep) {
        //#ifndef BT_NO_PROFILE
        CProfileManager.reset();
        //#endif //BT_NO_PROFILE
    }

    protected static void debugDrawSphere(IDebugDraw debugDrawer, float radius, Transform transform, v3 color) {
        v3 start = new v3(transform);

        v3 xoffs = new v3();
        xoffs.set(radius, 0, 0);
        transform.basis.transform(xoffs);
        v3 yoffs = new v3();
        yoffs.set(0, radius, 0);
        transform.basis.transform(yoffs);
        v3 zoffs = new v3();
        zoffs.set(0, 0, radius);
        transform.basis.transform(zoffs);

        v3 tmp1 = new v3();
        v3 tmp2 = new v3();

        // XY
        tmp1.sub(start, xoffs);
        tmp2.add(start, yoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, yoffs);
        tmp2.add(start, xoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, xoffs);
        tmp2.sub(start, yoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.sub(start, yoffs);
        tmp2.sub(start, xoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);

        // XZ
        tmp1.sub(start, xoffs);
        tmp2.add(start, zoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, zoffs);
        tmp2.add(start, xoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, xoffs);
        tmp2.sub(start, zoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.sub(start, zoffs);
        tmp2.sub(start, xoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);

        // YZ
        tmp1.sub(start, yoffs);
        tmp2.add(start, zoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, zoffs);
        tmp2.add(start, yoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, yoffs);
        tmp2.sub(start, zoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.sub(start, zoffs);
        tmp2.sub(start, yoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
    }

    public static void debugDrawObject(IDebugDraw debugDrawer, Transform worldTransform, CollisionShape shape, v3 color) {
        v3 tmp = new v3();
        v3 tmp2 = new v3();

        // Draw a small simplex at the center of the object
        v3 start = new v3(worldTransform);

        tmp.set(1f, 0f, 0f);

        Matrix3f transformBasis = worldTransform.basis;

        transformBasis.transform(tmp);
        tmp.add(start);
        tmp2.set(1f, 0f, 0f);
        debugDrawer.drawLine(start, tmp, tmp2);

        tmp.set(0f, 1f, 0f);
        transformBasis.transform(tmp);
        tmp.add(start);
        tmp2.set(0f, 1f, 0f);
        debugDrawer.drawLine(start, tmp, tmp2);

        tmp.set(0f, 0f, 1f);
        transformBasis.transform(tmp);
        tmp.add(start);
        tmp2.set(0f, 0f, 1f);
        debugDrawer.drawLine(start, tmp, tmp2);

        // JAVA TODO: debugDrawObject, note that this commented code is from old version, use actual version when implementing

//		if (shape->getShapeType() == COMPOUND_SHAPE_PROXYTYPE)
//		{
//			const btCompoundShape* compoundShape = static_cast<const btCompoundShape*>(shape);
//			for (int i=compoundShape->getNumChildShapes()-1;i>=0;i--)
//			{
//				btTransform childTrans = compoundShape->getChildTransform(i);
//				const btCollisionShape* colShape = compoundShape->getChildShape(i);
//				debugDrawObject(worldTransform*childTrans,colShape,color);
//			}
//
//		} else
//		{
//			switch (shape->getShapeType())
//			{
//
//			case SPHERE_SHAPE_PROXYTYPE:
//				{
//					const btSphereShape* sphereShape = static_cast<const btSphereShape*>(shape);
//					btScalar radius = sphereShape->getMargin();//radius doesn't include the margin, so draw with margin
//
//					debugDrawSphere(radius, worldTransform, color);
//					break;
//				}
//			case MULTI_SPHERE_SHAPE_PROXYTYPE:
//				{
//					const btMultiSphereShape* multiSphereShape = static_cast<const btMultiSphereShape*>(shape);
//
//					for (int i = multiSphereShape->getSphereCount()-1; i>=0;i--)
//					{
//						btTransform childTransform = worldTransform;
//						childTransform.getOrigin() += multiSphereShape->getSpherePosition(i);
//						debugDrawSphere(multiSphereShape->getSphereRadius(i), childTransform, color);
//					}
//
//					break;
//				}
//			case CAPSULE_SHAPE_PROXYTYPE:
//				{
//					const btCapsuleShape* capsuleShape = static_cast<const btCapsuleShape*>(shape);
//
//					btScalar radius = capsuleShape->getRadius();
//					btScalar halfHeight = capsuleShape->getHalfHeight();
//
//					// Draw the ends
//					{
//						btTransform childTransform = worldTransform;
//						childTransform.getOrigin() = worldTransform * btVector3(0,halfHeight,0);
//						debugDrawSphere(radius, childTransform, color);
//					}
//
//					{
//						btTransform childTransform = worldTransform;
//						childTransform.getOrigin() = worldTransform * btVector3(0,-halfHeight,0);
//						debugDrawSphere(radius, childTransform, color);
//					}
//
//					// Draw some additional lines
//					btVector3 start = worldTransform.getOrigin();
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * btVector3(-radius,halfHeight,0),start+worldTransform.getBasis() * btVector3(-radius,-halfHeight,0), color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * btVector3(radius,halfHeight,0),start+worldTransform.getBasis() * btVector3(radius,-halfHeight,0), color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * btVector3(0,halfHeight,-radius),start+worldTransform.getBasis() * btVector3(0,-halfHeight,-radius), color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * btVector3(0,halfHeight,radius),start+worldTransform.getBasis() * btVector3(0,-halfHeight,radius), color);
//
//					break;
//				}
//			case CONE_SHAPE_PROXYTYPE:
//				{
//					const btConeShape* coneShape = static_cast<const btConeShape*>(shape);
//					btScalar radius = coneShape->getRadius();//+coneShape->getMargin();
//					btScalar height = coneShape->getHeight();//+coneShape->getMargin();
//					btVector3 start = worldTransform.getOrigin();
//
//					int upAxis= coneShape->getConeUpIndex();
//
//
//					btVector3	offsetHeight(0,0,0);
//					offsetHeight[upAxis] = height * btScalar(0.5);
//					btVector3	offsetRadius(0,0,0);
//					offsetRadius[(upAxis+1)%3] = radius;
//					btVector3	offset2Radius(0,0,0);
//					offset2Radius[(upAxis+2)%3] = radius;
//
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight+offsetRadius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight-offsetRadius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight+offset2Radius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight-offset2Radius),color);
//
//
//
//					break;
//
//				}
//			case CYLINDER_SHAPE_PROXYTYPE:
//				{
//					const btCylinderShape* cylinder = static_cast<const btCylinderShape*>(shape);
//					int upAxis = cylinder->getUpAxis();
//					btScalar radius = cylinder->getRadius();
//					btScalar halfHeight = cylinder->getHalfExtentsWithMargin()[upAxis];
//					btVector3 start = worldTransform.getOrigin();
//					btVector3	offsetHeight(0,0,0);
//					offsetHeight[upAxis] = halfHeight;
//					btVector3	offsetRadius(0,0,0);
//					offsetRadius[(upAxis+1)%3] = radius;
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight+offsetRadius),start+worldTransform.getBasis() * (-offsetHeight+offsetRadius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight-offsetRadius),start+worldTransform.getBasis() * (-offsetHeight-offsetRadius),color);
//					break;
//				}
//			default:
//				{
//
//					if (shape->isConcave())
//					{
//						btConcaveShape* concaveMesh = (btConcaveShape*) shape;
//
//						//todo pass camera, for some culling
//						btVector3 aabbMax(btScalar(1e30),btScalar(1e30),btScalar(1e30));
//						btVector3 aabbMin(btScalar(-1e30),btScalar(-1e30),btScalar(-1e30));
//
//						DebugDrawcallback drawCallback(getDebugDrawer(),worldTransform,color);
//						concaveMesh->processAllTriangles(&drawCallback,aabbMin,aabbMax);
//
//					}
//
//					if (shape->getShapeType() == CONVEX_TRIANGLEMESH_SHAPE_PROXYTYPE)
//					{
//						btConvexTriangleMeshShape* convexMesh = (btConvexTriangleMeshShape*) shape;
//						//todo: pass camera for some culling
//						btVector3 aabbMax(btScalar(1e30),btScalar(1e30),btScalar(1e30));
//						btVector3 aabbMin(btScalar(-1e30),btScalar(-1e30),btScalar(-1e30));
//						//DebugDrawcallback drawCallback;
//						DebugDrawcallback drawCallback(getDebugDrawer(),worldTransform,color);
//						convexMesh->getMeshInterface()->InternalProcessAllTriangles(&drawCallback,aabbMin,aabbMax);
//					}
//
//
//					/// for polyhedral shapes
//					if (shape->isPolyhedral())
//					{
//						btPolyhedralConvexShape* polyshape = (btPolyhedralConvexShape*) shape;
//
//						int i;
//						for (i=0;i<polyshape->getNumEdges();i++)
//						{
//							btPoint3 a,b;
//							polyshape->getEdge(i,a,b);
//							btVector3 wa = worldTransform * a;
//							btVector3 wb = worldTransform * b;
//							getDebugDrawer()->drawLine(wa,wb,color);
//
//						}
//
//
//					}
//				}
//			}
//		}
    }


    public void addAnimation(Animated a) {
        animations.add(a);
    }

    private float curDT;

    public final void updateAnimations() {
        animations.removeIf(this::updateAnimation);
    }

    private boolean updateAnimation(Animated animated) {
        try {
            return !animated.animate(curDT); //invert for the 'removeIf'
        } catch (Throwable e) {
            e.printStackTrace();
            return true;
        }
    }

    public String summary() {
        return ("collidables=" + collidable.size() + " pairs=" + pairs().size());
    }


    public void removeAnimation(Animated a) {
        animations.remove(a);
    }

    protected List<Collidable> getCollidable() {
        return collidable;
    }

    private static class InplaceSolverIslandCallback extends Islands.IslandCallback {
        public ContactSolverInfo solverInfo;
        public Constrainer solver;
        public FasterList<TypedConstraint> sortedConstraints;
        public int numConstraints;
        //public StackAlloc* m_stackAlloc;
        public Intersecter intersecter;

        public void init(ContactSolverInfo solverInfo, Constrainer solver, FasterList<TypedConstraint> sortedConstraints, int numConstraints, Intersecter intersecter) {
            this.solverInfo = solverInfo;
            this.solver = solver;
            this.sortedConstraints = sortedConstraints;
            this.numConstraints = numConstraints;

            this.intersecter = intersecter;
        }

        @Override
        public void processIsland(Collection<Collidable> bodies, FasterList<PersistentManifold> manifolds, int manifolds_offset, int numManifolds, int islandId) {

            FasterList<TypedConstraint> sc = this.sortedConstraints;
            if (islandId < 0) {
                // we don't split islands, so all constraints/contact manifolds/bodies are passed into the solver regardless the island id
                solver.solveGroup(bodies, bodies.size(), manifolds, manifolds_offset, numManifolds, sc, 0, numConstraints, solverInfo/*,m_stackAlloc*/, intersecter);
            } else {
                // also add all non-contact constraints/joints for this island
                //ObjectArrayList<TypedConstraint> startConstraint = null;
                int startConstraint_idx = -1;
                int numCurConstraints = 0;
                int i;

                // find the first constraint for this island
                for (i = 0; i < numConstraints; i++) {
                    //return array[index];
                    if (getConstraintIslandId(sc.get(i)) == islandId) {
                        //startConstraint = &m_sortedConstraints[i];
                        //startConstraint = sortedConstraints.subList(i, sortedConstraints.size());
                        startConstraint_idx = i;
                        break;
                    }
                }
                // count the number of constraints in this island
                for (; i < numConstraints; i++) {
                    //return array[index];
                    if (getConstraintIslandId(sc.get(i)) == islandId) {
                        numCurConstraints++;
                    }
                }

                // only call solveGroup if there is some work: avoid virtual function call, its overhead can be excessive
                if ((numManifolds + numCurConstraints) > 0) {
                    solver.solveGroup(bodies, bodies.size(), manifolds, manifolds_offset, numManifolds, sc, startConstraint_idx, numCurConstraints, solverInfo/*,m_stackAlloc*/, intersecter);
                }
            }
        }
    }

    private static class ClosestNotMeConvexResultCallback extends ClosestConvexResultCallback {
        private final Collidable me;
        private final static float allowedPenetration = 0;
        private final OverlappingPairCache pairCache;
        private final Intersecter intersecter;

        public ClosestNotMeConvexResultCallback(Collidable me, v3 fromA, v3 toA, OverlappingPairCache pairCache, Intersecter intersecter) {
            super(fromA, toA);
            this.me = me;
            this.pairCache = pairCache;
            this.intersecter = intersecter;
        }

        @Override
        public float addSingleResult(LocalConvexResult convexResult, boolean normalInWorldSpace) {
            if (convexResult.hitCollidable == me) {
                return 1f;
            }

            v3 linVelA = new v3(), linVelB = new v3();
            linVelA.sub(convexToWorld, convexFromWorld);
            linVelB.set(0f, 0f, 0f);//toB.getOrigin()-fromB.getOrigin();

            v3 relativeVelocity = new v3();
            relativeVelocity.sub(linVelA, linVelB);
            // don't report time of impact for motion away from the contact normal (or causes minor penetration)
            if (convexResult.hitNormalLocal.dot(relativeVelocity) >= -allowedPenetration) {
                return 1f;
            } else {
                return super.addSingleResult(convexResult, normalInWorldSpace);
            }
        }

        @Override
        public boolean needsCollision(Broadphasing proxy0) {
            // don't collide with itself
            if (proxy0.data == me) {
                return false;
            }

            // don't do CCD when the collision filters are not matching
            if (!super.needsCollision(proxy0)) {
                return false;
            }

            Collidable otherObj = proxy0.data;

            // call needsResponse, see http://code.google.com/p/bullet/issues/detail?id=179
            if (intersecter.needsResponse(me, otherObj)) {
                // don't do CCD when there are already contact points (touching contact/penetration)
                BroadphasePair collisionPair = pairCache.findPair(me.broadphase, proxy0);
                if (collisionPair != null) {
                    if (collisionPair.algorithm != null) {
                        //manifoldArray.resize(0);
                        OArrayList<PersistentManifold> manifoldArray = new OArrayList<>();
                        collisionPair.algorithm.getAllContactManifolds(manifoldArray);
                        for (int j = 0; j < manifoldArray.size(); j++) {
                            if (manifoldArray.get(j).numContacts() > 0) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
    }
}
