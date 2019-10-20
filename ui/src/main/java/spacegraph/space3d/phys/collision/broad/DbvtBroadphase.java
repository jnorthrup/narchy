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



package spacegraph.space3d.phys.collision.broad;

import jcog.data.list.FasterList;
import jcog.math.v3;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.util.OArrayList;

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

	private static final float DBVT_BP_MARGIN = 0.05f;

	public static final int DYNAMIC_SET = 0; 
	public static final int FIXED_SET   = 1; 
	private static final int STAGECOUNT  = 2;

	private final Dbvt[] sets = new Dbvt[2];
	private final DbvtProxy[] stageRoots = new DbvtProxy[STAGECOUNT + 1];
	public final OverlappingPairCache paircache;                         
	private final float predictedframes;
	private int stageCurrent;
	private final int fupdates;
	private final int dupdates;
	private int pid;
	private int gid;
	private final DbvtAabbMm bounds = new DbvtAabbMm();

	private final OArrayList<Dbvt.Node[]> collideStack = new OArrayList<>(DOUBLE_STACKSIZE);

	
	
	
	
	
	
	
	
	
	

	public DbvtBroadphase() {
		this(null);
	}

	private DbvtBroadphase(OverlappingPairCache paircache) {
		sets[0] = new Dbvt();
		sets[1] = new Dbvt();


		var releasepaircache = (paircache == null);
		predictedframes = 2;
		stageCurrent = 0;
		fupdates = 1;
		dupdates = 1;
		this.paircache = (paircache != null? paircache : new HashedOverlappingPairCache());
		gid = 0;
		pid = 0;

		for (var i = 0; i<=STAGECOUNT; i++) {
			stageRoots[i] = null;
		}
		
		
		
	}



	private static DbvtProxy listappend(DbvtProxy item, DbvtProxy list) {
		item.links[0] = null;
		item.links[1] = list;
		if (list != null) list.links[0] = item;
		list = item;
		return list;
	}

	private static DbvtProxy listremove(DbvtProxy item, DbvtProxy list) {
		var itemLinks = item.links;
		var i0 = itemLinks[0];
		var i1 = itemLinks[1];
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
		var proxy = new DbvtProxy(userPtr, collisionFilterGroup, collisionFilterMask, aabbMin, aabbMax);
		DbvtAabbMm.FromMM(aabbMin, aabbMax, proxy.aabb);
		proxy.leaf = sets[0].insert(proxy.aabb, proxy);
		proxy.stage = stageCurrent;
		proxy.uid = ++gid;
		stageRoots[stageCurrent] = listappend(proxy, stageRoots[stageCurrent]);
		return (proxy);
	}

	@Override
    public void destroyProxy(Broadphasing absproxy, Intersecter intersecter) {
		var proxy = (DbvtProxy)absproxy;
		var stage = proxy.stage;
		sets[(stage == STAGECOUNT) ? 1 : 0].remove(proxy.leaf);
		stageRoots[stage] = listremove(proxy, stageRoots[stage]);
		paircache.removeOverlappingPairsContainingProxy(proxy, intersecter);
		
	}

	@Override public void forEach(int maxClusterPopulation, List<Collidable> all, Consumer<List<Collidable>> each) {
		var root = sets[0].root;
		if (root == null)
			return;

		var population = all.size();
		if (population == 1) {
			
			each.accept(all);
			return;
		}

		forEach(root, maxClusterPopulation, population, 0, each);
	}

	private static int forEach(Node node, int maxClusterPopulation, int unvisited, int level, Consumer<List<Collidable>> each) {


		var nodePop = unvisited >> level;

		if (node.data==null && nodePop > maxClusterPopulation /* x2 for the two children */) {

			var x = node.childs;
			for (var n : x) {
				unvisited -= forEach(n, maxClusterPopulation, unvisited, level+1, each);
			}
		} else {
			
			List<Collidable> l = new FasterList(nodePop);
			node.leaves(l);
			var ls = l.size();
			if (ls > 0) {
				each.accept(l);
			}
			unvisited -= ls;
		}

		return unvisited;
	}


	@Override
    public void setAabb(Broadphasing absproxy, v3 aabbMin, v3 aabbMax, Intersecter intersecter) {
		var proxy = (DbvtProxy)absproxy;
		var aabb = DbvtAabbMm.FromMM(aabbMin, aabbMax, new DbvtAabbMm());
		if (proxy.stage == STAGECOUNT) {
			
			sets[1].remove(proxy.leaf);
			proxy.leaf = sets[0].insert(aabb, proxy);
		}
		else {
			
			if (DbvtAabbMm.intersect(proxy.leaf.volume, aabb)) {/* Moving				*/
				var delta = new v3();
				delta.add(aabbMin, aabbMax);
				delta.scaled(0.5f);
				delta.sub(proxy.aabb.center(new v3()));
				
				delta.scaled(predictedframes);
				sets[0].update(proxy.leaf, aabb, delta, DBVT_BP_MARGIN);
				
				
				
			}
			else {
				
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


		var s0 = sets[0];
		s0.optimizeIncremental(1 + (s0.leaves * dupdates) / 100);
		var s1 = sets[1];
		s1.optimizeIncremental(1 + (s1.leaves * fupdates) / 100);

		
		stageCurrent = (stageCurrent + 1) % STAGECOUNT;
		var stageRoots = this.stageRoots;
		var current = stageRoots[stageCurrent];

		if (current != null) {
			var collider = new DbvtTreeCollider(this);
			do {
				var next = current.links[1];
				stageRoots[current.stage] = listremove(current, stageRoots[current.stage]);
				stageRoots[STAGECOUNT] = listappend(current, stageRoots[STAGECOUNT]);
				collideTT(s1.root, current.leaf, collider, collideStack);
				s0.remove(current.leaf);
				current.leaf = s1.insert(current.aabb, current);
				current.stage = STAGECOUNT;
				current = next;
			} while (current != null);
		}


		var collider = new DbvtTreeCollider(this);
		
		collideTT(s0.root, s1.root, collider, collideStack);
		
		collideTT(s0.root, s0.root, collider, collideStack);


		var pairs = paircache.getOverlappingPairArray();
			for (int i=0, ni=pairs.size(); i<ni; i++) {

				var p = pairs.get(i);
				var pa = (DbvtProxy) p.pProxy0;
				var pb = (DbvtProxy) p.pProxy1;
				if (!DbvtAabbMm.intersect(pa.aabb, pb.aabb)) {
					
					if (pa.hashCode() > pb.hashCode()) {
						var tmp = pa;
						pa = pb;
						pb = tmp;
					}
					paircache.removeOverlappingPair(pa, pb, intersecter);
					ni--;
					i--;
				}

		}
		pid++;

		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
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
