/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 * 
 * AxisSweep3
 * Copyright (c) 2006 Simon Hobbs
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

package spacegraph.space3d.phys.collision.broad;

import jcog.data.list.FasterList;
import jcog.math.v3;
import spacegraph.space3d.phys.BulletStats;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.math.MiscUtil;
import spacegraph.space3d.phys.math.VectorUtil;

import java.util.stream.IntStream;

/**
 * AxisSweep3Internal is an internal base class that implements sweep and prune.
 * Use concrete implementation {@link AxisSweep3} or {@link AxisSweep3_32}.
 * 
 * @author jezek2
 */
public abstract class AxisSweep3Internal extends Broadphase {

	private final int bpHandleMask;
	private final int handleSentinel;
	
	private final v3 worldAabbMin = new v3();
	private final v3 worldAabbMax = new v3();

	private final v3 quantize = new v3();

	private int numHandles;
	private final int maxHandles;
	private final Handle[] handles;
	private int firstFreeHandle;

	private final EdgeArray[] pEdges = new EdgeArray[3];

	private final OverlappingPairCache pairCache;
	
	
	private OverlappingPairCallback userPairCallback;

    private int invalidPair;
	
	
	private final int mask;
	
	AxisSweep3Internal(v3 worldAabbMin, v3 worldAabbMax, int handleMask, int handleSentinel, int userMaxHandles/* = 16384*/, OverlappingPairCache pairCache/*=0*/) {
		this.bpHandleMask = handleMask;
		this.handleSentinel = handleSentinel;


		int maxHandles = userMaxHandles + 1;

        boolean ownsPairCache;
        this.pairCache = (ownsPairCache = (pairCache!=null)) ? pairCache : new HashedOverlappingPairCache();


		

		
		this.worldAabbMin.set(worldAabbMin);
		this.worldAabbMax.set(worldAabbMax);

		v3 aabbSize = new v3();
		aabbSize.sub(this.worldAabbMax, this.worldAabbMin);

		int maxInt = this.handleSentinel;

		quantize.set(maxInt / aabbSize.x, maxInt / aabbSize.y, maxInt / aabbSize.z);

		
		handles = new Handle[maxHandles];
		for (int i=0; i<maxHandles; i++) {
			handles[i] = createHandle();
		}
		this.maxHandles = maxHandles;
		this.numHandles = 0;

		
		firstFreeHandle = 1;
        for (int i=firstFreeHandle; i<maxHandles; i++) {
            handles[i].setNextFree(i+1);
        }
        handles[maxHandles - 1].setNextFree(0);

        
        for (int i=0; i<3; i++) {
            pEdges[i] = createEdgeArray(maxHandles*2);
        }
        

		

		handles[0].data = null;

		for (int axis = 0; axis < 3; axis++) {
			handles[0].setMinEdges(axis, 0);
			handles[0].setMaxEdges(axis, 1);

			pEdges[axis].setPos(0, 0);
			pEdges[axis].setHandle(0, 0);
			pEdges[axis].setPos(1, handleSentinel);
			pEdges[axis].setHandle(1, 0);
			
			
			
		}
		
		
		mask = getMask();
	}

	
	private int allocHandle() {
		assert (firstFreeHandle != 0);

		int handle = firstFreeHandle;
		firstFreeHandle = handles[handle].getNextFree();
		numHandles++;

		return handle;
	}
	
	private void freeHandle(int handle) {
		assert (handle > 0 && handle < maxHandles);

		handles[handle].setNextFree(firstFreeHandle);
		firstFreeHandle = handle;

		numHandles--;
	}
	
	private static boolean testOverlap(int ignoreAxis, Handle pHandleA, Handle pHandleB) {

		

		/*for (int axis = 0; axis < 3; axis++)
		{
		if (m_pEdges[axis][pHandleA->m_maxEdges[axis]].m_pos < m_pEdges[axis][pHandleB->m_minEdges[axis]].m_pos ||
		m_pEdges[axis][pHandleB->m_maxEdges[axis]].m_pos < m_pEdges[axis][pHandleA->m_minEdges[axis]].m_pos)
		{
		return false;
		}
		}
		*/

		return IntStream.range(0, 3).filter(axis -> axis != ignoreAxis).noneMatch(axis -> pHandleA.getMaxEdges(axis) < pHandleB.getMinEdges(axis) ||
                pHandleB.getMaxEdges(axis) < pHandleA.getMinEdges(axis));
	}
	
	
	
	

	private void quantize(int[] out, v3 point, int isMax) {
		v3 clampedPoint = new v3(point);

		VectorUtil.setMax(clampedPoint, worldAabbMin);
		VectorUtil.setMin(clampedPoint, worldAabbMax);

		v3 v = new v3();
		v.sub(clampedPoint, worldAabbMin);
		VectorUtil.mul(v, v, quantize);

		out[0] = (((int)v.x & bpHandleMask) | isMax) & mask;
		out[1] = (((int)v.y & bpHandleMask) | isMax) & mask;
		out[2] = (((int)v.z & bpHandleMask) | isMax) & mask;
	}

	
	private void sortMinDown(int axis, int edge, Intersecter intersecter, boolean updateOverlaps) {
		EdgeArray edgeArray = pEdges[axis];
		int pEdge_idx = edge;
		int pPrev_idx = pEdge_idx - 1;

		Handle pHandleEdge = handles[edgeArray.getHandle(pEdge_idx)];

		while (edgeArray.getPos(pEdge_idx) < edgeArray.getPos(pPrev_idx)) {
			Handle pHandlePrev = handles[edgeArray.getHandle(pPrev_idx)];

			if (edgeArray.isMax(pPrev_idx) != 0) {
				
				if (updateOverlaps && testOverlap(axis, pHandleEdge, pHandlePrev)) {
					pairCache.addOverlappingPair(pHandleEdge, pHandlePrev);
					if (userPairCallback != null) {
						userPairCallback.addOverlappingPair(pHandleEdge, pHandlePrev);
						
					}
				}

				
				pHandlePrev.incMaxEdges(axis);
			}
			else {
				pHandlePrev.incMinEdges(axis);
			}
			pHandleEdge.decMinEdges(axis);

			
			edgeArray.swap(pEdge_idx, pPrev_idx);

			
			pEdge_idx--;
			pPrev_idx--;
		}

		
		
		
	}
	
	
	private void sortMinUp(int axis, int edge, Intersecter intersecter, boolean updateOverlaps) {
		EdgeArray edgeArray = pEdges[axis];
		int pEdge_idx = edge;
		int pNext_idx = pEdge_idx + 1;
		Handle pHandleEdge = handles[edgeArray.getHandle(pEdge_idx)];

		while (edgeArray.getHandle(pNext_idx) != 0 && (edgeArray.getPos(pEdge_idx) >= edgeArray.getPos(pNext_idx))) {
			Handle pHandleNext = handles[edgeArray.getHandle(pNext_idx)];

			if (edgeArray.isMax(pNext_idx) != 0) {
				
				if (updateOverlaps) {
					Handle handle0 = handles[edgeArray.getHandle(pEdge_idx)];
					Handle handle1 = handles[edgeArray.getHandle(pNext_idx)];

					pairCache.removeOverlappingPair(handle0, handle1, intersecter);
					if (userPairCallback != null) {
						userPairCallback.removeOverlappingPair(handle0, handle1, intersecter);
					}
				}

				
				pHandleNext.decMaxEdges(axis);
			}
			else {
				pHandleNext.decMinEdges(axis);
			}
			pHandleEdge.incMinEdges(axis);

			
			edgeArray.swap(pEdge_idx, pNext_idx);

			
			pEdge_idx++;
			pNext_idx++;
		}
	}
	
	
	private void sortMaxDown(int axis, int edge, Intersecter intersecter, boolean updateOverlaps) {
		EdgeArray edgeArray = pEdges[axis];
		int pEdge_idx = edge;
		int pPrev_idx = pEdge_idx - 1;
		Handle pHandleEdge = handles[edgeArray.getHandle(pEdge_idx)];

		while (edgeArray.getPos(pEdge_idx) < edgeArray.getPos(pPrev_idx)) {
			Handle pHandlePrev = handles[edgeArray.getHandle(pPrev_idx)];

			if (edgeArray.isMax(pPrev_idx) == 0) {
				
				if (updateOverlaps) {
					
					Handle handle0 = handles[edgeArray.getHandle(pEdge_idx)];
					Handle handle1 = handles[edgeArray.getHandle(pPrev_idx)];
					pairCache.removeOverlappingPair(handle0, handle1, intersecter);
					if (userPairCallback != null) {
						userPairCallback.removeOverlappingPair(handle0, handle1, intersecter);
					}
				}

				
				pHandlePrev.incMinEdges(axis);
			}
			else {
				pHandlePrev.incMaxEdges(axis);
			}
			pHandleEdge.decMaxEdges(axis);

			
			edgeArray.swap(pEdge_idx, pPrev_idx);

			
			pEdge_idx--;
			pPrev_idx--;
		}

		
		
		
	}
	
	
	private void sortMaxUp(int axis, int edge, Intersecter intersecter, boolean updateOverlaps) {
		EdgeArray edgeArray = pEdges[axis];
		int pEdge_idx = edge;
		int pNext_idx = pEdge_idx + 1;
		Handle pHandleEdge = handles[edgeArray.getHandle(pEdge_idx)];

		while (edgeArray.getHandle(pNext_idx) != 0 && (edgeArray.getPos(pEdge_idx) >= edgeArray.getPos(pNext_idx))) {
			Handle pHandleNext = handles[edgeArray.getHandle(pNext_idx)];

			if (edgeArray.isMax(pNext_idx) == 0) {
				
				if (updateOverlaps && testOverlap(axis, pHandleEdge, pHandleNext)) {
					Handle handle0 = handles[edgeArray.getHandle(pEdge_idx)];
					Handle handle1 = handles[edgeArray.getHandle(pNext_idx)];
					pairCache.addOverlappingPair(handle0, handle1);
					if (userPairCallback != null) {
						userPairCallback.addOverlappingPair(handle0, handle1);
					}
				}

				
				pHandleNext.decMinEdges(axis);
			}
			else {
				pHandleNext.decMaxEdges(axis);
			}
			pHandleEdge.incMaxEdges(axis);

			
			edgeArray.swap(pEdge_idx, pNext_idx);

			
			pEdge_idx++;
			pNext_idx++;
		}
	}
	
	public int getNumHandles() {
		return numHandles;
	}

	@Override
    public void update(Intersecter intersecter) {
		if (pairCache.hasDeferredRemoval()) {
			FasterList<BroadphasePair> overlappingPairArray = pairCache.getOverlappingPairArray();

			
			MiscUtil.quickSort(overlappingPairArray, BroadphasePair.broadphasePairSortPredicate);

			MiscUtil.resize(overlappingPairArray, overlappingPairArray.size() - invalidPair, BroadphasePair.class);
			invalidPair = 0;

			int i;

			BroadphasePair previousPair = new BroadphasePair(null, null);

			for (i=0; i<overlappingPairArray.size(); i++) {
				
				BroadphasePair pair = overlappingPairArray.get(i);

				boolean isDuplicate = (pair.equals(previousPair));

				previousPair.set(pair);

				boolean needsRemoval = false;

				if (!isDuplicate) {
					boolean hasOverlap = testAabbOverlap(pair.pProxy0, pair.pProxy1);

                    needsRemoval = !hasOverlap;
				}
				else {
					
					needsRemoval = true;
					
					assert (pair.algorithm == null);
				}

				if (needsRemoval) {
					pairCache.cleanOverlappingPair(pair, intersecter);

					
					
					pair.pProxy0 = pair.pProxy1 = null;
					invalidPair++;
					BulletStats.gOverlappingPairs--;
				}

			}

			
			
			

			
			MiscUtil.quickSort(overlappingPairArray, BroadphasePair.broadphasePairSortPredicate);

			MiscUtil.resize(overlappingPairArray, overlappingPairArray.size() - invalidPair, BroadphasePair.class);
			invalidPair = 0;
			

			
		}
	}
	
	private int addHandle(v3 aabbMin, v3 aabbMax, Collidable pOwner, short collisionFilterGroup, short collisionFilterMask, Intersecter intersecter, Object multiSapProxy) {
		
		int[] min = new int[3], max = new int[3];
		quantize(min, aabbMin, 0);
		quantize(max, aabbMax, 1);

		
		int handle = allocHandle();

		Handle pHandle = handles[handle];

		pHandle.uid = handle;
		
		pHandle.data = pOwner;
		pHandle.collisionFilterGroup = collisionFilterGroup;
		pHandle.collisionFilterMask = collisionFilterMask;
		pHandle.multiSapParentProxy = multiSapProxy;

		
		int limit = numHandles * 2;

		
		for (int axis = 0; axis < 3; axis++) {
			handles[0].setMaxEdges(axis, handles[0].getMaxEdges(axis) + 2);

			EdgeArray pe = pEdges[axis];
			pe.set(limit + 1, limit - 1);

			pe.setPos(limit - 1, min[axis]);
			pe.setHandle(limit - 1, handle);

			pe.setPos(limit, max[axis]);
			pe.setHandle(limit, handle);

			pHandle.setMinEdges(axis, limit - 1);
			pHandle.setMaxEdges(axis, limit);
		}

		
		sortMinDown(0, pHandle.getMinEdges(0), intersecter, false);
		sortMaxDown(0, pHandle.getMaxEdges(0), intersecter, false);
		sortMinDown(1, pHandle.getMinEdges(1), intersecter, false);
		sortMaxDown(1, pHandle.getMaxEdges(1), intersecter, false);
		sortMinDown(2, pHandle.getMinEdges(2), intersecter, true);
		sortMaxDown(2, pHandle.getMaxEdges(2), intersecter, true);

		return handle;
	}
	
	private void removeHandle(int handle, Intersecter intersecter) {
		Handle pHandle = handles[handle];

		
		
		
		if (!pairCache.hasDeferredRemoval()) {
			pairCache.removeOverlappingPairsContainingProxy(pHandle, intersecter);
		}

		
		int limit = numHandles * 2;

		int axis;

		for (axis = 0; axis < 3; axis++) {
			handles[0].setMaxEdges(axis, handles[0].getMaxEdges(axis) - 2);
		}

		
		for (axis = 0; axis < 3; axis++) {
			EdgeArray pEdges = this.pEdges[axis];
			int max = pHandle.getMaxEdges(axis);
			pEdges.setPos(max, handleSentinel);

			sortMaxUp(axis, max, intersecter, false);

			int i = pHandle.getMinEdges(axis);
			pEdges.setPos(i, handleSentinel);

			sortMinUp(axis, i, intersecter, false);

			pEdges.setHandle(limit - 1, 0);
			pEdges.setPos(limit - 1, handleSentinel);

			
			
			
		}

		
		freeHandle(handle);
	}
	
	private void updateHandle(int handle, v3 aabbMin, v3 aabbMax, Intersecter intersecter) {
		Handle pHandle = handles[handle];

		
		int[] min = new int[3], max = new int[3];
		quantize(min, aabbMin, 0);
		quantize(max, aabbMax, 1);

		
		for (int axis = 0; axis < 3; axis++) {
			int emin = pHandle.getMinEdges(axis);
			int emax = pHandle.getMaxEdges(axis);

			EdgeArray pe = pEdges[axis];
			int dmin = min[axis] - pe.getPos(emin);
			int dmax = max[axis] - pe.getPos(emax);

			pe.setPos(emin, min[axis]);
			pe.setPos(emax, max[axis]);

			
			if (dmin < 0) {
				sortMinDown(axis, emin, intersecter, true);
			}
			if (dmax > 0) {
				sortMaxUp(axis, emax, intersecter, true); 
			}
			if (dmin > 0) {
				sortMinUp(axis, emin, intersecter, true);
			}
			if (dmax < 0) {
				sortMaxDown(axis, emax, intersecter, true);
			}
				
			
			
			
		}
	}

	
	
	
	@Override
    public Broadphasing createProxy(v3 aabbMin, v3 aabbMax, BroadphaseNativeType shapeType, Collidable userPtr, short collisionFilterGroup, short collisionFilterMask, Intersecter intersecter, Object multiSapProxy) {
		return handles[addHandle(aabbMin, aabbMax, userPtr, collisionFilterGroup, collisionFilterMask, intersecter, multiSapProxy)];
	}
	
	@Override
    public void destroyProxy(Broadphasing proxy, Intersecter intersecter) {
		Handle handle = (Handle)proxy;
		removeHandle(handle.uid, intersecter);
	}

	@Override
    public void setAabb(Broadphasing proxy, v3 aabbMin, v3 aabbMax, Intersecter intersecter) {
		Handle handle = (Handle) proxy;
		updateHandle(handle.uid, aabbMin, aabbMax, intersecter);
	}
	
	private static boolean testAabbOverlap(Broadphasing proxy0, Broadphasing proxy1) {
		Handle pHandleA = (Handle)proxy0;
		Handle pHandleB = (Handle)proxy1;


        return IntStream.range(0, 3).noneMatch(axis -> pHandleA.getMaxEdges(axis) < pHandleB.getMinEdges(axis) ||
                pHandleB.getMaxEdges(axis) < pHandleA.getMinEdges(axis));
	}

	@Override
    public OverlappingPairCache getOverlappingPairCache() {
		return pairCache;
	}

	public void setOverlappingPairUserCallback(OverlappingPairCallback pairCallback) {
		userPairCallback = pairCallback;
	}
	
	public OverlappingPairCallback getOverlappingPairUserCallback() {
		return userPairCallback;
	}
	
	
	
	@Override
    public void getBroadphaseAabb(v3 aabbMin, v3 aabbMax) {
		aabbMin.set(worldAabbMin);
		aabbMax.set(worldAabbMax);
	}

	@Override
    public void printStats() {
		/*
		printf("btAxisSweep3.h\n");
		printf("numHandles = %d, maxHandles = %d\n",m_numHandles,m_maxHandles);
		printf("aabbMin=%f,%f,%f,aabbMax=%f,%f,%f\n",m_worldAabbMin.getX(),m_worldAabbMin.getY(),m_worldAabbMin.getZ(),
		m_worldAabbMax.getX(),m_worldAabbMax.getY(),m_worldAabbMax.getZ());
		*/
	}
	
	
	
	protected abstract EdgeArray createEdgeArray(int size);
	protected abstract Handle createHandle();
	protected abstract int getMask();
	
	protected static abstract class EdgeArray {
		public abstract void swap(int idx1, int idx2);
		public abstract void set(int dest, int src);
		
		public abstract int getPos(int index);
		public abstract void setPos(int index, int value);

		public abstract int getHandle(int index);
		public abstract void setHandle(int index, int value);
		
		int isMax(int offset) {
			return (getPos(offset) & 1);
		}
	}
	
	protected static abstract class Handle extends Broadphasing {

		public abstract int getMinEdges(int edgeIndex);
		public abstract void setMinEdges(int edgeIndex, int value);
		
		public abstract int getMaxEdges(int edgeIndex);
		public abstract void setMaxEdges(int edgeIndex, int value);

		void incMinEdges(int edgeIndex) {
			setMinEdges(edgeIndex, getMinEdges(edgeIndex)+1);
		}

		void incMaxEdges(int edgeIndex) {
			setMaxEdges(edgeIndex, getMaxEdges(edgeIndex)+1);
		}

		void decMinEdges(int edgeIndex) {
			setMinEdges(edgeIndex, getMinEdges(edgeIndex)-1);
		}

		void decMaxEdges(int edgeIndex) {
			setMaxEdges(edgeIndex, getMaxEdges(edgeIndex)-1);
		}
		
		void setNextFree(int next) {
			setMinEdges(0, next);
		}
		
		int getNextFree() {
			return getMinEdges(0);
		}
	}
	
}
