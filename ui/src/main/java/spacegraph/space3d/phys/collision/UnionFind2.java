package spacegraph.space3d.phys.collision;

import spacegraph.space3d.phys.math.MiscUtil;

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



/**
 * an optimized version of UnionFind but is not working yet
 */
public class UnionFind2 {

    

    private int[][] ele = new int[0][2];
    private int numElements;

    /**
     * This is a special operation, destroying the content of UnionFind.
     * It sorts the elements, based on island id, in order to make it easy to iterate over islands.
     */
    public void sortIslands() {


        var elements = this.ele;
        var n = this.numElements;
        for (var i = 0; i < n; i++) {

            var e = elements[i];
            e[0] = find(i);
            e[1] = i;
        }

        
        
        
        

        
        MiscUtil.quickSort(elements, n);
    }

    public void reset(int N) {
        if (ele == null || ele.length < N) {
            ele = new int[N][2];
        }

        numElements = N;

        var ee = this.ele;
        for (var i = 0; i < N; i++) {
            var e = ee[i];
            e[0] = i;
            e[1] = 1;
        }

        
        for (var j = N; j < ele.length; j++) {
            var e = ee[j];
            e[0] = -1;
            e[1] = -1;
        }
    }

    public int size() {
        return numElements;
    }

    public boolean isRoot(int x) {
        
        return (x == ele[x][0]);
    }

    public void free() {
        numElements = 0;
    }

    public int find(int p, int q) {
        return (find(p) == find(q))? 1 : 0;
    }

    public void unite(int p, int q) {

        if (p == -1 || q == -1) return; 

        int i = find(p), j = find(q);
        if (i == j) {
            return;
        }


        var ei = ele[i];

        ei[0] = j;
        
        
        ele[j][1] += ei[1];
        
    }

    public int find(int x) {


        var numElements = this.numElements;


        var e = this.ele;
        while (x != e[x][0]) {


            var i = e[x][0];
            if (!valid(i, numElements))
                return x;

            e[x][0] = e[i][0];
            
            
            x = e[x][0];
            
            

            if (!valid(x, numElements))
                return x;
        }
        return x;
    }

    void valid(int x) {
        valid(x, numElements);
    }

    private static boolean valid(int x, int numElements) {

        return x >= 0 && (x < numElements);
            
    }

    public final int id(int i) {
        
        
        return ele[i][0];
    }
    public final int sz(int i) {
        
        
        return ele[i][1];
    }

    













}
