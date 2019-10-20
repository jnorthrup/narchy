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
import spacegraph.space3d.phys.collision.broad.*;
import spacegraph.space3d.phys.collision.narrow.*;
import spacegraph.space3d.phys.math.AabbUtil2;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.TransformUtil;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.shape.*;
import spacegraph.util.math.Matrix3f;
import spacegraph.util.math.Quat4f;

import java.util.List;

import static jcog.math.v3.v;

/**
 * CollisionWorld is interface and container for the collision detection.
 *
 * @author jezek2
 */
public abstract class Collisions<X> extends BulletGlobals {
    private static final float maxAABBLength = 1e12f;


    /**
     * holds spatials which have not been added to 'objects' yet (beginning of next cycle)
     */
    

    
    public final Intersecter intersecter;
    
    private final DispatcherInfo dispatchInfo = new DispatcherInfo();
    
    
    final Broadphase broadphase;

    /**
     * This constructor doesn't own the dispatcher and paircache/broadphase.
     */
    Collisions(Intersecter intersecter, Broadphase broadphase) {
        this.intersecter = intersecter;
        this.broadphase = broadphase;
    }



















    /**
     * list of current colidables in the engine, aggregated from the spatials that are present
     */
    public abstract List<Collidable> collidables();

    


    boolean on(Collidable c) {


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


    void solveCollisions() {
        
        

        updateAabbs();

        
            broadphase.update(intersecter);



        
        intersecter.dispatchAllCollisionPairs(broadphase.getOverlappingPairCache(), dispatchInfo, this.intersecter);
    }
















    public OverlappingPairCache pairs() {
        return broadphase.getOverlappingPairCache();
    }

    DispatcherInfo getDispatchInfo() {
        return dispatchInfo;
    }


    
    private void updateSingleAabb(Collidable colObj) {
        v3 minAabb = new v3(), maxAabb = new v3();
        v3 tmp = new v3();
        Transform tmpTrans = new Transform();

        colObj.shape().getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb);

        v3 contactThreshold = new v3();

        float bt = getContactBreakingThreshold();
        contactThreshold.set(bt, bt, bt);

        minAabb.sub(contactThreshold);
        maxAabb.add(contactThreshold);

        Broadphase bp = broadphase;

        
        tmp.sub(maxAabb, minAabb); 
        if (colObj.isStaticObject() || (tmp.lengthSquared() < maxAABBLength)) {
            Broadphasing broadphase = colObj.broadphase;
            if (broadphase == null)
                throw new RuntimeException();
            bp.setAabb(broadphase, minAabb, maxAabb, intersecter);
        } else {
            
            
            colObj.setActivationState(Collidable.DISABLE_SIMULATION);








        }
    }

    private void updateAabbs() {


        for (Collidable collidable : collidables()) {
            updateAabbsIfActive(collidable);
        }


    }

    private void updateAabbsIfActive(Collidable<X> colObj) {
        
        if (colObj.isActive()) {
            updateSingleAabb(colObj);
        }
    }


    private static final ConvexShape pointShape = (ConvexShape) new SphereShape(Util.sqrt(Float.MIN_NORMAL)).setMargin(Float.MIN_NORMAL);

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


            SubsimplexConvexCast convexCaster = new SubsimplexConvexCast(pointShape, convexShape, simplexSolver);
            
            
            
            

            if (convexCaster.calcTimeOfImpact(rayFromTrans, rayToTrans, colObjWorldTransform, colObjWorldTransform, castResult)) {
                
                if (castResult.normal.lengthSquared() > Float.MIN_NORMAL) {
                    if (castResult.fraction < resultCallback.closestHitFraction) {
                        
                        
                        rayFromTrans.transform(castResult.normal);
                        

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

                    BvhTriangleMeshShape triangleMesh = (BvhTriangleMeshShape) collisionShape;
                    Transform worldTocollisionObject = new Transform();
                    worldTocollisionObject.invert(colObjWorldTransform);
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
                    worldTocollisionObject.invert(colObjWorldTransform);

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
                
                if (collisionShape.isCompound()) {
                    CompoundShape compoundShape = (CompoundShape) collisionShape;
                    int i = 0;
                    Transform childTrans = new Transform();
                    for (i = 0; i < compoundShape.size(); i++) {
                        compoundShape.getChildTransform(i, childTrans);
                        CollisionShape childCollisionShape = compoundShape.getChildShape(i);
                        Transform childWorldTrans = new Transform(colObjWorldTransform);
                        childWorldTrans.mul(childTrans);

                        CollisionShape saveCollisionShape = collidable.shape();
                        collidable.internalSetTemporaryCollisionShape(childCollisionShape);

                        simplexSolver.reset();
                        rayTestSingle(rayFromTrans, rayToTrans,
                                collidable,
                                childCollisionShape,
                                childWorldTrans,
                                simplexSolver,
                                resultCallback);

                        
                        collidable.internalSetTemporaryCollisionShape(saveCollisionShape);
                    }
                }
            }
        }
    }

    private static class BridgeTriangleConvexcastCallback extends TriangleConvexcastCallback {
        final ConvexResultCallback resultCallback;
        final Collidable collidable;
        final ConcaveShape triangleMesh;
        boolean normalInWorldSpace;

        BridgeTriangleConvexcastCallback(ConvexShape castShape, Transform from, Transform to, ConvexResultCallback resultCallback, Collidable collidable, ConcaveShape triangleMesh, Transform triangleToWorld) {
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
            castResult.fraction = 1f;

            ConvexShape convexShape = (ConvexShape) collisionShape;
            VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();
            GjkEpaPenetrationDepthSolver gjkEpaPenetrationSolver = new GjkEpaPenetrationDepthSolver();


            GjkConvexCast convexCaster2 = new GjkConvexCast(castShape, convexShape, simplexSolver);
            

            ConvexCast castPtr = convexCaster2;

            if (castPtr.calcTimeOfImpact(convexFromTrans, convexToTrans, colObjWorldTransform, colObjWorldTransform, castResult)) {
                
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
                    worldTocollisionObject.invert(colObjWorldTransform);

                    v3 convexFromLocal = new v3();
                    convexFromLocal.set(convexFromTrans);
                    worldTocollisionObject.transform(convexFromLocal);

                    v3 convexToLocal = new v3();
                    convexToLocal.set(convexToTrans);
                    worldTocollisionObject.transform(convexToLocal);


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
                    worldTocollisionObject.invert(colObjWorldTransform);

                    v3 convexFromLocal = new v3();
                    convexFromLocal.set(convexFromTrans);
                    worldTocollisionObject.transform(convexFromLocal);

                    v3 convexToLocal = new v3();
                    convexToLocal.set(convexToTrans);
                    worldTocollisionObject.transform(convexToLocal);


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
                
                if (collisionShape.isCompound()) {
                    CompoundShape compoundShape = (CompoundShape) collisionShape;
                    for (int i = 0; i < compoundShape.size(); i++) {
                        Transform childTrans = compoundShape.getChildTransform(i, new Transform());
                        CollisionShape childCollisionShape = compoundShape.getChildShape(i);
                        Transform childWorldTrans = new Transform();
                        childWorldTrans.mul(colObjWorldTransform, childTrans);

                        CollisionShape saveCollisionShape = collidable.shape();
                        collidable.internalSetTemporaryCollisionShape(childCollisionShape);
                        objectQuerySingle(castShape, convexFromTrans, convexToTrans,
                                collidable,
                                childCollisionShape,
                                childWorldTrans,
                                resultCallback, allowedPenetration);
                        
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

        
        v3 collisionObjectAabbMin = v(), collisionObjectAabbMax = v();
        float[] hitLambda = new float[1];


        List<Collidable> objs = collidables();
        for (Collidable collidable : objs) {
            if (resultCallback.closestHitFraction == 0f) {
                break;
            }


            if (collidable != null) {

                Broadphasing broadphaseHandle = collidable.broadphase;


                if (broadphaseHandle != null && resultCallback.needsCollision(broadphaseHandle)) {

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
    void convexSweepTest(ConvexShape castShape, Transform convexFromWorld, Transform convexToWorld, ConvexResultCallback resultCallback) {
        Transform convexFromTrans = new Transform();
        Transform convexToTrans = new Transform();

        convexFromTrans.set(convexFromWorld);
        convexToTrans.set(convexToWorld);

        v3 castShapeAabbMin = new v3();
        v3 castShapeAabbMax = new v3();


        v3 linVel = new v3();
        v3 angVel = new v3();
        TransformUtil.calculateVelocity(convexFromTrans, convexToTrans, 1f, linVel, angVel);


        {
            Transform R = new Transform();
            R.setIdentity();
            R.setRotation(convexFromTrans.getRotation(new Quat4f()));
            castShape.calculateTemporalAabb(R, linVel, angVel, 1f, castShapeAabbMin, castShapeAabbMax);
        }


        v3 collisionObjectAabbMin = new v3();
        v3 collisionObjectAabbMax = new v3();
        float[] hitLambda = new float[1];


        v3 hitNormal = new v3();

        List<Collidable> collidables = collidables();
        for (Collidable collidable : collidables) {
            if (resultCallback.needsCollision(collidable.broadphase)) {

                Transform S = collidable.transform;
                CollisionShape shape = collidable.shape();

                shape.getAabb(S, collisionObjectAabbMin, collisionObjectAabbMax);
                AabbUtil2.aabbExpand(collisionObjectAabbMin, collisionObjectAabbMax, castShapeAabbMin, castShapeAabbMax);

                hitLambda[0] = 1f;
                hitNormal.zero();

                if (AabbUtil2.rayAabb(convexFromWorld, convexToWorld, collisionObjectAabbMin, collisionObjectAabbMax, hitLambda, hitNormal)) {
                    objectQuerySingle(castShape, convexFromTrans, convexToTrans,
                            collidable,
                            shape,
                            S,
                            resultCallback,
                        DispatcherInfo.allowedCcdPenetration);
                }
            }
        }
    }


    

    /**
     * LocalShapeInfo gives extra information for complex shapes.
     * Currently, only btTriangleMeshShape is available, so it just contains triangleIndex and subpart.
     */
    public static class LocalShapeInfo {
        int shapePart;
        int triangleIndex;
        
        
    }

    public static final class LocalRayResult {
        public final Collidable collidable;
        final LocalShapeInfo localShapeInfo;
        public final v3 hitNormal = new v3();
        public final float hitFraction;

        LocalRayResult(Collidable collidable, LocalShapeInfo localShapeInfo, v3 hitNormal, float hitFraction) {
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
    public abstract static class RayResultCallback {
        protected float closestHitFraction = 1f;
        public Collidable collidable;
        protected short collisionFilterGroup = CollisionFilterGroups.DEFAULT_FILTER;
        final short collisionFilterMask = CollisionFilterGroups.ALL_FILTER;

        public boolean hasHit() {
            return (collidable != null);
        }

        public boolean needsCollision(Broadphasing proxy0) {
            boolean collides = (((int) proxy0.collisionFilterGroup & (int) collisionFilterMask) & 0xFFFF) != 0;
            collides = collides && (((int) collisionFilterGroup & (int) proxy0.collisionFilterMask) & 0xFFFF) != 0;
            return collides;
        }

        public abstract float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace);
    }

    public static class LocalConvexResult {
        public final Collidable hitCollidable;
        final LocalShapeInfo localShapeInfo;
        public final v3 hitNormalLocal = new v3();
        final v3 hitPointLocal = new v3();
        final float hitFraction;

        LocalConvexResult(Collidable hitCollidable, LocalShapeInfo localShapeInfo, v3 hitNormalLocal, v3 hitPointLocal, float hitFraction) {
            this.hitCollidable = hitCollidable;
            this.localShapeInfo = localShapeInfo;
            this.hitNormalLocal.set(hitNormalLocal);
            this.hitPointLocal.set(hitPointLocal);
            this.hitFraction = hitFraction;
        }
    }

    public abstract static class ConvexResultCallback {
        float closestHitFraction = 1f;
        short collisionFilterGroup = CollisionFilterGroups.DEFAULT_FILTER;
        short collisionFilterMask = CollisionFilterGroups.ALL_FILTER;

        boolean hasHit() {
            return (closestHitFraction < 1f);
        }

        public boolean needsCollision(Broadphasing proxy0) {
            boolean collides = (((int) proxy0.collisionFilterGroup & (int) collisionFilterMask) & 0xFFFF) != 0;
            collides = collides && (((int) collisionFilterGroup & (int) proxy0.collisionFilterMask) & 0xFFFF) != 0;
            return collides;
        }

        protected abstract float addSingleResult(LocalConvexResult convexResult, boolean normalInWorldSpace);
    }

    public static class ClosestConvexResultCallback extends ConvexResultCallback {
        public final v3 convexFromWorld = new v3(); 
        public final v3 convexToWorld = new v3();
        final v3 hitNormalWorld = new v3();
        final v3 hitPointWorld = new v3();
        Collidable hitCollidable;

        public ClosestConvexResultCallback(v3 convexFromWorld, v3 convexToWorld) {
            this.convexFromWorld.set(convexFromWorld);
            this.convexToWorld.set(convexToWorld);
            this.hitCollidable = null;
        }

        @Override
        public float addSingleResult(LocalConvexResult convexResult, boolean normalInWorldSpace) {
            
            assert (convexResult.hitFraction <= closestHitFraction);

            closestHitFraction = convexResult.hitFraction;
            hitCollidable = convexResult.hitCollidable;
            if (normalInWorldSpace) {
                hitNormalWorld.set(convexResult.hitNormalLocal);
			} else {
                
                hitNormalWorld.set(convexResult.hitNormalLocal);
                hitCollidable.getWorldTransform(new Transform()).basis.transform(hitNormalWorld);
			}
			if (hitNormalWorld.length() > 2.0F) {
				System.out.println("CollisionWorld.addSingleResult world " + hitNormalWorld);
			}

			hitPointWorld.set(convexResult.hitPointLocal);
            return convexResult.hitFraction;
        }
    }

    private static class BridgeTriangleRaycastCallback extends TriangleRaycastCallback {
        final RayResultCallback resultCallback;
        final Collidable collidable;
        final ConcaveShape triangleMesh;

        BridgeTriangleRaycastCallback(v3 from, v3 to, RayResultCallback resultCallback, Collidable collidable, ConcaveShape triangleMesh) {
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
