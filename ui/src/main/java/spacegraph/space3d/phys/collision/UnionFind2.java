package spacegraph.space3d.phys.collision;

import spacegraph.space3d.phys.math.MiscUtil;

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



/**
 * an optimized version of UnionFind but is not working yet
 */
public class UnionFind2 {

    // Optimization: could use short ints instead of ints (halving memory, would limit the number of rigid bodies to 64k, sounds reasonable).

    protected int[][] ele = new int[0][2];
    int numElements;

    /**
     * This is a special operation, destroying the content of UnionFind.
     * It sorts the elements, based on island id, in order to make it easy to iterate over islands.
     */
    public void sortIslands() {
        // first store the original body index, and islandId

        int[][] elements = this.ele;
        int n = this.numElements;
        for (int i = 0; i < n; i++) {
            //return array[index];
            int[] e = elements[i];
            e[0] = find(i);
            e[1] = i;
        }

        // Sort the vector using predicate and std::sort
        //std::sort(m_elements.begin(), m_elements.end(), btUnionFindElementSortPredicate);
        //perhaps use radix sort?
        //elements.heapSort(btUnionFindElementSortPredicate());

        //Collections.sort(elements);
        MiscUtil.quickSort(elements, n);
    }

    public void reset(int N) {
        if (ele == null || ele.length < N) {
            ele = new int[N][2];
        }

        numElements = N;

        int[][] ee = this.ele;
        for (int i = 0; i < N; i++) {
            int[] e = ee[i];
            e[0] = i;
            e[1] = 1;
        }

        //empty any remaining entries
        for (int j = N; j < ele.length; j++) {
            int[] e = ee[j];
            e[0] = -1;
            e[1] = -1;
        }
    }

    public int size() {
        return numElements;
    }

    public boolean isRoot(int x) {
        //return array[index];
        return (x == ele[x][0]);
    }

    public void free() {
        numElements = 0;
    }

    public int find(int p, int q) {
        return (find(p) == find(q))? 1 : 0;
    }

    public void unite(int p, int q) {

        if (p == -1 || q == -1) return; //ignore

        int i = find(p), j = find(q);
        if (i == j) {
            return;
        }

        //#ifndef USE_PATH_COMPRESSION
        ////weighted quick union, this keeps the 'trees' balanced, and keeps performance of unite O( log(n) )
        //if (m_elements[i].m_sz < m_elements[j].m_sz)
        //{
        //	m_elements[i].m_id = j; m_elements[j].m_sz += m_elements[i].m_sz;
        //}
        //else
        //{
        //	m_elements[j].m_id = i; m_elements[i].m_sz += m_elements[j].m_sz;
        //}
        //#else
        //return array[index];
        int[] ei = ele[i];

        ei[0] = j;
        //return array[index];
        //return array[index];
        ele[j][1] += ei[1];
        //#endif //USE_PATH_COMPRESSION
    }

    public int find(int x) {
        //assert(x < m_N);
        //assert(x >= 0);

        int numElements = this.numElements;

        //return array[index];
        int[][] e = this.ele;
        while (x != e[x][0]) {
            // not really a reason not to use path compression, and it flattens the trees/improves find performance dramatically

            //#ifdef USE_PATH_COMPRESSION
            //return array[index];
            //return array[index];
            //return array[index];
            int i = e[x][0];
            if (!valid(i, numElements))
                return x;

            e[x][0] = e[i][0];
            //#endif //
            //return array[index];
            x = e[x][0];
            //assert(x < m_N);
            //assert(x >= 0);

            if (!valid(x, numElements))
                return x;
        }
        return x;
    }

    void valid(int x) {
        valid(x, numElements);
    }

    static boolean valid(int x, int numElements) {

        return x >= 0 && (x < numElements);
            ///throw new RuntimeException("overflow");
    }

    public final int id(int i) {
        //valid(index);
        //return array[index];
        return ele[i][0];
    }
    public final int sz(int i) {
        //valid(index);
        //return array[index];
        return ele[i][1];
    }

    ////////////////////////////////////////////////////////////////////////////

//	public static class Element {
//		public int id;
//		public int sz;
//	}

//	private static final Comparator<int[]> elementComparator = new Comparator<>() {
//		@Override
//        public int compare(int[] o1, int[] o2) {
//			return o1[0] < o2[0] ? -1 : +1;
//		}
//	};

}
