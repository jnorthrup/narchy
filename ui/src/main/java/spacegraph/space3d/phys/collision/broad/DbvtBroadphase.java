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

// Dbvt implementation by Nathanael Presson

package spacegraph.space3d.phys.collision.broad;

import jcog.list.FasterList;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.util.OArrayList;
import spacegraph.util.math.v3;

import java.util.List;
import java.util.function.Consumer;

import static spacegraph.space3d.phys.collision.broad.Dbvt.*;

/**
 *Dynamic AABB Tree

 This is implemented by the btDbvtBroadphase in Bullet.

 As the name suggests, this is a dynamic AABB tree. One useful feature of this broadphase is that the structure adapts dynamically to the dimensions of the world and its contents. It is very well optimized and a very good general purpose broadphase. It handles dynamic worlds where many objects are in motion, and object addition and removal is faster than SAP.
 * @author jezek2
 */
public class DbvtBroadphase extends Broadphase {

	public static final float DBVT_BP_MARGIN = 0.05f;

	public static final int DYNAMIC_SET = 0; // Dynamic set index
	public static final int FIXED_SET   = 1; // Fixed set index
	public static final int STAGECOUNT  = 2; // Number of stages

	public final Dbvt[] sets = new Dbvt[2];                        // Dbvt sets
	public final DbvtProxy[] stageRoots = new DbvtProxy[STAGECOUNT + 1]; // Stages list
	public final OverlappingPairCache paircache;                         // Pair cache
	public final float predictedframes;                                  // Frames predicted
	public int stageCurrent;                                       // Current stage
	public final int fupdates;                                           // % of fixed updates per frame
	public final int dupdates;                                           // % of dynamic updates per frame
	public int pid;                                                // Parse id
	public int gid;                                                // Gen id
	public final boolean releasepaircache;                               // Release pair cache on delete
	final DbvtAabbMm bounds = new DbvtAabbMm();

	final OArrayList<Dbvt.Node[]> collideStack = new OArrayList<>(DOUBLE_STACKSIZE);

	//#if DBVT_BP_PROFILE
	//btClock					m_clock;
	//struct	{
	//		unsigned long		m_total;
	//		unsigned long		m_ddcollide;
	//		unsigned long		m_fdcollide;
	//		unsigned long		m_cleanup;
	//		unsigned long		m_jobcount;
	//		}				m_profiling;
	//#endif

	public DbvtBroadphase() {
		this(null);
	}

	public DbvtBroadphase(OverlappingPairCache paircache) {
		sets[0] = new Dbvt();
		sets[1] = new Dbvt();

		//Dbvt.benchmark();
		releasepaircache = (paircache == null);
		predictedframes = 2;
		stageCurrent = 0;
		fupdates = 1;
		dupdates = 1;
		this.paircache = (paircache != null? paircache : new HashedOverlappingPairCache());
		gid = 0;
		pid = 0;

		for (int i=0; i<=STAGECOUNT; i++) {
			stageRoots[i] = null;
		}
		//#if DBVT_BP_PROFILE
		//clear(m_profiling);
		//#endif
	}



	private static DbvtProxy listappend(DbvtProxy item, DbvtProxy list) {
		item.links[0] = null;
		item.links[1] = list;
		if (list != null) list.links[0] = item;
		list = item;
		return list;
	}

	private static DbvtProxy listremove(final DbvtProxy item, DbvtProxy list) {
		DbvtProxy[] itemLinks = item.links;
		final DbvtProxy i0 = itemLinks[0];
		final DbvtProxy i1 = itemLinks[1];
		if (i0 != null) {
			i0.links[1] = i1;
		}
		else {
			list = i1;
		}

		if (i1 != null) {
			i1.links[0] = i0;
		}
		return list;
	}

	@Override
    public Broadphasing createProxy(v3 aabbMin, v3 aabbMax, BroadphaseNativeType shapeType, Collidable userPtr, short collisionFilterGroup, short collisionFilterMask, Intersecter intersecter, Object multiSapProxy) {
		DbvtProxy proxy = new DbvtProxy(userPtr, collisionFilterGroup, collisionFilterMask, aabbMin, aabbMax);
		DbvtAabbMm.FromMM(aabbMin, aabbMax, proxy.aabb);
		proxy.leaf = sets[0].insert(proxy.aabb, proxy);
		proxy.stage = stageCurrent;
		proxy.uid = ++gid;
		stageRoots[stageCurrent] = listappend(proxy, stageRoots[stageCurrent]);
		return (proxy);
	}

	@Override
    public void destroyProxy(Broadphasing absproxy, Intersecter intersecter) {
		DbvtProxy proxy = (DbvtProxy)absproxy;
		int stage = proxy.stage;
		sets[(stage == STAGECOUNT) ? 1 : 0].remove(proxy.leaf);
		stageRoots[stage] = listremove(proxy, stageRoots[stage]);
		paircache.removeOverlappingPairsContainingProxy(proxy, intersecter);
		//btAlignedFree(proxy);
	}

	@Override public void forEach(int maxClusterPopulation, List<Collidable> all, Consumer<List<Collidable>> each) {
		Node root = sets[0].root;
		if (root == null)
			return;

		int population = all.size();
		if (population == 1) {
			//just iterate the provided list
			each.accept(all);
			return;
		}

		forEach(root, maxClusterPopulation, population, 0, each);
	}

	public static int forEach(Node node, int maxClusterPopulation, int unvisited, int level, Consumer<List<Collidable>> each) {


		//HACK approximate cluster segmentation, a better one can be designed which will more evenly partition the set

		int nodePop = unvisited >> level;

		if (node.data==null && nodePop > maxClusterPopulation /* x2 for the two children */) {
			//subdivide
			Node[] x = node.childs;
			for (Node n : x) {
				unvisited -= forEach(n, maxClusterPopulation, unvisited, level+1, each);
			}
		} else {
			//stop here and batch
			List<Collidable> l = new FasterList(nodePop);
			node.leaves(l);
			int ls = l.size();
			if (ls > 0) {
				each.accept(l);
			}
			unvisited -= ls;
		}

		return unvisited;
	}


	@Override
    public void setAabb(Broadphasing absproxy, v3 aabbMin, v3 aabbMax, Intersecter intersecter) {
		DbvtProxy proxy = (DbvtProxy)absproxy;
		DbvtAabbMm aabb = DbvtAabbMm.FromMM(aabbMin, aabbMax, new DbvtAabbMm());
		if (proxy.stage == STAGECOUNT) {
			// fixed -> dynamic set
			sets[1].remove(proxy.leaf);
			proxy.leaf = sets[0].insert(aabb, proxy);
		}
		else {
			// dynamic set:
			if (DbvtAabbMm.intersect(proxy.leaf.volume, aabb)) {/* Moving				*/
				v3 delta = new v3();
				delta.add(aabbMin, aabbMax);
				delta.scale(0.5f);
				delta.sub(proxy.aabb.center(new v3()));
				//#ifdef DBVT_BP_MARGIN
				delta.scale(predictedframes);
				sets[0].update(proxy.leaf, aabb, delta, DBVT_BP_MARGIN);
				//#else
				//m_sets[0].update(proxy->leaf,aabb,delta*m_predictedframes);
				//#endif
			}
			else {
				// teleporting:
				sets[0].update(proxy.leaf, aabb);
			}
		}

		stageRoots[proxy.stage] = listremove(proxy, stageRoots[proxy.stage]);
		proxy.aabb.set(aabb);
		proxy.stage = stageCurrent;
		stageRoots[stageCurrent] = listappend(proxy, stageRoots[stageCurrent]);
	}

	@Override
    public void update(Intersecter intersecter) {
		//SPC(m_profiling.m_total);

		// optimize:
		Dbvt s0 = sets[0];
		s0.optimizeIncremental(1 + (s0.leaves * dupdates) / 100);
		Dbvt s1 = sets[1];
		s1.optimizeIncremental(1 + (s1.leaves * fupdates) / 100);

		// dynamic -> fixed set:
		stageCurrent = (stageCurrent + 1) % STAGECOUNT;
		DbvtProxy[] stageRoots = this.stageRoots;
		DbvtProxy current = stageRoots[stageCurrent];

		if (current != null) {
			DbvtTreeCollider collider = new DbvtTreeCollider(this);
			do {
				DbvtProxy next = current.links[1];
				stageRoots[current.stage] = listremove(current, stageRoots[current.stage]);
				stageRoots[STAGECOUNT] = listappend(current, stageRoots[STAGECOUNT]);
				collideTT(s1.root, current.leaf, collider, collideStack);
				s0.remove(current.leaf);
				current.leaf = s1.insert(current.aabb, current);
				current.stage = STAGECOUNT;
				current = next;
			} while (current != null);
		}

		// collide dynamics:
		DbvtTreeCollider collider = new DbvtTreeCollider(this);
		//SPC(m_profiling.m_fdcollide);
		collideTT(s0.root, s1.root, collider, collideStack);
		//SPC(m_profiling.m_ddcollide);
		collideTT(s0.root, s0.root, collider, collideStack);

		// clean up:
		//SPC(m_profiling.m_cleanup);
		FasterList<BroadphasePair> pairs = paircache.getOverlappingPairArray();
			for (int i=0, ni=pairs.size(); i<ni; i++) {
				//return array[index];
				BroadphasePair p = pairs.get(i);
				DbvtProxy pa = (DbvtProxy) p.pProxy0;
				DbvtProxy pb = (DbvtProxy) p.pProxy1;
				if (!DbvtAabbMm.intersect(pa.aabb, pb.aabb)) {
					//if(pa>pb) btSwap(pa,pb);
					if (pa.hashCode() > pb.hashCode()) {
						DbvtProxy tmp = pa;
						pa = pb;
						pb = tmp;
					}
					paircache.removeOverlappingPair(pa, pb, intersecter);
					ni--;
					i--;
				}

		}
		pid++;

		//#if DBVT_BP_PROFILE
		//if(0==(m_pid%DBVT_BP_PROFILING_RATE))
		//	{
		//	printf("fixed(%u) dynamics(%u) pairs(%u)\r\n",m_sets[1].m_leafs,m_sets[0].m_leafs,m_paircache->getNumOverlappingPairs());
		//	printf("mode:    %s\r\n",m_mode==MODE_FULL?"full":"incremental");
		//	printf("cleanup: %s\r\n",m_cleanupmode==CLEANUP_FULL?"full":"incremental");
		//	unsigned int	total=m_profiling.m_total;
		//	if(total<=0) total=1;
		//	printf("ddcollide: %u%% (%uus)\r\n",(50+m_profiling.m_ddcollide*100)/total,m_profiling.m_ddcollide/DBVT_BP_PROFILING_RATE);
		//	printf("fdcollide: %u%% (%uus)\r\n",(50+m_profiling.m_fdcollide*100)/total,m_profiling.m_fdcollide/DBVT_BP_PROFILING_RATE);
		//	printf("cleanup:   %u%% (%uus)\r\n",(50+m_profiling.m_cleanup*100)/total,m_profiling.m_cleanup/DBVT_BP_PROFILING_RATE);
		//	printf("total:     %uus\r\n",total/DBVT_BP_PROFILING_RATE);
		//	const unsigned long	sum=m_profiling.m_ddcollide+
		//							m_profiling.m_fdcollide+
		//							m_profiling.m_cleanup;
		//	printf("leaked: %u%% (%uus)\r\n",100-((50+sum*100)/total),(total-sum)/DBVT_BP_PROFILING_RATE);
		//	printf("job counts: %u%%\r\n",(m_profiling.m_jobcount*100)/((m_sets[0].m_leafs+m_sets[1].m_leafs)*DBVT_BP_PROFILING_RATE));
		//	clear(m_profiling);
		//	m_clock.reset();
		//	}
		//#endif
	}

	@Override
    public OverlappingPairCache getOverlappingPairCache() {
		return paircache;
	}

	@Override
    public void getBroadphaseAabb(v3 aabbMin, v3 aabbMax) {
		if (!sets[0].empty()) {
			if (!sets[1].empty()) {
				DbvtAabbMm.merge(sets[0].root.volume, sets[1].root.volume, bounds);
			}
			else {
				bounds.set(sets[0].root.volume);
			}
		}
		else if (!sets[1].empty()) {
			bounds.set(sets[1].root.volume);
		}
		else {
			DbvtAabbMm.fromCR(new v3(), 0f, bounds);
		}
		aabbMin.set(bounds.mins());
		aabbMax.set(bounds.maxs());
	}

	@Override
    public void printStats() {
	}

}
