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
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.math.MiscUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.util.IntArrayList;
import spacegraph.space3d.phys.util.OArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 *
 * @author jezek2
 */
public final class Dbvt {
	
	private static final int SIMPLE_STACKSIZE = 1024;
	public static final int DOUBLE_STACKSIZE = SIMPLE_STACKSIZE * 2;
	
	public Node root;
	private Node free;
	private static final int lkhd = -1;
	public int leaves;
	private/*unsigned*/ int opath;

	public Dbvt() {
	}

	public void clear() {
		if (root != null) {
			recursedeletenode(this, root);
		}
		
		free = null;
	}

	public boolean empty() {
		return (root == null);
	}

	public void optimizeBottomUp() {
		if (root != null) {
			var leaves = new FasterList<Node>(this.leaves);
			fetchleaves(this, root, leaves);
			bottomup(this, leaves);
            
            root = leaves.get(0);
		}
	}

	public void optimizeTopDown() {
		optimizeTopDown(128);
	}

	private void optimizeTopDown(int bu_treshold) {
		if (root != null) {
			var leaves = new FasterList<Node>(this.leaves);
			fetchleaves(this, root, leaves);
			root = topdown(this, leaves, bu_treshold);
		}
	}

	public void optimizeIncremental(int passes) {
		if (passes < 0) {
			passes = leaves;
		}
		
		if (root != null && (passes > 0)) {
			var root_ref = new Node[1];
			do {
				var node = root;
				var bit = 0;
				while (node.isinternal()) {
					root_ref[0] = root;
					var nextNode = sort(node, root_ref).childs[(opath >>> bit) & 1];
					assert(nextNode!=node);
					node = nextNode;
					root = root_ref[0];
					
					bit = (bit + 1) & (/*sizeof(unsigned)*/4 * 8 - 1);
				}
				update(node);
				++opath;
			}
			while ((--passes) > 0);
		}
	}

	public Node insert(DbvtAabbMm box, DbvtProxy data) {
		var leaf = set(this, null, box, data);
		insertleaf(this, root, leaf);
		leaves++;
		return leaf;
	}

	private void update(Node leaf) {
		update(leaf, -1);
	}

	private void update(Node leaf, int lookahead) {
		var root = removeleaf(this, leaf);
		if (root != null) {
			if (lookahead >= 0) {
				for (var i = 0; (i < lookahead) && root.parent != null; i++) {
					root = root.parent;
				}
			}
			else {
				root = this.root;
			}
		}
		insertleaf(this, root, leaf);
	}

	public void update(Node leaf, DbvtAabbMm volume) {
		var root = removeleaf(this, leaf);
		if (root != null) {
			if (lkhd >= 0) {
				for (var i = 0; (i < lkhd) && root.parent != null; i++) {
					root = root.parent;
				}
			}
			else {
				root = this.root;
			}
		}
		leaf.volume.set(volume);
		insertleaf(this, root, leaf);
	}

	public boolean update(Node leaf, DbvtAabbMm volume, v3 velocity, float margin) {
		if (leaf.volume.Contain(volume)) {
			return false;
		}
		var tmp = new v3();
		tmp.set(margin, margin, margin);
		volume.Expand(tmp);
		volume.SignedExpand(velocity);
		update(leaf, volume);
		return true;
	}

	public boolean update(Node leaf, DbvtAabbMm volume, v3 velocity) {
		if (leaf.volume.Contain(volume)) {
			return false;
		}
		volume.SignedExpand(velocity);
		update(leaf, volume);
		return true;
	}

	public boolean update(Node leaf, DbvtAabbMm volume, float margin) {
		if (leaf.volume.Contain(volume)) {
			return false;
		}
		var tmp = new v3();
		tmp.set(margin, margin, margin);
		volume.Expand(tmp);
		update(leaf, volume);
		return true;
	}

	public void remove(Node leaf) {
		removeleaf(this, leaf);
		deletenode(this, leaf);
		leaves--;
	}

	public static void write(IWriter iwriter) {
		throw new UnsupportedOperationException();
	}

	public static void clone(Dbvt dest) {
		clone(dest, null);
	}

	private static void clone(Dbvt dest, IClone iclone) {
		throw new UnsupportedOperationException();
	}

	private static int countLeaves(Node node) {
		if (node.isinternal()) {
			return countLeaves(node.childs[0]) + countLeaves(node.childs[1]);
		}
		else {
			return 1;
		}
	}

	private static void extractLeaves(Node node, OArrayList<Node> leaves) {
		if (node.isinternal()) {
			extractLeaves(node.childs[0], leaves);
			extractLeaves(node.childs[1], leaves);
		}
		else {
			leaves.add(node);
		}
	}

	private static void enumNodes(Node root, ICollide policy) {
		
		policy.process(root);
		if (root.isinternal()) {
			enumNodes(root.childs[0], policy);
			enumNodes(root.childs[1], policy);
		}
	}

	private static void enumLeaves(Node root, ICollide policy) {
		
		if (root.isinternal()) {
			enumLeaves(root.childs[0], policy);
			enumLeaves(root.childs[1], policy);
		}
		else {
			policy.process(root);
		}
	}

	
	private static Node[] sStkNN(Node... x) { return x; }

	public static void collideTT(Node root0, Node root1, ICollide policy, OArrayList<Node[]> stack) {
		
		if (root0 != null && root1 != null) {
			stack.add(sStkNN(root0, root1));
			do {
				var p = stack.removeLast();
				var pa = p[0];
				var pac = pa.childs;
				var pb = p[1];
				if (pa == pb) {
					if (pa.isinternal()) {
						stack.addAll(sStkNN(pac[0], pac[0]),sStkNN(pac[1], pac[1]),sStkNN(pac[0], pac[1]));
					}
				}
				else if (DbvtAabbMm.intersect(pa.volume, pb.volume)) {
					var pbc = pb.childs;
					if (pa.isinternal()) {
						if (pb.isinternal()) {
							stack.addAll(sStkNN(pac[0], pbc[0]),sStkNN(pac[1], pbc[0]),sStkNN(pac[0], pbc[1]),sStkNN(pac[1], pbc[1]));
						}
						else {
							stack.addAll(sStkNN(pac[0], pb),sStkNN(pac[1], pb));
						}
					}
					else {
						if (pb.isinternal()) {
							stack.addAll(sStkNN(pa, pbc[0]),sStkNN(pa, pbc[1]));
						}
						else {
							policy.process(pa, pb);
						}
					}
				}
			}
			while (!stack.isEmpty());
		}
	}

	private static void collideTT(Node root0, Node root1, Transform xform, ICollide policy) {
		
		if (root0 != null && root1 != null) {
			var stack = new OArrayList<sStkNN>(DOUBLE_STACKSIZE);
			stack.add(new sStkNN(root0, root1));
			do {
				var p = stack.remove(stack.size() - 1);
				if (p.a == p.b) {
					if (p.a.isinternal()) {
						stack.add(new sStkNN(p.a.childs[0], p.a.childs[0]));
						stack.add(new sStkNN(p.a.childs[1], p.a.childs[1]));
						stack.add(new sStkNN(p.a.childs[0], p.a.childs[1]));
					}
				}
				else if (DbvtAabbMm.intersect(p.a.volume, p.b.volume, xform)) {
					if (p.a.isinternal()) {
						if (p.b.isinternal()) {
							stack.add(new sStkNN(p.a.childs[0], p.b.childs[0]));
							stack.add(new sStkNN(p.a.childs[1], p.b.childs[0]));
							stack.add(new sStkNN(p.a.childs[0], p.b.childs[1]));
							stack.add(new sStkNN(p.a.childs[1], p.b.childs[1]));
						}
						else {
							stack.add(new sStkNN(p.a.childs[0], p.b));
							stack.add(new sStkNN(p.a.childs[1], p.b));
						}
					}
					else {
						if (p.b.isinternal()) {
							stack.add(new sStkNN(p.a, p.b.childs[0]));
							stack.add(new sStkNN(p.a, p.b.childs[1]));
						}
						else {
							policy.process(p.a, p.b);
						}
					}
				}
			}
			while (!stack.isEmpty());
		}
	}

	public static void collideTT(Node root0, Transform xform0, Node root1, Transform xform1, ICollide policy) {
		var xform = new Transform();
		xform.invert(xform0);
		xform.mul(xform1);
		collideTT(root0, root1, xform, policy);
	}

	public static void collideTV(Node root, DbvtAabbMm volume, ICollide policy) {
		
		if (root != null) {
			var stack = new OArrayList<Node>(SIMPLE_STACKSIZE);
			stack.add(root);
			do {
				var n = stack.remove(stack.size() - 1);
				if (DbvtAabbMm.intersect(n.volume, volume)) {
					if (n.isinternal()) {
						stack.add(n.childs[0]);
						stack.add(n.childs[1]);
					}
					else {
						policy.process(n);
					}
				}
			}
			while (!stack.isEmpty());
		}
	}

	public static void collideRAY(Node root, v3 origin, v3 direction, ICollide policy) {
		
		if (root != null) {
			var normal = new v3();
			normal.normalize(direction);
			var invdir = new v3();
			invdir.set(1f / normal.x, 1f / normal.y, 1f / normal.z);
			int[] signs = { direction.x<0 ? 1:0, direction.y<0 ? 1:0, direction.z<0 ? 1:0 };
			var stack = new OArrayList<Node>(SIMPLE_STACKSIZE);
			stack.add(root);
			do {
				var node = stack.remove(stack.size() - 1);
				if (DbvtAabbMm.intersect(node.volume, origin, invdir, signs)) {
					if (node.isinternal()) {
						stack.add(node.childs[0]);
						stack.add(node.childs[1]);
					}
					else {
						policy.process(node);
					}
				}
			}
			while (!stack.isEmpty());
		}
	}

	public static void collideKDOP(Node root, v3[] normals, float[] offsets, int count, ICollide policy) {
		
		if (root != null) {
            assert (count < (/*sizeof(signs)*/128 / /*sizeof(signs[0])*/ 4));
			var signs = new int[4 * 8];
            for (var i = 0; i<count; ++i) {
				signs[i] = ((normals[i].x >= 0) ? 1 : 0) +
						((normals[i].y >= 0) ? 2 : 0) +
						((normals[i].z >= 0) ? 4 : 0);
			}
			var stack = new OArrayList<sStkNP>(SIMPLE_STACKSIZE);
            stack.add(new sStkNP(root, 0));
			var inside = (1 << count) - 1;
            do {
				var se = stack.remove(stack.size() - 1);
				var out = false;
				for (int i = 0, j = 1; (!out) && (i < count); ++i, j <<= 1) {
					if (0 == (se.mask & j)) {
						var side = se.node.volume.Classify(normals[i], offsets[i], signs[i]);
						switch (side) {
							case -1:
								out = true;
								break;
							case +1:
								se.mask |= j;
								break;
						}
					}
				}
				if (!out) {
					if ((se.mask != inside) && (se.node.isinternal())) {
						stack.add(new sStkNP(se.node.childs[0], se.mask));
						stack.add(new sStkNP(se.node.childs[1], se.mask));
					}
					else {
						if (ICollide.AllLeaves(se.node)) {
							enumLeaves(se.node, policy);
						}
					}
				}
			}
			while (!stack.isEmpty());
		}
	}

//	public static void collideOCL(Node root, v3[] normals, float[] offsets, v3 sortaxis, int count, ICollide policy) {
//		collideOCL(root, normals, offsets, sortaxis, count, policy, true);
//	}

	private static void collideOCL(Node root, v3[] normals, float[] offsets, v3 sortaxis, int count, ICollide policy, boolean fullsort) {
		
		if (root != null) {
			var srtsgns = (sortaxis.x >= 0 ? 1 : 0) +
					(sortaxis.y >= 0 ? 2 : 0) +
					(sortaxis.z >= 0 ? 4 : 0);
			var ifree = new IntArrayList();
			var stack = new IntArrayList();
            assert (count < (/*sizeof(signs)*/128 / /*sizeof(signs[0])*/ 4));
			var signs = new int[/*sizeof(unsigned)*8*/4 * 8];
            for (var i = 0; i < count; i++) {
				signs[i] = ((normals[i].x >= 0) ? 1 : 0) +
						((normals[i].y >= 0) ? 2 : 0) +
						((normals[i].z >= 0) ? 4 : 0);
			}


			var stock = new OArrayList<sStkNPS>();
            stack.add(allocate(ifree, stock, new sStkNPS(root, 0, root.volume.ProjectMinimum(sortaxis, srtsgns))));
			var inside = (1 << count) - 1;
            do {

				var id = stack.remove(stack.size() - 1);

				var se = stock.get(id);
				ifree.add(id);
				if (se.mask != inside) {
					var out = false;
					for (int i = 0, j = 1; (!out) && (i < count); ++i, j <<= 1) {
						if (0 == (se.mask & j)) {
							var side = se.node.volume.Classify(normals[i], offsets[i], signs[i]);
							switch (side) {
								case -1:
									out = true;
									break;
								case +1:
									se.mask |= j;
									break;
							}
						}
					}
					if (out) {
						continue;
					}
				}
				if (ICollide.Descent(se.node)) {
					if (se.node.isinternal()) {
						Node[] pns = {se.node.childs[0], se.node.childs[1]};
						sStkNPS[] nes = {new sStkNPS(pns[0], se.mask, pns[0].volume.ProjectMinimum(sortaxis, srtsgns)),
							new sStkNPS(pns[1], se.mask, pns[1].volume.ProjectMinimum(sortaxis, srtsgns))
						};
						var q = nes[0].value < nes[1].value ? 1 : 0;
						var j = stack.size();
						if (fullsort && (j > 0)) {
							/* Insert 0	*/
							j = nearest(stack, stock, nes[q].value, 0, stack.size());
							stack.add(0);
							
							
							
							for (var k = stack.size() - 1; k > j; --k) {
								stack.set(k, stack.get(k - 1));
							
							}
							stack.set(j, allocate(ifree, stock, nes[q]));
							/* Insert 1	*/
							j = nearest(stack, stock, nes[1 - q].value, j, stack.size());
							stack.add(0);
							
							
							
							for (var k = stack.size() - 1; k > j; --k) {
								stack.set(k, stack.get(k - 1));
							
							}
							stack.set(j, allocate(ifree, stock, nes[1 - q]));
						}
						else {
							stack.add(allocate(ifree, stock, nes[q]));
							stack.add(allocate(ifree, stock, nes[1 - q]));
						}
					}
					else {
						policy.process(se.node, se.value);
					}
				}
			}
			while (stack.size() != 0);
		}
	}

	public static void collideTU(Node root, ICollide policy) {
		
		if (root != null) {
			var stack = new OArrayList<Node>(SIMPLE_STACKSIZE);
			stack.add(root);
			do {
				var n = stack.remove(stack.size() - 1);
				if (ICollide.Descent(n)) {
					if (n.isinternal()) {
						stack.add(n.childs[0]);
						stack.add(n.childs[1]);
					}
					else {
						policy.process(n);
					}
				}
			}
			while (!stack.isEmpty());
		}
	}

	private static int nearest(IntArrayList i, OArrayList<sStkNPS> a, float v, int l, int h) {
		var m = 0;
		while (l < h) {
			m = (l + h) >> 1;
            
            if (a.get(i.get(m)).value >= v) {
				l = m + 1;
			}
			else {
				h = m;
			}
		}
		return h;
	}

	private static int allocate(IntArrayList ifree, OArrayList<sStkNPS> stock, sStkNPS value) {
		int i;
		var size = ifree.size();
		if (size > 0) {
			
			i = ifree.remove(size - 1);
            
            stock.get(i).set(value);
		}
		else {
			i = stock.size();
			stock.add(value);
		}
		return (i);
	}

	

	private static int indexof(Node node) {
		return (node.parent.childs[1] == node)? 1:0;
	}

	private static DbvtAabbMm merge(DbvtAabbMm a, DbvtAabbMm b, DbvtAabbMm out) {
		DbvtAabbMm.merge(a, b, out);
		return out;
	}

	
	private static float size(DbvtAabbMm a) {
		var edges = a.lengths(new v3());

		return (edges.x * edges.y * edges.z +
		        edges.x + edges.y + edges.z);
	}


	private static void deletenode(Dbvt pdbvt, Node node) {
		
		pdbvt.free = node;
	}

	private static void recursedeletenode(Dbvt pdbvt, Node node) {
		if (!node.isLeaf()) {
			recursedeletenode(pdbvt, node.childs[0]);
			recursedeletenode(pdbvt, node.childs[1]);
		}
		if (node == pdbvt.root) {
			pdbvt.root = null;
		}
		deletenode(pdbvt, node);
	}

	private static Node set(Dbvt pdbvt, Node parent, DbvtAabbMm volume, DbvtProxy data) {
		Node node;
		if (pdbvt.free != null) {
			node = pdbvt.free;
			pdbvt.free = null;
		}
		else {
			node = new Node();
		}
		node.parent = parent;
		node.volume.set(volume);
		node.data = data;
		node.childs[1] = null;
		return node;
	}

	private static void insertleaf(Dbvt pdbvt, Node root, Node leaf) {
		if (pdbvt.root == null) {
			pdbvt.root = leaf;
			leaf.parent = null;
		}
		else {
			if (!root.isLeaf()) {
				do {
					if (DbvtAabbMm.Proximity(root.childs[0].volume, leaf.volume) <
					    DbvtAabbMm.Proximity(root.childs[1].volume, leaf.volume)) {
						root = root.childs[0];
					}
					else {
						root = root.childs[1];
					}
				}
				while (!root.isLeaf());
			}
			var prev = root.parent;
			var node = set(pdbvt, prev, merge(leaf.volume, root.volume, new DbvtAabbMm()), null);
			if (prev != null) {
				prev.childs[indexof(root)] = node;
				node.childs[0] = root;
				root.parent = node;
				node.childs[1] = leaf;
				leaf.parent = node;
				do {
					if (!prev.volume.Contain(node.volume)) {
						DbvtAabbMm.merge(prev.childs[0].volume, prev.childs[1].volume, prev.volume);
					}
					else {
						break;
					}
					node = prev;
				}
				while (null != (prev = node.parent));
			}
			else {
				node.childs[0] = root;
				root.parent = node;
				node.childs[1] = leaf;
				leaf.parent = node;
				pdbvt.root = node;
			}
		}
	}

	private static Node removeleaf(Dbvt pdbvt, Node leaf) {
		if (leaf == pdbvt.root) {
			pdbvt.root = null;
			return null;
		}
		else {
			var parent = leaf.parent;
			var prev = parent.parent;
			var sibling = parent.childs[1 - indexof(leaf)];
			if (prev != null) {
				prev.childs[indexof(parent)] = sibling;
				sibling.parent = prev;
				deletenode(pdbvt, parent);
				while (prev != null) {
					var pb = prev.volume;
					DbvtAabbMm.merge(prev.childs[0].volume, prev.childs[1].volume, prev.volume);
					if (DbvtAabbMm.NotEqual(pb, prev.volume)) {
						prev = prev.parent;
					}
					else {
						break;
					}
				}
				return (prev != null? prev : pdbvt.root);
			}
			else {
				pdbvt.root = sibling;
				sibling.parent = null;
				deletenode(pdbvt, parent);
				return pdbvt.root;
			}
		}
	}

	private static void fetchleaves(Dbvt pdbvt, Node root, FasterList<Node> leaves) {
		fetchleaves(pdbvt, root, leaves, -1);
	}

	private static void fetchleaves(Dbvt pdbvt, Node root, FasterList<Node> leaves, int depth) {
		if (root.isinternal() && depth != 0) {
			fetchleaves(pdbvt, root.childs[0], leaves, depth - 1);
			fetchleaves(pdbvt, root.childs[1], leaves, depth - 1);
			deletenode(pdbvt, root);
		}
		else {
			leaves.add(root);
		}
	}

	private static void split(FasterList<Node> leaves, FasterList<Node> left, FasterList<Node> right, v3 org, v3 axis) {
		var tmp = new v3();
		MiscUtil.resize(left, 0, Node.class);
		MiscUtil.resize(right, 0, Node.class);
		for (var li : leaves) {

			li.volume.center(tmp);
			tmp.sub(org);
			((axis.dot(tmp) < 0f) ? left : right).add(li);
		}
	}

	private static DbvtAabbMm bounds(List<Node> leaves) {

		var volume = new DbvtAabbMm(leaves.get(0).volume);
		for (int i=1, ni=leaves.size(); i<ni; i++) {
            
            merge(volume, leaves.get(i).volume, volume);
		}
		return volume;
	}

	private static void bottomup(Dbvt pdbvt, FasterList<Node> leaves) {
		var tmpVolume = new DbvtAabbMm();
		int num;
        while ((num  = leaves.size()) > 1) {
			var minsize = BulletGlobals.SIMD_INFINITY;
			int[] minidx = { -1, -1 };
			for (var i = 0; i< num; i++) {
				var li = leaves.get(i);
				for (var j = i+1; j< num; j++) {


					var sz = size(merge(li.volume, leaves.get(j).volume, tmpVolume));
					if (sz < minsize) {
						minsize = sz;
						minidx[0] = i;
						minidx[1] = j;
					}
				}
			}
            
            
            Node[] n = {leaves.get(minidx[0]), leaves.get(minidx[1])};
			var p = set(pdbvt, null, merge(n[0].volume, n[1].volume, new DbvtAabbMm()), null);
			p.childs[0] = n[0];
			p.childs[1] = n[1];
			n[0].parent = p;
			n[1].parent = p;
			
			leaves.setFast(minidx[0], p);
			Collections.swap(leaves, minidx[1], num - 1);
			leaves.removeFast(num - 1);
		}
	}

	private static final v3[] axis = { new v3(1, 0, 0), new v3(0, 1, 0), new v3(0, 0, 1) };

	private static Node topdown(Dbvt pdbvt, FasterList<Node> leaves, int bu_treshold) {
		if (leaves.size() > 1) {
			if (leaves.size() > bu_treshold) {
				var vol = bounds(leaves);
				var org = vol.center(new v3());
				var sets = IntStream.range(0, 2).mapToObj(i -> new FasterList()).toArray(FasterList[]::new);
				var bestmidp = leaves.size();
				int[][] splitcount = {{0, 0}, {0, 0}, {0, 0}};

				var x = new v3();

				for (var leave : leaves) {

					leave.volume.center(x);
					x.sub(org);
					for (var j = 0; j < 3; j++) {
						splitcount[j][x.dot(axis[j]) > 0f ? 1 : 0]++;
					}
				}
				var bestaxis = -1;
                for (var i = 0; i<3; i++) {
					if ((splitcount[i][0] > 0) && (splitcount[i][1] > 0)) {
						var midp = Math.abs(splitcount[i][0] - splitcount[i][1]);
						if (midp < bestmidp) {
							bestaxis = i;
							bestmidp = midp;
						}
					}
				}
				if (bestaxis >= 0) {
					
					
					split(leaves, sets[0], sets[1], org, axis[bestaxis]);
				}
				else {
					
					
					for (int i=0, ni=leaves.size(); i<ni; i++) {
                        
                        sets[i & 1].add(leaves.get(i));
					}
				}
				var node = set(pdbvt, null, vol, null);
				node.childs[0] = topdown(pdbvt, sets[0], bu_treshold);
				node.childs[1] = topdown(pdbvt, sets[1], bu_treshold);
				node.childs[0].parent = node;
				node.childs[1].parent = node;
				return node;
			}
			else {
				bottomup(pdbvt, leaves);
                return leaves.get(0);
                
            }
		}
        return leaves.get(0);
        
    }

	private static Node sort(Node n, Node[] r) {
		var p = n.parent;
		
		
		if (p != null && p.hashCode() > n.hashCode()) {
			var i = indexof(n);
			var j = 1 - i;
			var s = p.childs[j];
			var q = p.parent;
			assert (n == p.childs[i]);
			if (q != null) {
				q.childs[indexof(p)] = n;
			}
			else {
				r[0] = n;
			}
			s.parent = n;
			p.parent = n;
			n.parent = q;
			p.childs[0] = n.childs[0];
			p.childs[1] = n.childs[1];
			n.childs[0].parent = p;
			n.childs[1].parent = p;
			n.childs[i] = p;
			n.childs[j] = s;

			DbvtAabbMm.swap(p.volume, n.volume);
			return p;
		}
		return n;
	}

	private static Node walkup(Node n, int count) {
		while (n != null && (count--) != 0) {
			n = n.parent;
		}
		return n;
	}

	

	public static final class Node {
		public final DbvtAabbMm volume = new DbvtAabbMm();
		Node parent;
		final Node[] childs = new Node[2];
		public DbvtProxy data;

		boolean isLeaf() {
			return data!=null;
			
		}

		boolean isinternal() {
			return !isLeaf();
		}

		/** recursively collets all leaves  */
		public final void leaves(Collection<Collidable> l) {
			if (data!=null) {
				l.add(data.data);
			} else {
				for (var x : childs)
					x.leaves(l);
			}
		}
	}
	
	/** Stack element */
	static final class sStkNN {
		final Node a;
		final Node b;

		sStkNN(Node na, Node nb) {
			a = na;
			b = nb;
		}
	}

	static final class sStkNP {
		final Node node;
		int mask;

		sStkNP(Node n, int m) {
			node = n;
			mask = m;
		}
	}

	static final class sStkNPS {
		Node node;
		int mask;
		float value;

		sStkNPS(Node n, int m, float v) {
			node = n;
			mask = m;
			value = v;
		}
		
		void set(sStkNPS o) {
			node = o.node;
			mask = o.mask;
			value = o.value;
		}
	}
	










	public abstract static class ICollide {
		void process(Node n1, Node n2) {

		}

		void process(Node n) {
		}

		void process(Node n, float f) {
			process(n);
		}

		static boolean Descent(Node n) {
			return true;
		}

		static boolean AllLeaves(Node n) {
			return true;
		}
	}

	public abstract static class IWriter {
		public abstract void Prepare(Node root, int numnodes);
		public abstract void WriteNode(Node n, int index, int parent, int child0, int child1);
		public abstract void WriteLeaf(Node n, int index, int parent);
	}
	
	private static class IClone {
		public void CloneLeaf(Node n) {
		}
	}
	
}
