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

import com.google.common.base.Joiner;
import jcog.func.TriConsumer;
import jcog.func.TriFunction;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implements a graph which uses the neighbour list representation.
 * No multiple edges are allowed. The implementation also supports the
 * growing of the graph. This is very useful when the number of nodes is
 * not known in advance or when we construct a graph reading a file.
 */
public class AdjGraph<V, E> implements Graph<V, E>, java.io.Serializable {




    private int serial;

    public V node(int i) {
        return antinodes.get(i).v;
    }

    public boolean addIfNew(V s) {
        if (this.node(s)==-1) {
            _addNode(s);
            return true;
        }
        return false;
    }

    public Set<Node<V>> nodeSet() {
        return nodes.keySet();
    }

    public int node(V node) {
        return nodes.getIfAbsent(node, -1);
    }

    /**
     * Contains sets of node indexes. If "nodes" is not null, indices are
     * defined by "nodes", otherwise they correspond to 0,1,...
     */
    public static class Node<V> {
        public final V v;
        final IntHashSet e;
        final int id;

        Node(V v, int id) {
            super();
            this.v = v;
            this.id = id;
            this.e = new IntHashSet(0);
        }

        @Override
        public String toString() {
            return v.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || v.equals(obj) || (obj instanceof Node && v.equals(((Node) obj).v));
        }

        @Override
        public int hashCode() {
            return v.hashCode();
        }
    }


    /**
     * Contains the indices of the nodes. The vector "nodes" contains this
     * information implicitly but this way we can find indexes in log time at
     * the cost of memory (node that the edge lists typically use much more memory
     * than this anyway). Note that the nodes vector is still necessary to
     * provide constant access to nodes based on indexes.
     */
    public final ObjectIntHashMap<Node<V>> nodes;
    private final IntObjectHashMap<Node<V>> antinodes;


    /**
     * edge values indexed by their unique 64-bit id formed from the id's of the src (32bit int) and target (32bit int)
     */
    private final LongObjectHashMap<E> edges;


    /**
     * Indicates if the graph is directed.
     */
    private final boolean directed;




    /**
     * Constructs an empty graph. That is, the graph has zero nodes, but any
     * number of nodes and edges can be added later.
     *
     * @param directed if true the graph will be directed
     */
    public AdjGraph(boolean directed) {
        this(directed, 0, 0);
    }

    private AdjGraph(boolean directed, int nodeCap, int edgeCap) {
        edges = new LongObjectHashMap<>(edgeCap);
        nodes = new ObjectIntHashMap<>(nodeCap);
        antinodes = new IntObjectHashMap(nodeCap);
        this.directed = directed;
    }





    /**
     * If the given object is not associated with a node yet, adds a new
     * node. Returns the index of the node. If the graph was constructed to have
     * a specific size, it is not possible to add nodes and therefore calling
     * this method will throw an exception.
     *
     * @throws NullPointerException if the size was specified at construction time.
     */
    public int addNode(V o) {


        int index = node(o);
        if (index == -1) {
            return _addNode(o);
        }

        return index;
    }

    private int _addNode(V o) {
        int id = serial++;
        Node n = new Node(o, id);
        nodes.put(n, id);
        antinodes.put(id, n);
        return id;
    }






    @Override
    public boolean setEdge(int i, int j) {
        throw new UnsupportedOperationException();
    }

    public boolean setEdge(V i, V j, E value) {
        int ii = node(i);
        if (ii != -1) {
            int jj = node(j);
            if (jj != -1) {
                return setEdge(ii, jj, value);
            }
        }
        return false;
    }

    @Override
    public boolean setEdge(int i, int j, E value) {
        boolean ret = antinodes.get(i).e.add(j);
        if (ret && !directed) antinodes.get(j).e.add(i);
        edges.put(eid(i, j), value);
        return ret;
    }

    /**
     * Returns null always.
     */
    @Override
    public @Nullable E edge(int i, int j) {
        return edges.get(eid(i, j));
    }

    public @Nullable E edge(V i, V j) {
        int ii = node(i);
        if (ii != -1) {
            int jj = node(j);
            if (jj != -1) {
                return edge(ii, jj);
            }
        }
        return null;
    }


    


    @Override
    public String toString() {
        return nodes + " * " + Joiner.on(",").join(StreamSupport.stream(edges.keyValuesView().spliterator(), false).map((e) -> {
            long id = e.getOne();
            return (id >> 32) + "->" + (id & 0xffffffffL) + '=' + e.getTwo();
        }).collect(Collectors.toList()));
    }

    @Override
    public boolean removeEdge(int i, int j) {
        boolean ret = antinodes.get(i).e.remove(j);
        if (ret && !directed) {
            boolean ret2 = antinodes.get(j).e.remove(i);
            assert (ret2);
        }
        boolean ret3 = edges.remove(eid(i, j)) != null;
        assert (ret3);
        return ret;
    }

    public final long eid(int i, int j) {
        if (!directed) {
            if (j < i) {
                int x = j;
                j = i;
                i = x;
            }
        }
        long x = i;
        x <<= 32;
        x |= j;
        return x;
    }



    @Override
    public boolean isEdge(int i, int j) {
        return antinodes.get(i).e.contains(j);
    }



    @Override
    public IntHashSet neighborsOut(int i) {
        return antinodes.get(i).e;
    }







    public void neighborEdges(V v, BiConsumer<V,E> each) {
        int i = node(v);
        if (i < 0)
            return;
        antinodes.get(i).e.forEach(ee -> {
            E ej = edge(i, ee);
            Node<V> nohd = antinodes.get(ee);
            each.accept(nohd.v, ej);
        });
    }
   public void neighborEdges(V v, BiFunction<V,E,E> each) {
        int i = node(v);
        if (i < 0)
            return;
        antinodes.get(i).e.forEach(ee -> {
            E ej = edge(i, ee);
            Node<V> nohd = antinodes.get(ee);
            E ej2 = each.apply(nohd.v, ej);
            if (ej2!=ej)
                setEdge(i, ee, ej2);
        });
    }


    /**
     * If the graph was gradually grown using {@link #addNode}, returns the
     * object associated with the node, otherwise null
     */
    @Override
    public V vertex(int v) {
        return nodes == null ? null : antinodes.get(v).v;
    }






    @Override
    public int size() {
        return nodes.size();
    }



    @Override
    public boolean directed() {
        return directed;
    }



    @Override
    public int degree(int i) {
        return antinodes.get(i).e.size();
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public E edge(V s, V p, E ifMissing) {
        @Nullable E existing = edge(s, p);
        return Optional.ofNullable(existing).orElse(ifMissing);
    }

    public E edge(int s, int p, E ifMissing) {
        return edge(s, p, ()->ifMissing);
    }

    private E edge(int s, int p, Supplier<E> ifMissing) {
        @Nullable E existing = edge(s, p);
        if (existing != null)
            return existing;
        else {
            E ee = ifMissing.get();
            setEdge(s, p, ee);
            return ee;
        }
    }

    public boolean removeEdge(V i, V j) {
        int ii = node(i);
        if (ii != -1) {
            int jj = node(j);
            if (jj != -1) {
                return removeEdge(ii, jj);
            }
        }
        return false;
    }

    private void eachNode(TriConsumer<Node<V>, Node<V>, E> edge) {
        edges.forEachKeyValue((eid, ev) -> {
            int a = (int) (eid >> 32);
            int b = (int) (eid);
            Node<V> na = antinodes.get(a);
            Node<V> nb = antinodes.get(b);
            if (na == null || nb == null) {
                throw new RuntimeException("oob");
            }
            edge.accept(na, nb, ev);
        });

    }

    /**
     * returns true if modified
     */
    private void each(TriConsumer<V, V, E> edge) {
        eachNode((an, bn, e) -> edge.accept(an.v, bn.v, e));
    }


    public AdjGraph<V, E> compact() {
        return compact((x, y, z) -> z);
    }

    private AdjGraph<V, E> compact(TriFunction<V, V, E, E> retain) {
        AdjGraph g = new AdjGraph(directed);
        each((a, b, e) -> {

            E e1 = retain.apply(a, b, e);
            if (e1 != null) {
                int aa = g.addNode(a);
                int bb = g.addNode(b);
                g.setEdge(aa, bb, e1);
            }
        });
        return g;
    }

    /**
     * Saves the given graph to
     * the given stream in GML format.
     */
    public void writeGML(PrintStream out) {

        out.println("graph [ directed " + (directed ? "1" : "0"));

        for (Node<V> k : nodes.keySet())
            out.println("node [ id " + k.id + " label \"" +
                    k.v.toString().replace('\"','\'') + "\" ]");

        eachNode((a, b, e) -> out.println(
                "edge [ source " + a.id + " target " + b.id + " label \"" +
                        e.toString().replace('\"','\'') + "\" ]"));

        out.println("]");
    }
}




