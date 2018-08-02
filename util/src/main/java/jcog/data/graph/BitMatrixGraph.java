/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package jcog.data.graph;

import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * This class implements a graph which uses a bitmatrix as inner representation
 * of edges.
 */
public class BitMatrixGraph implements Graph {





    private final List<BitSet> sets;

    private final boolean directed;





    /**
     * Constructs a directed graph with the given number of nodes.
     * The graph has no edges initially. The graph is directed.
     *
     * @param n size of graph
     */
    public BitMatrixGraph(int n) {

        this(n, true);
    }



    /**
     * Constructs an graph with the given number of nodes.
     * The graph has no edges initially.
     *
     * @param n        size of graph
     * @param directed if true graph is directed
     */
    private BitMatrixGraph(int n, boolean directed) {

        sets = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) sets.add(new BitSet());
        this.directed = directed;
    }






    @Override
    public boolean isEdge(int i, int j) {

        return sets.get(i).get(j);
    }



    @Override
    public IntHashSet neighborsOut(int i) {

        IntHashSet result = new IntHashSet();
        BitSet neighb = sets.get(i);
        final int max = size();
        for (int j = 0; j < max; ++j) {
            if (neighb.get(j)) result.add(j);
        }

        return result;
    }



    /**
     * Returns null always
     */
    @Override
    public Object vertex(int v) {
        return null;
    }



    /**
     * Returns null always.
     */
    @Override
    public Object edge(int i, int j) {
        return null;
    }



    @Override
    public int size() {
        return sets.size();
    }



    @Override
    public boolean directed() {
        return directed;
    }



    @Override
    public boolean setEdge(int i, int j) {

        if (i > size() || j > size() || i < 0 || j < 0) throw new
                IndexOutOfBoundsException();

        BitSet neighb = sets.get(i);
        boolean old = neighb.get(j);
        neighb.set(j);

        if (!old && !directed) {
            neighb = sets.get(j);
            neighb.set(i);
        }

        return !old;
    }



    @Override
    public boolean removeEdge(int i, int j) {

        if (i > size() || j > size() || i < 0 || j < 0) throw new
                IndexOutOfBoundsException();

        BitSet neighb = sets.get(i);
        boolean old = neighb.get(j);
        neighb.clear(j);

        if (old && !directed) {
            neighb = sets.get(i);
            neighb.clear(j);
        }

        return old;
    }



    @Override
    public int degree(int i) {

        BitSet neighb = sets.get(i);
        return neighb.cardinality(); 
    }

}

