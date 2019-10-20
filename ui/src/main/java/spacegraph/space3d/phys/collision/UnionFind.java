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

import jcog.data.list.FasterList;
import spacegraph.space3d.phys.math.MiscUtil;

import java.util.Comparator;

/**
 * UnionFind calculates connected subsets. Implements weighted Quick Union with
 * path compression.
 *
 * @author jezek2
 */
public class UnionFind {

	

	private final FasterList<Element> elements = new FasterList<>();

	/**
	 * This is a special operation, destroying the content of UnionFind.
	 * It sorts the elements, based on island id, in order to make it easy to iterate over islands.
	 */
	public void sortIslands() {

        int numElements = elements.size();

		for (int i = 0; i < numElements; i++) {
			
			elements.get(i).id = find(i);
			
			elements.get(i).sz = i;
		}

		
		
		
		

		
		MiscUtil.quickSort(elements, elementComparator);
	}

	public void reset(int N) {
		allocate(N);

		for (int i = 0; i < N; i++) {
			
			elements.get(i).id = i;
			
			elements.get(i).sz = 1;
		}
	}

	public int size() {
		return elements.size();
	}

	public boolean isRoot(int x) {
		
		return (x == elements.get(x).id);
	}

	private Element get(int index) {
		return elements.get(index);
		
	}

	private void allocate(int N) {
		MiscUtil.resize(elements, N, Element.class);
	}

	public void free() {
		elements.clearFast();
	}

	public int find(int p, int q) {
		return (find(p) == find(q))? 1 : 0;
	}

	public void unite(int p, int q) {
		int i = find(p), j = find(q);
		if (i == j) {
			return;
		}

		
		
		
		
		
		
		
		
		
		
		
		
		elements.get(i).id = j;
		
		
		elements.get(j).sz += elements.get(i).sz;
		
	}

	private int find(int x) {
		
		

		
		while (x != elements.get(x).id) {
			

			
			
			
			
			elements.get(x).id = elements.get(elements.get(x).id).id;
			
			
			x = elements.get(x).id;
			
			
		}
		return x;
	}

	public final int getSz(int i) {
		return get(i).sz;
	}

	public int getId(int i) {
		return get(i).id;
	}

	

	public static class Element {
		int id;
		int sz;
	}

	private static final Comparator<Element> elementComparator = Comparator.comparingInt(o -> o.id);

}
