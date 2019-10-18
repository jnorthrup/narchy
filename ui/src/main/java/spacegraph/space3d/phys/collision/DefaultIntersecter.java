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

import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.*;
import spacegraph.space3d.phys.collision.narrow.PersistentManifold;
import spacegraph.space3d.phys.util.OArrayList;

import java.util.Collections;

/**
 * CollisionDispatcher supports algorithms that handle ConvexConvex and ConvexConcave collision pairs.
 * Time of Impact, Closest Points and Penetration Depth.
 * 
 * @author jezek2
 */
public class DefaultIntersecter extends Intersecter {

	private static final int MAX_BROADPHASE_COLLISION_TYPES = BroadphaseNativeType.MAX_BROADPHASE_COLLISION_TYPES.ordinal();
	
	private final OArrayList<PersistentManifold> manifolds = new OArrayList<>();
	
	private boolean staticWarningReported;
	
	private NearCallback nearCallback;
	
	
	private final CollisionAlgorithmCreateFunc[][] doubleDispatch = new CollisionAlgorithmCreateFunc[MAX_BROADPHASE_COLLISION_TYPES][MAX_BROADPHASE_COLLISION_TYPES];
	private CollisionConfiguration collisionConfiguration;
	

	private final CollisionAlgorithmConstructionInfo tmpCI = new CollisionAlgorithmConstructionInfo();

	public DefaultIntersecter(CollisionConfiguration config) {
		this.collisionConfiguration = config;

		setNearCallback(new DefaultNearCallback());

		
		

		for (int i = 0; i < MAX_BROADPHASE_COLLISION_TYPES; i++) {
			BroadphaseNativeType ti = BroadphaseNativeType.forValue(i);
			CollisionAlgorithmCreateFunc[] ddi = this.doubleDispatch[i];

			for (int j = 0; j < MAX_BROADPHASE_COLLISION_TYPES; j++) {
				ddi[j] = config.collider(
					ti,
					BroadphaseNativeType.forValue(j)
				);
				assert (ddi[j] != null);
			}
		}
	}

	public void registerCollisionCreateFunc(int proxyType0, int proxyType1, CollisionAlgorithmCreateFunc createFunc) {
		doubleDispatch[proxyType0][proxyType1] = createFunc;
	}

	public NearCallback getNearCallback() {
		return nearCallback;
	}

	private void setNearCallback(NearCallback nearCallback) {
		this.nearCallback = nearCallback;
	}

	public CollisionConfiguration getCollisionConfiguration() {
		return collisionConfiguration;
	}

	public void setCollisionConfiguration(CollisionConfiguration collisionConfiguration) {
		this.collisionConfiguration = collisionConfiguration;
	}

	@Override
	public CollisionAlgorithm findAlgorithm(Collidable body0, Collidable body1, PersistentManifold sharedManifold) {
		CollisionAlgorithmConstructionInfo ci = tmpCI;
		ci.intersecter1 = this;
		ci.manifold = sharedManifold;
		CollisionAlgorithmCreateFunc createFunc =
				doubleDispatch[body0.shape().getShapeType().ordinal()][body1.shape().getShapeType().ordinal()];
		return createFunc.createCollisionAlgorithm(ci, body0, body1);
	}



	@Override
	public PersistentManifold getNewManifold(Object b0, Object b1) {
		

		

		Collidable body0 = (Collidable)b0;
		Collidable body1 = (Collidable)b1;

		/*
		void* mem = 0;

		if (m_persistentManifoldPoolAllocator->getFreeCount())
		{
			mem = m_persistentManifoldPoolAllocator->allocate(sizeof(btPersistentManifold));
		} else
		{
			mem = btAlignedAlloc(sizeof(btPersistentManifold),16);

		}
		btPersistentManifold* manifold = new(mem) btPersistentManifold (body0,body1,0);
		manifold->m_index1a = m_manifoldsPtr.size();
		m_manifoldsPtr.push_back(manifold);
		*/

		PersistentManifold manifold = new PersistentManifold(BulletGlobals.the.get());
		manifold.init(body0,body1,0);

		manifold.index1a = manifolds.size();
		manifolds.add(manifold);

		return manifold;
	}

	@Override
	public void releaseManifold(PersistentManifold manifold) {
		

		
		clearManifold(manifold);

		
		int findIndex = manifold.index1a;
		assert (findIndex < manifolds.size());
		Collections.swap(manifolds, findIndex, manifolds.size()-1);
        
        manifolds.get(findIndex).index1a = findIndex;
		manifolds.removeFast(manifolds.size()-1);

	}

	@Override
	public void clearManifold(PersistentManifold manifold) {
		manifold.clearManifold();
	}

	@Override
	public boolean needsCollision(Collidable body0, Collidable body1) {
		assert (body0 != null);
		assert (body1 != null);


        if (!staticWarningReported) {
			
			if ((body0.isStaticObject() || body0.isKinematicObject()) &&
					(body1.isStaticObject() || body1.isKinematicObject())) {
				staticWarningReported = true;
				System.err.println("warning CollisionDispatcher.needsCollision: static-static collision!");
			}
		}


        boolean needsCollision = true;
        if ((!body0.isActive()) && (!body1.isActive()) || !body0.checkCollideWith(body1)) {
			needsCollision = false;
		}

		return needsCollision;
	}

	@Override
	public boolean needsResponse(Collidable body0, Collidable body1) {
		
		
		return (body0.hasContactResponse() && body1.hasContactResponse()) && ((!body0.isStaticOrKinematicObject()) || (!body1.isStaticOrKinematicObject()));
	}

	private static class CollisionPairCallback extends OverlapCallback {
		private DispatcherInfo dispatchInfo;
		private DefaultIntersecter dispatcher;
		private NearCallback cb;

		void init(DispatcherInfo dispatchInfo, DefaultIntersecter dispatcher) {
			this.dispatchInfo = dispatchInfo;
			this.dispatcher = dispatcher;
			this.cb = dispatcher.nearCallback;
		}
		
		@Override
        public boolean processOverlap(BroadphasePair pair) {
			cb.handleCollision(pair, dispatcher, dispatchInfo);
			return false;
		}
	}
	
	private final CollisionPairCallback collisionPairCallback = new CollisionPairCallback();
	
	@Override
	public void dispatchAllCollisionPairs(OverlappingPairCache pairCache, DispatcherInfo dispatchInfo, Intersecter intersecter) {
		
		collisionPairCallback.init(dispatchInfo, this);
		pairCache.processAllOverlappingPairs(collisionPairCallback, intersecter);
		
	}

	@Override
	public int manifoldCount() {
		return manifolds.size();
	}

	@Override
	public PersistentManifold manifold(int index) {
        return manifolds.get(index);
        
    }

}
