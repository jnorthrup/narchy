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

import jcog.list.FasterList;
import jcog.util.Flip;
import org.jetbrains.annotations.Nullable;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.collision.Islands;
import spacegraph.space3d.phys.collision.broad.*;
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.constraint.BroadConstraint;
import spacegraph.space3d.phys.constraint.TypedConstraint;
import spacegraph.space3d.phys.math.*;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.shape.SphereShape;
import spacegraph.space3d.phys.solve.Constrainer;
import spacegraph.space3d.phys.solve.ContactSolverInfo;
import spacegraph.space3d.phys.solve.SequentialImpulseConstrainer;
import spacegraph.space3d.phys.util.OArrayList;
import spacegraph.util.math.Matrix3f;
import spacegraph.util.math.v3;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static spacegraph.space3d.phys.Body3D.ifDynamic;
import static spacegraph.space3d.phys.Collidable.ISLAND_SLEEPING;
import static spacegraph.util.math.v3.v;

/**
 * DynamicsWorld is the interface class for several dynamics implementation,
 * basic, discrete, parallel, and continuous etc.
 *
 * @author jezek2
 */
public class Dynamics3D<X> extends Collisions<X> {

    private final Constrainer constrainer;
    private final Islands islands;
    private final List<TypedConstraint> constraints = new FasterList();
    @Nullable
    private v3 gravity;


    private final Flip<List<Collidable>> coll = new Flip(FasterList::new);
    private List<Collidable> collidable = coll.read();

    public final FasterList<BroadConstraint> broadConstraints = new FasterList<>(0);
    private final FasterList<TypedConstraint> sortedConstraints = new FasterList<>(0);
    private final InplaceSolverIslandCallback solverCallback = new InplaceSolverIslandCallback();

    private final ContactSolverInfo solverInfo = new ContactSolverInfo();

    
    @Deprecated protected float localTime = 1f / 60f;
    private boolean ownsIslandManager;
    private boolean ownsConstrainer;



    private final Iterable<Spatial<X>> spatials;


    public Dynamics3D(Intersecter intersecter, Broadphase broadphase, Iterable<Spatial<X>> spatials) {
        this(intersecter, broadphase, spatials, null);
    }

    private Dynamics3D(Intersecter intersecter, Broadphase broadphase, Iterable<Spatial<X>> spatials, Constrainer constrainer) {
        super(intersecter, broadphase);
        this.spatials = spatials;
        islands = new Islands();
        ownsIslandManager = true;
        if (constrainer == null) {
            this.constrainer = new SequentialImpulseConstrainer();
            this.ownsConstrainer = true;
        } else {
            this.constrainer = constrainer;
            this.ownsConstrainer = false;
        }

    }

    public static Body3D newBody(float mass, CollisionShape shape, Transform t, int group, int mask) {

        Body3D body = new Body3D(mass, t, shape);
        body.group = (short) group;
        body.mask = (short) mask;
        if (mass != 0f) { 
            shape.calculateLocalInertia(mass, v());
        }

        return body;
    }


    public final int update(float dt, int maxSubSteps) {
        return update(dt, maxSubSteps, dt);
    }

    private void synchronizeMotionStates(boolean clear) {


        for (Collidable ccc : collidable) {
            Body3D body = ifDynamic(ccc);
            if (body == null) {
                continue;
            }


            if (clear) {
                body.clearForces();
            }
        }


    }

    private int update(float timeStep, int maxSubSteps, float fixedTimeStep) {

        BulletGlobals.the.set(this);


        try {
            int numSimulationSubSteps = 0;

            if (maxSubSteps != 0) {
                
                localTime += timeStep;
                if (localTime >= fixedTimeStep) {
                    numSimulationSubSteps = Math.round(localTime / fixedTimeStep);
                    localTime -= numSimulationSubSteps * fixedTimeStep;
                }
            } else {
                
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


            updateObjects(fixedTimeStep);

            if (numSimulationSubSteps != 0) {

                
                int clampedSimulationSteps = Math.min(numSimulationSubSteps, maxSubSteps);

                for (int i = 0; i < clampedSimulationSteps; i++) {
                    internalSingleStepSimulation(fixedTimeStep);
                    synchronizeMotionStates(i == clampedSimulationSteps - 1);
                }
            }

            

            CProfileManager.incrementFrameCounter();

            return numSimulationSubSteps;
        } catch (Throwable t) {
            t.printStackTrace();
            return 1;
        } /*finally {
            

            
        }*/
    }



    private void updateObjects(float dt) {

        List<Collidable> nextCollidables = coll.write();
        nextCollidables.clear();

        final short[] i = {0};
        spatials.forEach((s) -> {


            s.order = i[0]++;

            s.update(this);

            s.forEachBody(c -> {

                Body3D d = ifDynamic(c);
                if (d != null) {

                    on(d);

                    nextCollidables.add(d);

                    if (d.getActivationState() != Collidable.ISLAND_SLEEPING)
                        d.saveKinematicState(dt); 

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









        });

        


        
        this.collidable = coll.commit();

    }






    @Override
    public final List<Collidable> collidables() {
        return collidable;
    }








































































































    public final void addConstraint(TypedConstraint constraint) {
        addConstraint(constraint, false);
    }


    private void updateActivationState(float timeStep) {

        
		float deactivationTime = isDeactivationDisabled() ? 0 : getDeactivationTime();

        collidable.forEach(colObj -> {
            Body3D body = ifDynamic(colObj);
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


    private static final Comparator<TypedConstraint> sortConstraintOnIslandPredicate = (lhs, rhs) ->
            (lhs == rhs) ? 0
                    :
                    ((getConstraintIslandId(lhs) < getConstraintIslandId(rhs)) ? -1 : +1);

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

    private void solveBroadConstraints(float timeStep) {
        for (BroadConstraint b : broadConstraints) {
            b.solve(broadphase, collidable, timeStep);
        }
    }

    public void setGravity(@Nullable v3 gravity) {
        this.gravity = gravity;
    }






    private static int getConstraintIslandId(TypedConstraint lhs) {
        int rcolObj0 = lhs.getRigidBodyA().tag();
        int rcolObj1 = lhs.getRigidBodyB().tag();
        return rcolObj0 >= 0 ? rcolObj0 : rcolObj1;
    }

    /**
     * solve contact and other joint constraints
     */
    private void solveConstraints(float timeStep, ContactSolverInfo solverInfo) {

        calculateSimulationIslands();

        solverInfo.timeStep = timeStep;

        

        if (!constraints.isEmpty()) {
            sortedConstraints.clear();
            sortedConstraints.addAll(constraints);

            
            MiscUtil.quickSort(sortedConstraints, sortConstraintOnIslandPredicate);
        }

        int num = sortedConstraints.size();
        solverCallback.init(solverInfo,
                constrainer,
                sortedConstraints,
                num,
                /*,m_stackAlloc*/ intersecter);

        
        islands.buildAndProcessIslands(intersecter, collidable, solverCallback);

        constrainer.allSolved(solverInfo /*, m_stackAlloc*/);
    }

    private void calculateSimulationIslands() {

        islands.updateActivationState(this);

        forEachConstraint((TypedConstraint constraint) -> {
            Body3D colObj0 = constraint.getRigidBodyA();
            if (colObj0 == null || !colObj0.isActive() || colObj0.isStaticOrKinematicObject())
                return;

            Body3D colObj1 = constraint.getRigidBodyB();
            if (colObj1 == null || !colObj1.isActive() || colObj1.isStaticOrKinematicObject())
                return;

            islands.find.unite(colObj0.tag(), colObj1.tag());
        });


        
        islands.storeIslandActivationState(this);
    }

    private void forEachConstraint(Consumer<TypedConstraint> e) {
        constraints.forEach(e);
    }

    private void integrateTransforms(float timeStep) {

        v3 tmp = new v3();
        Transform predictedTrans = new Transform();
        SphereShape tmpSphere = new SphereShape(1);

        for (Collidable colObj : collidable) {
            Body3D body = ifDynamic(colObj);
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


                            tmpSphere.setRadius(body.getCcdSweptSphereRadius());

                            Broadphasing bph = body.broadphase;
                            sweepResults.collisionFilterGroup = bph.collisionFilterGroup;
                            sweepResults.collisionFilterMask = bph.collisionFilterMask;

                            convexSweepTest(tmpSphere, BW, predictedTrans, sweepResults);

                            if (sweepResults.hasHit() && (sweepResults.closestHitFraction > 0.0001f)) {
                                body.setHitFraction(sweepResults.closestHitFraction);
                                body.predictIntegratedTransform(timeStep * body.getHitFraction(), predictedTrans);
                                body.setHitFraction(0f);

                            }
                        }

                    }

                    body.proceedToTransform(predictedTrans);
                }
            }
        }
    }

    private void predictUnconstraintMotion(float timeStep) {

        collidables().forEach((colObj) -> {
            Body3D body = ifDynamic(colObj);
            if (body != null && !body.isStaticOrKinematicObject() && body.isActive()) {
                body.integrateVelocities(timeStep);
                body.applyDamping(timeStep);
                body.predictIntegratedTransform(timeStep, body.interpolationWorldTransform);
            }
        });
    }

    protected static void startProfiling(float timeStep) {
        
        CProfileManager.reset();
        
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

        





























































































































































    }



    public String summary() {
        return ("collidables=" + collidable.size() + " pairs=" + pairs().size());
    }


    protected List<Collidable> getCollidable() {
        return collidable;
    }

    private static class InplaceSolverIslandCallback extends Islands.IslandCallback {
        ContactSolverInfo solverInfo;
        Constrainer solver;
        FasterList<TypedConstraint> sortedConstraints;
        int numConstraints;
        
        Intersecter intersecter;

        void init(ContactSolverInfo solverInfo, Constrainer solver, FasterList<TypedConstraint> sortedConstraints, int numConstraints, Intersecter intersecter) {
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
                
                solver.solveGroup(bodies, bodies.size(), manifolds, manifolds_offset, numManifolds, sc, 0, numConstraints, solverInfo/*,m_stackAlloc*/, intersecter);
            } else {
                
                
                int startConstraint_idx = -1;
                int numCurConstraints = 0;
                int i;

                
                for (i = 0; i < numConstraints; i++) {
                    
                    if (getConstraintIslandId(sc.get(i)) == islandId) {
                        
                        
                        startConstraint_idx = i;
                        break;
                    }
                }
                
                for (; i < numConstraints; i++) {
                    
                    if (getConstraintIslandId(sc.get(i)) == islandId) {
                        numCurConstraints++;
                    }
                }

                
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

        ClosestNotMeConvexResultCallback(Collidable me, v3 fromA, v3 toA, OverlappingPairCache pairCache, Intersecter intersecter) {
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
            linVelB.set(0f, 0f, 0f);

            v3 relativeVelocity = new v3();
            relativeVelocity.sub(linVelA, linVelB);
            
            if (convexResult.hitNormalLocal.dot(relativeVelocity) >= -allowedPenetration) {
                return 1f;
            } else {
                return super.addSingleResult(convexResult, normalInWorldSpace);
            }
        }

        @Override
        public boolean needsCollision(Broadphasing proxy0) {
            
            if (proxy0.data == me) {
                return false;
            }

            
            if (!super.needsCollision(proxy0)) {
                return false;
            }

            Collidable otherObj = proxy0.data;

            
            if (intersecter.needsResponse(me, otherObj)) {
                
                BroadphasePair collisionPair = pairCache.findPair(me.broadphase, proxy0);
                if (collisionPair != null) {
                    if (collisionPair.algorithm != null) {
                        
                        OArrayList<PersistentManifold> manifoldArray = new OArrayList<>();
                        collisionPair.algorithm.getAllContactManifolds(manifoldArray);
                        for (PersistentManifold aManifoldArray : manifoldArray) {
                            if (aManifoldArray.numContacts() > 0) {
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
