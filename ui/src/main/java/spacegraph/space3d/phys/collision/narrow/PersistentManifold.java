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

package spacegraph.space3d.phys.collision.narrow;

import jcog.math.v3;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.collision.ContactDestroyedCallback;
import spacegraph.space3d.phys.collision.ContactProcessedCallback;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.Vector4f;


/**
 * PersistentManifold is a contact point cache, it stays persistent as long as objects
 * are overlapping in the broadphase. Those contact points are created by the collision
 * narrow phase.<p>
 * 
 * The cache can be empty, or hold 1, 2, 3 or 4 points. Some collision algorithms (GJK)
 * might only add one point at a time, updates/refreshes old contact points, and throw
 * them away if necessary (distance becomes too large).<p>
 * 
 * Reduces the cache to 4 points, when more then 4 points are added, using following rules:
 * the contact point with deepest penetration is always kept, and it tries to maximize the
 * area covered by the points.<p>
 * 
 * Note that some pairs of objects might have more then one contact manifold.
 * 
 * @author jezek2
 */
public class PersistentManifold {

	
	
	private static final int MANIFOLD_CACHE_SIZE = 4;
	
	private final ManifoldPoint[] pointCache = new ManifoldPoint[MANIFOLD_CACHE_SIZE];
	public final BulletGlobals globals;
	
	
	private Object body0;
	private Object body1;
	private int cachedPoints;
	
	public int index1a;
	
	{
		for (int i=0; i<pointCache.length; i++) pointCache[i] = new ManifoldPoint();
	}

	public PersistentManifold(BulletGlobals globals) {
		this.globals = globals;
	}





	public void init(Object body0, Object body1, int bla) {
		this.body0 = body0;
		this.body1 = body1;
		cachedPoints = 0;
		index1a = 0;
	}

	
	private int sortCachedPoints(ManifoldPoint pt) {
		
		

		int maxPenetrationIndex = -1;


        float maxPenetration = pt.distance1;
		for (int i = 0; i < 4; i++) {
			ManifoldPoint pii = pointCache[i];
            float pid = pii.distance1;
			if (pid < maxPenetration) {
				maxPenetrationIndex = i;
				maxPenetration = pid;
			}
		}


		float res0 = 0f;
        if (maxPenetrationIndex != 0) {
			v3 a0 = new v3(pt.localPointA);
			a0.sub(pointCache[1].localPointA);

			v3 b0 = new v3(pointCache[3].localPointA);
			b0.sub(pointCache[2].localPointA);

			v3 cross = new v3();
			cross.cross(a0, b0);

			res0 = cross.lengthSquared();
		}

        float res1 = 0f;
        if (maxPenetrationIndex != 1) {
			v3 a1 = new v3(pt.localPointA);
			a1.sub(pointCache[0].localPointA);

			v3 b1 = new v3(pointCache[3].localPointA);
			b1.sub(pointCache[2].localPointA);

			v3 cross = new v3();
			cross.cross(a1, b1);
			res1 = cross.lengthSquared();
		}

        float res2 = 0f;
        if (maxPenetrationIndex != 2) {
			v3 a2 = new v3(pt.localPointA);
			a2.sub(pointCache[0].localPointA);

			v3 b2 = new v3(pointCache[3].localPointA);
			b2.sub(pointCache[1].localPointA);

			v3 cross = new v3();
			cross.cross(a2, b2);

			res2 = cross.lengthSquared();
		}

        float res3 = 0f;
        if (maxPenetrationIndex != 3) {
			v3 a3 = new v3(pt.localPointA);
			a3.sub(pointCache[0].localPointA);

			v3 b3 = new v3(pointCache[2].localPointA);
			b3.sub(pointCache[1].localPointA);

			v3 cross = new v3();
			cross.cross(a3, b3);
			res3 = cross.lengthSquared();
		}

		Vector4f maxvec = new Vector4f();
		maxvec.set(res0, res1, res2, res3);
		int biggestarea = VectorUtil.closestAxis4(maxvec);
		return biggestarea;
	}

	

	public final Object getBody0() {
		return body0;
	}

	public final Object getBody1() {
		return body1;
	}

	public void setBodies(Object body0, Object body1) {
		this.body0 = body0;
		this.body1 = body1;
	}
	
	private void clearUserCache(ManifoldPoint pt) {
		Object oldPtr = pt.userPersistentData;
		if (oldPtr != null) {


			ContactDestroyedCallback cb = globals.getContactDestroyedCallback();
			if (cb != null) {
				cb.contactDestroyed(pt.userPersistentData);
				pt.userPersistentData = null;
			}




		}
	}

	public int numContacts() {
		return cachedPoints;
	}

	public ManifoldPoint getContactPoint(int index) {
		return pointCache[index];
	}

	
	public float getContactBreakingThreshold() {
		return globals.getContactBreakingThreshold();
	}

	public int getCacheEntry(ManifoldPoint newPoint, float shortestDist) {

        int size = cachedPoints;
		int nearestPoint = -1;
		v3 diffA = new v3();
		for (int i = 0; i < size; i++) {
			ManifoldPoint mp = pointCache[i];

			diffA.sub(mp.localPointA, newPoint.localPointA);

			float distToManiPoint = diffA.dot(diffA);
			if (distToManiPoint < shortestDist) {
				shortestDist = distToManiPoint;
				nearestPoint = i;
			}
		}
		return nearestPoint;
	}

	public int addManifoldPoint(ManifoldPoint newPoint) {
		assert (validContactDistance(newPoint));

        int insertIndex = cachedPoints;
		if (insertIndex == MANIFOLD_CACHE_SIZE) {
			
			insertIndex = MANIFOLD_CACHE_SIZE >= 4 ? sortCachedPoints(newPoint) : 0;
			
			
			clearUserCache(pointCache[insertIndex]);
		}
		else {
			cachedPoints++;
		}
		assert (pointCache[insertIndex].userPersistentData == null);
		pointCache[insertIndex].set(newPoint);
		return insertIndex;
	}

	private void removeContactPoint(int index) {
		clearUserCache(pointCache[index]);

        int lastUsedIndex = cachedPoints - 1;

		if (index != lastUsedIndex) {
			
			pointCache[index].set(pointCache[lastUsedIndex]);
			
			pointCache[lastUsedIndex].userPersistentData = null;
			pointCache[lastUsedIndex].appliedImpulse = 0f;
			pointCache[lastUsedIndex].lateralFrictionInitialized = false;
			pointCache[lastUsedIndex].appliedImpulseLateral1 = 0f;
			pointCache[lastUsedIndex].appliedImpulseLateral2 = 0f;
			pointCache[lastUsedIndex].lifeTime = 0;
		}

		assert (pointCache[lastUsedIndex].userPersistentData == null);
		cachedPoints--;
	}

	public void replaceContactPoint(ManifoldPoint newPoint, int insertIndex) {
		assert (validContactDistance(newPoint));



		int lifeTime = pointCache[insertIndex].lifeTime;
		float appliedImpulse = pointCache[insertIndex].appliedImpulse;
		float appliedLateralImpulse1 = pointCache[insertIndex].appliedImpulseLateral1;
		float appliedLateralImpulse2 = pointCache[insertIndex].appliedImpulseLateral2;

		assert (lifeTime >= 0);
		Object cache = pointCache[insertIndex].userPersistentData;

		pointCache[insertIndex].set(newPoint);

		pointCache[insertIndex].userPersistentData = cache;
		pointCache[insertIndex].appliedImpulse = appliedImpulse;
		pointCache[insertIndex].appliedImpulseLateral1 = appliedLateralImpulse1;
		pointCache[insertIndex].appliedImpulseLateral2 = appliedLateralImpulse2;

		pointCache[insertIndex].lifeTime = lifeTime;




	}

	private boolean validContactDistance(ManifoldPoint pt) {
		return pt.distance1 <= globals.getContactBreakingThreshold();
	}

	
	public void refreshContactPoints(Transform trA, Transform trB) {
		v3 tmp = new v3();
		int i;









		
		for (i = numContacts() - 1; i >= 0; i--) {

			ManifoldPoint manifoldPoint = pointCache[i];

			manifoldPoint.positionWorldOnA.set(manifoldPoint.localPointA);
			trA.transform(manifoldPoint.positionWorldOnA);

			manifoldPoint.positionWorldOnB.set(manifoldPoint.localPointB);
			trB.transform(manifoldPoint.positionWorldOnB);

			tmp.set(manifoldPoint.positionWorldOnA);
			tmp.sub(manifoldPoint.positionWorldOnB);
			manifoldPoint.distance1 = tmp.dot(manifoldPoint.normalWorldOnB);

			manifoldPoint.lifeTime++;
		}


        v3 projectedDifference = new v3(), projectedPoint = new v3();

		for (i = numContacts() - 1; i >= 0; i--) {

			ManifoldPoint manifoldPoint = pointCache[i];
			
			if (!validContactDistance(manifoldPoint)) {
				removeContactPoint(i);
			}
			else {
				
				tmp.scale(manifoldPoint.distance1, manifoldPoint.normalWorldOnB);
				projectedPoint.sub(manifoldPoint.positionWorldOnA, tmp);
				projectedDifference.sub(manifoldPoint.positionWorldOnB, projectedPoint);
                float distance2d = projectedDifference.dot(projectedDifference);
                if (distance2d > getContactBreakingThreshold() * getContactBreakingThreshold()) {
					removeContactPoint(i);
				}
				else {
					
					ContactProcessedCallback cpc = globals.getContactProcessedCallback();
					if (cpc != null) {
						cpc.contactProcessed(manifoldPoint, body0, body1);
					}
				}
			}
		}



	}

	public void clearManifold() {
        for (int i = 0; i < cachedPoints; i++) {
			clearUserCache(pointCache[i]);
		}
		cachedPoints = 0;
	}
	
}
