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

import org.jetbrains.annotations.NotNull;
import spacegraph.math.Matrix3f;
import spacegraph.math.Quat4f;
import spacegraph.math.v3;
import spacegraph.phys.collision.broad.*;
import spacegraph.phys.collision.narrow.*;
import spacegraph.phys.math.AabbUtil2;
import spacegraph.phys.math.Transform;
import spacegraph.phys.math.TransformUtil;
import spacegraph.phys.math.VectorUtil;
import spacegraph.phys.shape.*;

import java.util.List;

import static spacegraph.math.v3.v;

/**
 * CollisionWorld is interface and container for the collision detection.
 *
 * @author jezek2
 */
public abstract class Collisions<X> extends BulletGlobals {
    public static final float maxAABBLength = 1e12f;


    /**
     * holds spatials which have not been added to 'objects' yet (beginning of next cycle)
     */
    //protected FasterList<Spatial<X>> pendingAdd = $.newArrayList();

    @NotNull
    public final Intersecter intersecter;
    @NotNull
    protected final DispatcherInfo dispatchInfo = new DispatcherInfo();
    //protected btStackAlloc*	m_stackAlloc;
    @NotNull
    protected final Broadphase broadphase;

    /**
     * This constructor doesn't own the dispatcher and paircache/broadphase.
     */
    protected Collisions(Intersecter intersecter, Broadphase broadphase) {
        this.intersecter = intersecter;
        this.broadphase = broadphase;
    }

//	public void destroy() {
//		// clean up remaining objects
//		for (int i = 0; i < objects.size(); i++) {
//			//return array[index];
//			Collidable collidable = objects.get(i);
//
//			Broadphasing bp = collidable.broadphase();
//			if (bp != null) {
//				//
//				// only clear the cached algorithms
//				//
//				broadphasePairCache.getOverlappingPairCache().cleanProxyFromPairs(bp, intersecter);
//				broadphasePairCache.destroyProxy(bp, intersecter);
//			}
//		}
//	}


    /**
     * list of current colidables in the engine, aggregated from the spatials that are present
     */
    abstract public List<Collidable> collidables();

    //abstract public void forEachCollidable(IntObjectProcedure<Collidable<X>> each);


    protected boolean on(Collidable c) {
        // check that the object isn't already added
        //assert (!collisionObjects.contains(collisionObject));

        Broadphasing currentBroadphase = c.broadphase;
        if (currentBroadphase == null) {

            v3 minAabb = new v3();
            v3 maxAabb = new v3();

            CollisionShape shape = c.shape();
            shape.getAabb(c.transform, minAabb, maxAabb);

            c.broadphase(broadphase.createProxy(
                    minAabb,
                    maxAabb,
                    shape.getShapeType(),
                    c,
                    c.group,
                    c.mask,
                    intersecter, null));
            return true;
        }

        return false;

    }


    protected void solveCollisions() {
        //BulletStats.pushProfile("performDiscreteCollisionDetection");
        //DispatcherInfo dispatchInfo = getDispatchInfo();

        updateAabbs();

        //BulletStats.pushProfile("calculateOverlappingPairs");
            broadphase.update(intersecter);



        //BulletStats.pushProfile("dispatchAllCollisionPairs");
        intersecter.dispatchAllCollisionPairs(broadphase.getOverlappingPairCache(), dispatchInfo, this.intersecter);
    }

//	public void removeIf(Predicate<Collidable<X>> removalCondition) {
//		objects.removeIf((c -> {
//			if (removalCondition.test(c)) {
//				removing(c);
//				return true;
//			}
//			return false;
//		}));
//	}

//	public final void removeBody(Collidable collidable) {
//		removing(collidable);
//	}


    public OverlappingPairCache pairs() {
        return broadphase.getOverlappingPairCache();
    }

    public DispatcherInfo getDispatchInfo() {
        return dispatchInfo;
    }


    // JAVA NOTE: ported from 2.74, missing contact threshold stuff
    protected void updateSingleAabb(Collidable colObj) {
        v3 minAabb = new v3(), maxAabb = new v3();
        v3 tmp = new v3();
        Transform tmpTrans = new Transform();

        colObj.shape().getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb);
        // need to increase the aabb for contact thresholds
        v3 contactThreshold = new v3();

        float bt = getContactBreakingThreshold();
        contactThreshold.set(bt, bt, bt);

        minAabb.sub(contactThreshold);
        maxAabb.add(contactThreshold);

        Broadphase bp = broadphase;

        // moving objects should be moderately sized, probably something wrong if not
        tmp.sub(maxAabb, minAabb); // TODO: optimize
        if (colObj.isStaticObject() || (tmp.lengthSquared() < maxAABBLength)) {
            Broadphasing broadphase = colObj.broadphase;
            if (broadphase == null)
                throw new RuntimeException();
            bp.setAabb(broadphase, minAabb, maxAabb, intersecter);
        } else {
            // something went wrong, investigate
            // this assert is unwanted in 3D modelers (danger of loosing work)
            colObj.setActivationState(Collidable.DISABLE_SIMULATION);

//			if (updateAabbs_reportMe && debugDrawer != null) {
//				updateAabbs_reportMe = false;
//				debugDrawer.reportErrorWarning("Overflow in AABB, object removed from simulation");
//				debugDrawer.reportErrorWarning("If you can reproduce this, please email bugs@continuousphysics.com\n");
//				debugDrawer.reportErrorWarning("Please include above information, your Platform, version of OS.\n");
//				debugDrawer.reportErrorWarning("Thanks.\n");
//			}
        }
    }

    public void updateAabbs() {
//		BulletStats.pushProfile("updateAabbs");
//		try {
        collidables().forEach(this::updateAabbsIfActive);
//		}
//		finally {
//			BulletStats.popProfile();
//		}
    }

    private void updateAabbsIfActive(Collidable<X> colObj) {
        // only update aabb of active objects
        if (colObj.isActive()) {
            updateSingleAabb(colObj);
        }
    }


    final static ConvexShape pointShape = (ConvexShape) new SphereShape(0f).setMargin(0);

    public static void rayTestSingle(Transform rayFromTrans, Transform rayToTrans,
                                     Collidable collidable,
                                     CollisionShape collisionShape,
                                     Transform colObjWorldTransform,
                                     VoronoiSimplexSolver simplexSolver,
                                     RayResultCallback resultCallback) {

        if (collisionShape.isConvex()) {
            ConvexCast.CastResult castResult = new ConvexCast.CastResult();
            castResult.fraction = resultCallback.closestHitFraction;

            ConvexShape convexShape = (ConvexShape) collisionShape;

            //#define USE_SUBSIMPLEX_CONVEX_CAST 1
            //#ifdef USE_SUBSIMPLEX_CONVEX_CAST
            SubsimplexConvexCast convexCaster = new SubsimplexConvexCast(pointShape, convexShape, simplexSolver);
            //#else
            //btGjkConvexCast	convexCaster(castShape,convexShape,&simplexSolver);
            //btContinuousConvexCollision convexCaster(castShape,convexShape,&simplexSolver,0);
            //#endif //#USE_SUBSIMPLEX_CONVEX_CAST

            if (convexCaster.calcTimeOfImpact(rayFromTrans, rayToTrans, colObjWorldTransform, colObjWorldTransform, castResult)) {
                //add hit
                if (castResult.normal.lengthSquared() > 0.0001f) {
                    if (castResult.fraction < resultCallback.closestHitFraction) {
                        //#ifdef USE_SUBSIMPLEX_CONVEX_CAST
                        //rotate normal into worldspace
                        rayFromTrans.basis.transform(castResult.normal);
                        //#endif //USE_SUBSIMPLEX_CONVEX_CAST

                        castResult.normal.normalize();
                        LocalRayResult localRayResult = new LocalRayResult(
                                collidable,
                                null,
                                castResult.normal,
                                castResult.fraction);

                        boolean normalInWorldSpace = true;
                        resultCallback.addSingleResult(localRayResult, normalInWorldSpace);
                    }
                }
            }
        } else {
            if (collisionShape.isConcave()) {
                if (collisionShape.getShapeType() == BroadphaseNativeType.TRIANGLE_MESH_SHAPE_PROXYTYPE) {
                    // optimized version for BvhTriangleMeshShape
                    BvhTriangleMeshShape triangleMesh = (BvhTriangleMeshShape) collisionShape;
                    Transform worldTocollisionObject = new Transform();
                    worldTocollisionObject.inverse(colObjWorldTransform);
                    v3 rayFromLocal = new v3(rayFromTrans);
                    worldTocollisionObject.transform(rayFromLocal);
                    v3 rayToLocal = new v3(rayToTrans);
                    worldTocollisionObject.transform(rayToLocal);

                    BridgeTriangleRaycastCallback rcb = new BridgeTriangleRaycastCallback(rayFromLocal, rayToLocal, resultCallback, collidable, triangleMesh);
                    rcb.hitFraction = resultCallback.closestHitFraction;
                    triangleMesh.performRaycast(rcb, rayFromLocal, rayToLocal);
                } else {
                    ConcaveShape triangleMesh = (ConcaveShape) collisionShape;

                    Transform worldTocollisionObject = new Transform();
                    worldTocollisionObject.inverse(colObjWorldTransform);

                    v3 rayFromLocal = new v3(rayFromTrans);
                    worldTocollisionObject.transform(rayFromLocal);
                    v3 rayToLocal = new v3(rayToTrans);
                    worldTocollisionObject.transform(rayToLocal);

                    BridgeTriangleRaycastCallback rcb = new BridgeTriangleRaycastCallback(rayFromLocal, rayToLocal, resultCallback, collidable, triangleMesh);
                    rcb.hitFraction = resultCallback.closestHitFraction;

                    v3 rayAabbMinLocal = new v3(rayFromLocal);
                    VectorUtil.setMin(rayAabbMinLocal, rayToLocal);
                    v3 rayAabbMaxLocal = new v3(rayFromLocal);
                    VectorUtil.setMax(rayAabbMaxLocal, rayToLocal);

                    triangleMesh.processAllTriangles(rcb, rayAabbMinLocal, rayAabbMaxLocal);
                }
            } else {
                // todo: use AABB tree or other BVH acceleration structure!
                if (collisionShape.isCompound()) {
                    CompoundShape compoundShape = (CompoundShape) collisionShape;
                    int i = 0;
                    Transform childTrans = new Transform();
                    for (i = 0; i < compoundShape.getNumChildShapes(); i++) {
                        compoundShape.getChildTransform(i, childTrans);
                        CollisionShape childCollisionShape = compoundShape.getChildShape(i);
                        Transform childWorldTrans = new Transform(colObjWorldTransform);
                        childWorldTrans.mul(childTrans);
                        // replace collision shape so that callback can determine the triangle
                        CollisionShape saveCollisionShape = collidable.shape();
                        collidable.internalSetTemporaryCollisionShape(childCollisionShape);

                        simplexSolver.reset();
                        rayTestSingle(rayFromTrans, rayToTrans,
                                collidable,
                                childCollisionShape,
                                childWorldTrans,
                                simplexSolver,
                                resultCallback);

                        // restore
                        collidable.internalSetTemporaryCollisionShape(saveCollisionShape);
                    }
                }
            }
        }
    }

    private static class BridgeTriangleConvexcastCallback extends TriangleConvexcastCallback {
        public ConvexResultCallback resultCallback;
        public Collidable collidable;
        public ConcaveShape triangleMesh;
        public boolean normalInWorldSpace;

        public BridgeTriangleConvexcastCallback(ConvexShape castShape, Transform from, Transform to, ConvexResultCallback resultCallback, Collidable collidable, ConcaveShape triangleMesh, Transform triangleToWorld) {
            super(castShape, from, to, triangleToWorld, triangleMesh.getMargin());
            this.resultCallback = resultCallback;
            this.collidable = collidable;
            this.triangleMesh = triangleMesh;
        }

        @Override
        public float reportHit(v3 hitNormalLocal, v3 hitPointLocal, float hitFraction, int partId, int triangleIndex) {
            LocalShapeInfo shapeInfo = new LocalShapeInfo();
            shapeInfo.shapePart = partId;
            shapeInfo.triangleIndex = triangleIndex;
            if (hitFraction <= resultCallback.closestHitFraction) {
                LocalConvexResult convexResult = new LocalConvexResult(collidable, shapeInfo, hitNormalLocal, hitPointLocal, hitFraction);
                return resultCallback.addSingleResult(convexResult, normalInWorldSpace);
            }
            return hitFraction;
        }
    }

    /**
     * objectQuerySingle performs a collision detection query and calls the resultCallback. It is used internally by rayTest.
     */
    public static void objectQuerySingle(ConvexShape castShape, Transform convexFromTrans, Transform convexToTrans, Collidable collidable, CollisionShape collisionShape, Transform colObjWorldTransform, ConvexResultCallback resultCallback, float allowedPenetration) {
        if (collisionShape.isConvex()) {
            ConvexCast.CastResult castResult = new ConvexCast.CastResult();
            castResult.allowedPenetration = allowedPenetration;
            castResult.fraction = 1f; // ??

            ConvexShape convexShape = (ConvexShape) collisionShape;
            VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();
            GjkEpaPenetrationDepthSolver gjkEpaPenetrationSolver = new GjkEpaPenetrationDepthSolver();

            // JAVA TODO: should be convexCaster1
            //ContinuousConvexCollision convexCaster1(castShape,convexShape,&simplexSolver,&gjkEpaPenetrationSolver);
            GjkConvexCast convexCaster2 = new GjkConvexCast(castShape, convexShape, simplexSolver);
            //btSubsimplexConvexCast convexCaster3(castShape,convexShape,&simplexSolver);

            ConvexCast castPtr = convexCaster2;

            if (castPtr.calcTimeOfImpact(convexFromTrans, convexToTrans, colObjWorldTransform, colObjWorldTransform, castResult)) {
                // add hit
                if (castResult.normal.lengthSquared() > 0.0001f) {
                    if (castResult.fraction < resultCallback.closestHitFraction) {
                        castResult.normal.normalize();
                        LocalConvexResult localConvexResult = new LocalConvexResult(collidable, null, castResult.normal, castResult.hitPoint, castResult.fraction);

                        boolean normalInWorldSpace = true;
                        resultCallback.addSingleResult(localConvexResult, normalInWorldSpace);
                    }
                }
            }
        } else {
            if (collisionShape.isConcave()) {
                if (collisionShape.getShapeType() == BroadphaseNativeType.TRIANGLE_MESH_SHAPE_PROXYTYPE) {
                    BvhTriangleMeshShape triangleMesh = (BvhTriangleMeshShape) collisionShape;
                    Transform worldTocollisionObject = new Transform();
                    worldTocollisionObject.inverse(colObjWorldTransform);

                    v3 convexFromLocal = new v3();
                    convexFromLocal.set(convexFromTrans);
                    worldTocollisionObject.transform(convexFromLocal);

                    v3 convexToLocal = new v3();
                    convexToLocal.set(convexToTrans);
                    worldTocollisionObject.transform(convexToLocal);

                    // rotation of box in local mesh space = MeshRotation^-1 * ConvexToRotation
                    Transform rotationXform = new Transform();
                    Matrix3f tmpMat = new Matrix3f();
                    tmpMat.mul(worldTocollisionObject.basis, convexToTrans.basis);
                    rotationXform.set(tmpMat);

                    BridgeTriangleConvexcastCallback tccb = new BridgeTriangleConvexcastCallback(castShape, convexFromTrans, convexToTrans, resultCallback, collidable, triangleMesh, colObjWorldTransform);
                    tccb.hitFraction = resultCallback.closestHitFraction;
                    tccb.normalInWorldSpace = true;

                    v3 boxMinLocal = new v3();
                    v3 boxMaxLocal = new v3();
                    castShape.getAabb(rotationXform, boxMinLocal, boxMaxLocal);
                    triangleMesh.performConvexcast(tccb, convexFromLocal, convexToLocal, boxMinLocal, boxMaxLocal);
                } else {
                    ConcaveShape triangleMesh = (ConcaveShape) collisionShape;
                    Transform worldTocollisionObject = new Transform();
                    worldTocollisionObject.inverse(colObjWorldTransform);

                    v3 convexFromLocal = new v3();
                    convexFromLocal.set(convexFromTrans);
                    worldTocollisionObject.transform(convexFromLocal);

                    v3 convexToLocal = new v3();
                    convexToLocal.set(convexToTrans);
                    worldTocollisionObject.transform(convexToLocal);

                    // rotation of box in local mesh space = MeshRotation^-1 * ConvexToRotation
                    Transform rotationXform = new Transform();
                    Matrix3f tmpMat = new Matrix3f();
                    tmpMat.mul(worldTocollisionObject.basis, convexToTrans.basis);
                    rotationXform.set(tmpMat);

                    BridgeTriangleConvexcastCallback tccb = new BridgeTriangleConvexcastCallback(castShape, convexFromTrans, convexToTrans, resultCallback, collidable, triangleMesh, colObjWorldTransform);
                    tccb.hitFraction = resultCallback.closestHitFraction;
                    tccb.normalInWorldSpace = false;
                    v3 boxMinLocal = new v3();
                    v3 boxMaxLocal = new v3();
                    castShape.getAabb(rotationXform, boxMinLocal, boxMaxLocal);

                    v3 rayAabbMinLocal = new v3(convexFromLocal);
                    VectorUtil.setMin(rayAabbMinLocal, convexToLocal);
                    v3 rayAabbMaxLocal = new v3(convexFromLocal);
                    VectorUtil.setMax(rayAabbMaxLocal, convexToLocal);
                    rayAabbMinLocal.add(boxMinLocal);
                    rayAabbMaxLocal.add(boxMaxLocal);
                    triangleMesh.processAllTriangles(tccb, rayAabbMinLocal, rayAabbMaxLocal);
                }
            } else {
                // todo: use AABB tree or other BVH acceleration structure!
                if (collisionShape.isCompound()) {
                    CompoundShape compoundShape = (CompoundShape) collisionShape;
                    for (int i = 0; i < compoundShape.getNumChildShapes(); i++) {
                        Transform childTrans = compoundShape.getChildTransform(i, new Transform());
                        CollisionShape childCollisionShape = compoundShape.getChildShape(i);
                        Transform childWorldTrans = new Transform();
                        childWorldTrans.mul(colObjWorldTransform, childTrans);
                        // replace collision shape so that callback can determine the triangle
                        CollisionShape saveCollisionShape = collidable.shape();
                        collidable.internalSetTemporaryCollisionShape(childCollisionShape);
                        objectQuerySingle(castShape, convexFromTrans, convexToTrans,
                                collidable,
                                childCollisionShape,
                                childWorldTrans,
                                resultCallback, allowedPenetration);
                        // restore
                        collidable.internalSetTemporaryCollisionShape(saveCollisionShape);
                    }
                }
            }
        }
    }

    /**
     * rayTest performs a raycast on all objects in the CollisionWorld, and calls the resultCallback.
     * This allows for several queries: first hit, all hits, any hit, dependent on the value returned by the callback.
     */
    public RayResultCallback rayTest(v3 rayFromWorld, v3 rayToWorld, RayResultCallback resultCallback, VoronoiSimplexSolver simplexSolver) {


        Transform rayFromTrans = new Transform(rayFromWorld);
        Transform rayToTrans = new Transform(rayToWorld);

        // go over all objects, and if the ray intersects their aabb, do a ray-shape query using convexCaster (CCD)
        v3 collisionObjectAabbMin = v(), collisionObjectAabbMax = v();
        float[] hitLambda = new float[1];

        //Transform tmpTrans = new Transform();

        List<Collidable> objs = collidables();
        for (int i = 0, objsSize = objs.size(); i < objsSize; i++) {
            Collidable collidable = objs.get(i);
            // terminate further ray tests, once the closestHitFraction reached zero
            if (resultCallback.closestHitFraction == 0f) {
                break;
            }

            //return array[index];
            if (collidable != null) {

                Broadphasing broadphaseHandle = collidable.broadphase;

                // only perform raycast if filterMask matches
                if (broadphaseHandle != null && resultCallback.needsCollision(broadphaseHandle)) {
                    //RigidcollisionObject* collisionObject = ctrl->GetRigidcollisionObject();
                    CollisionShape shape = collidable.shape();

                    Transform worldTransform = collidable.transform;

                    shape.getAabb(worldTransform, collisionObjectAabbMin, collisionObjectAabbMax);

                    if (!collisionObjectAabbMin.isFinite() || !collisionObjectAabbMax.isFinite())
                        continue;

                    hitLambda[0] = resultCallback.closestHitFraction;

                    if (AabbUtil2.rayAabb(rayFromWorld, rayToWorld, collisionObjectAabbMin, collisionObjectAabbMax, hitLambda)) {

                        simplexSolver.reset();
                        rayTestSingle(rayFromTrans, rayToTrans,
                                collidable,
                                shape,
                                worldTransform,
                                simplexSolver,
                                resultCallback);
                    }
                }
            }

        }

        return resultCallback;
    }

    /**
     * convexTest performs a swept convex cast on all objects in the {@link Collisions}, and calls the resultCallback
     * This allows for several queries: first hit, all hits, any hit, dependent on the value return by the callback.
     */
    public void convexSweepTest(ConvexShape castShape, Transform convexFromWorld, Transform convexToWorld, ConvexResultCallback resultCallback) {
        Transform convexFromTrans = new Transform();
        Transform convexToTrans = new Transform();

        convexFromTrans.set(convexFromWorld);
        convexToTrans.set(convexToWorld);

        v3 castShapeAabbMin = new v3();
        v3 castShapeAabbMax = new v3();

        // Compute AABB that encompasses angular movement
        v3 linVel = new v3();
        v3 angVel = new v3();
        TransformUtil.calculateVelocity(convexFromTrans, convexToTrans, 1f, linVel, angVel);


        {
            Transform R = new Transform();
            R.setIdentity();
            R.setRotation(convexFromTrans.getRotation(new Quat4f()));
            castShape.calculateTemporalAabb(R, linVel, angVel, 1f, castShapeAabbMin, castShapeAabbMax);
        }

        //Transform R = new Transform();
        v3 collisionObjectAabbMin = new v3();
        v3 collisionObjectAabbMax = new v3();
        float[] hitLambda = new float[1];

        // go over all objects, and if the ray intersects their aabb + cast shape aabb,
        // do a ray-shape query using convexCaster (CCD)

        v3 hitNormal = new v3();

        List<Collidable> collidables = collidables();
        for (int i = 0, collidablesSize = collidables.size(); i < collidablesSize; i++) {
            Collidable collidable = collidables.get(i);

            // only perform raycast if filterMask matches
            if (resultCallback.needsCollision(collidable.broadphase)) {
                //RigidcollisionObject* collisionObject = ctrl->GetRigidcollisionObject();
                Transform S = collidable.transform;
                CollisionShape shape = collidable.shape();

                shape.getAabb(S, collisionObjectAabbMin, collisionObjectAabbMax);
                AabbUtil2.aabbExpand(collisionObjectAabbMin, collisionObjectAabbMax, castShapeAabbMin, castShapeAabbMax);

                hitLambda[0] = 1f; // could use resultCallback.closestHitFraction, but needs testing
                hitNormal.zero(); //may not be necessary

                if (AabbUtil2.rayAabb(convexFromWorld, convexToWorld, collisionObjectAabbMin, collisionObjectAabbMax, hitLambda, hitNormal)) {
                    objectQuerySingle(castShape, convexFromTrans, convexToTrans,
                            collidable,
                            shape,
                            S,
                            resultCallback,
                            dispatchInfo.allowedCcdPenetration);
                }
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////

    /**
     * LocalShapeInfo gives extra information for complex shapes.
     * Currently, only btTriangleMeshShape is available, so it just contains triangleIndex and subpart.
     */
    public static class LocalShapeInfo {
        public int shapePart;
        public int triangleIndex;
        //const btCollisionShape*	m_shapeTemp;
        //const btTransform*	m_shapeLocalTransform;
    }

    public static final class LocalRayResult {
        public final Collidable collidable;
        public final LocalShapeInfo localShapeInfo;
        public final v3 hitNormal = new v3();
        public final float hitFraction;

        public LocalRayResult(Collidable collidable, LocalShapeInfo localShapeInfo, v3 hitNormal, float hitFraction) {
            this.collidable = collidable;
            this.localShapeInfo = localShapeInfo;
            this.hitNormal.set(hitNormal);
            this.hitFraction = hitFraction;
        }

        @Override
        public String toString() {
            return "LocalRayResult{" +
                    "collidable=" + collidable +
                    ", localShapeInfo=" + localShapeInfo +
                    ", hitNormalLocal=" + hitNormal +
                    ", hitFraction=" + hitFraction +
                    '}';
        }
    }

    /**
     * RayResultCallback is used to report new raycast results.
     */
    public static abstract class RayResultCallback {
        public float closestHitFraction = 1f;
        public Collidable collidable;
        public short collisionFilterGroup = CollisionFilterGroups.DEFAULT_FILTER;
        public short collisionFilterMask = CollisionFilterGroups.ALL_FILTER;

        public boolean hasHit() {
            return (collidable != null);
        }

        public boolean needsCollision(Broadphasing proxy0) {
            boolean collides = ((proxy0.collisionFilterGroup & collisionFilterMask) & 0xFFFF) != 0;
            collides = collides && ((collisionFilterGroup & proxy0.collisionFilterMask) & 0xFFFF) != 0;
            return collides;
        }

        public abstract float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace);
    }

    public static class LocalConvexResult {
        public Collidable hitCollidable;
        public LocalShapeInfo localShapeInfo;
        public final v3 hitNormalLocal = new v3();
        public final v3 hitPointLocal = new v3();
        public float hitFraction;

        public LocalConvexResult(Collidable hitCollidable, LocalShapeInfo localShapeInfo, v3 hitNormalLocal, v3 hitPointLocal, float hitFraction) {
            this.hitCollidable = hitCollidable;
            this.localShapeInfo = localShapeInfo;
            this.hitNormalLocal.set(hitNormalLocal);
            this.hitPointLocal.set(hitPointLocal);
            this.hitFraction = hitFraction;
        }
    }

    public static abstract class ConvexResultCallback {
        public float closestHitFraction = 1f;
        public short collisionFilterGroup = CollisionFilterGroups.DEFAULT_FILTER;
        public short collisionFilterMask = CollisionFilterGroups.ALL_FILTER;

        public boolean hasHit() {
            return (closestHitFraction < 1f);
        }

        public boolean needsCollision(Broadphasing proxy0) {
            boolean collides = ((proxy0.collisionFilterGroup & collisionFilterMask) & 0xFFFF) != 0;
            collides = collides && ((collisionFilterGroup & proxy0.collisionFilterMask) & 0xFFFF) != 0;
            return collides;
        }

        public abstract float addSingleResult(LocalConvexResult convexResult, boolean normalInWorldSpace);
    }

    public static class ClosestConvexResultCallback extends ConvexResultCallback {
        public final v3 convexFromWorld = new v3(); // used to calculate hitPointWorld from hitFraction
        public final v3 convexToWorld = new v3();
        public final v3 hitNormalWorld = new v3();
        public final v3 hitPointWorld = new v3();
        public Collidable hitCollidable;

        public ClosestConvexResultCallback(v3 convexFromWorld, v3 convexToWorld) {
            this.convexFromWorld.set(convexFromWorld);
            this.convexToWorld.set(convexToWorld);
            this.hitCollidable = null;
        }

        @Override
        public float addSingleResult(LocalConvexResult convexResult, boolean normalInWorldSpace) {
            // caller already does the filter on the m_closestHitFraction
            assert (convexResult.hitFraction <= closestHitFraction);

            closestHitFraction = convexResult.hitFraction;
            hitCollidable = convexResult.hitCollidable;
            if (normalInWorldSpace) {
                hitNormalWorld.set(convexResult.hitNormalLocal);
                if (hitNormalWorld.length() > 2) {
                    System.out.println("CollisionWorld.addSingleResult world " + hitNormalWorld);
                }
            } else {
                // need to transform normal into worldspace
                hitNormalWorld.set(convexResult.hitNormalLocal);
                hitCollidable.getWorldTransform(new Transform()).basis.transform(hitNormalWorld);
                if (hitNormalWorld.length() > 2) {
                    System.out.println("CollisionWorld.addSingleResult world " + hitNormalWorld);
                }
            }

            hitPointWorld.set(convexResult.hitPointLocal);
            return convexResult.hitFraction;
        }
    }

    private static class BridgeTriangleRaycastCallback extends TriangleRaycastCallback {
        public RayResultCallback resultCallback;
        public Collidable collidable;
        public ConcaveShape triangleMesh;

        public BridgeTriangleRaycastCallback(v3 from, v3 to, RayResultCallback resultCallback, Collidable collidable, ConcaveShape triangleMesh) {
            super(from, to);
            this.resultCallback = resultCallback;
            this.collidable = collidable;
            this.triangleMesh = triangleMesh;
        }

        @Override
        public float reportHit(v3 hitNormalLocal, float hitFraction, int partId, int triangleIndex) {
            LocalShapeInfo shapeInfo = new LocalShapeInfo();
            shapeInfo.shapePart = partId;
            shapeInfo.triangleIndex = triangleIndex;

            LocalRayResult rayResult = new LocalRayResult(collidable, shapeInfo, hitNormalLocal, hitFraction);

            boolean normalInWorldSpace = false;
            return resultCallback.addSingleResult(rayResult, normalInWorldSpace);
        }
    }

}
