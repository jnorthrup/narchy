package jcog.data.graph;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.graph.search.Search;
import jcog.list.ArrayIterator;
import jcog.list.FasterList;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


/**
 * graph rooted in a set of vertices (nodes),
 * providing access to edges only indirectly through them
 * (ie. the edges are not stored in their own index but only secondarily as part of the vertices)
 *
 * TODO abstract into subclasses:
 *      HashNodeGraph backed by HashMap node and edge containers
 *      BagNodeGraph backed by Bag's/Bagregate's
 *
 *      then replace TermWidget/EDraw stuff with BagNodeGraph
 */
public class NodeGraph<N, E> {

    protected Map<N, Node<N, E>> nodes;

    public NodeGraph() {
        this(new LinkedHashMap<>());
    }

    public NodeGraph(Map<N, Node<N, E>> nodes) {
        this.nodes = nodes;
    }

    public void clear() {
        nodes.clear();
    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        nodes().forEach((node) -> node.print(out));
    }

    public Node<N, E> add(N key) {
        final boolean[] created = {false};
        Node<N, E> r = nodes.computeIfAbsent(key, (x) -> {
            created[0] = true;
            return newNode(x);
        });
        if (created[0]) {
            onAdd(r);
        }
        return r;
    }

    protected Node<N, E> newNode(N data) {
        return new Node<>(data);
    }

    protected void onAdd(Node<N, E> r) {

    }

    public boolean edgeAdd(N from, E data, N to) {
        Node<N, E> f = node(from);
        if (f == null)
            throw new NullPointerException();
        Node<N, E> t = node(to);
        if (t == null)
            throw new NullPointerException();
        return edgeAdd(f, data, t);
    }

    public boolean edgeAdd(Node<N, E> from, E data, Node<N, E> to) {
        Edge<N, E> ee = new Edge<>(from, to, data);
        if (from.outAdd(ee)) {
            boolean a = to.inAdd(ee);
            assert (a);
            return true;
        }
        return false;
    }

    public Node<N, E> node(Object key) {
        return nodes.get(key);
    }

    public Collection<Node<N, E>> nodes() {
        return nodes.values();
    }

    public boolean edgeRemove(Edge<N, E> e) {
        if (e.from.outRemove(e)) {
            boolean removed = e.to.inRemove(e);
            assert(removed);
            return true;
        }
        return false;
    }

    /**
     * TODO return zero-copy Iterable
     */
    public List<Node<N, E>> nodesWithIns(int x) {
        return nodesWithIns(x, true);
    }

    /**
     * TODO return zero-copy Iterable
     */
    public List<Node<N, E>> nodesWithIns(int x, boolean includeSelfLoops) {

        List<Node<N, E>> result = new ArrayList<>();
        for (Node<N, E> n : nodes()) {
            if (n.ins(includeSelfLoops) == x) {
                result.add(n);
            }
        }

        return result;

    }

//    private void markReachable(Node<N, E> startingNode) {
//        ArrayList<Node<N, E>> arr = new ArrayList<>();
//        arr.add(startingNode);
//        nodes().forEach(Node::setUnreachable);
//        traverseDFSNodes(arr, false, true, (node, path) -> {
//            node.setReachable();
//            return true;
//        });
//    }

    




    public boolean dfs(N startingNode, Search<N, E> tv) {
        return dfs(List.of(startingNode), tv);
    }
    public boolean bfs(N startingNode, Search<N, E> tv) {
        return bfs(List.of(startingNode), tv);
    }

    public boolean dfs(Iterable<N> startingNodes, Search<N, E> tv) {
        return dfsNodes(Iterables.transform(startingNodes, this::add), tv);
    }
    public boolean bfs(Iterable<N> startingNodes, Search<N, E> tv) {
        return bfsNodes(Iterables.transform(startingNodes, this::add), tv);
    }

    private boolean bfsNodes(Iterable<Node<N, E>> startingNodes, Search<N, E> search) {
        search.start();
        try {

            Queue<Pair<List<BooleanObjectPair<Edge<N, E>>>,Node<N,E>>> q = new ArrayDeque();

            for (Node n : startingNodes) {
                if (!search.bfs(n, q))
                    return false;
                q.clear();
            }

            return true;
        } finally {
            search.stop();
        }
    }
    private boolean dfsNodes(Iterable<Node<N, E>> startingNodes, Search<N, E> search) {

        search.start();
        try {

            search.path = new FasterList(8);

            for (Node n : startingNodes)
                if (!search.dfs(n))
                    return false;

            return true;
        } finally {
            search.stop();
        }
    }

//    /** dead simple stack-based depth first search */
//    protected boolean dfs(Search<N, E> search, Node<N,E> n, ) {
//        return search.visit(n, (s) ->
//                search.visit(e.path.getLast().getTwo().other(n)));
//    }




//    public boolean hasCycles() {
//
//        for (Node<N, E> n : nodes()) {
//            n.setVisited(false);
//            n.setActive(false);
//        }
//
//        for (Node<N, E> n : nodes()) {
//            if (checkCycles(n))
//                return true;
//        }
//        return false;
//    }

//    private boolean checkCycles(Node<N, E> n) {
//
//        if (n.isActive()) {
//            return true;
//        }
//
//        if (!n.isVisited()) {
//
//            n.setVisited(true);
//            n.setActive(true);
//
//            if (n.out().anyMatch(succ -> checkCycles(succ.to)))
//                return true;
//
//            n.setActive(false);
//
//        }
//
//        return false;
//    }

    public Stream<Edge<N,E>> edges() {
        return nodes().stream().flatMap(Node::outStream);
    }

    @Override
    public String toString() {

        StringBuilder s = new StringBuilder();
        s.append("Nodes: ");
        for (Node<N, E> n : nodes()) {
            s.append(n.toString()).append("\n");
        }

        s.append("Edges: ");

        edges().forEach(e -> {
            s.append(e.toString()).append("\n");
        });

        return s.toString();
    }

    /**
     *
     * @author Thomas Wuerthinger
     */
    public static class Node<N, E> {


        /** buffers a lazily updated array-backed cache of the values
         * for fast iteration and streaming */
        public static class FastIteratingHashSet<X> extends LinkedHashSet<X> {

            Object[] cache = ArrayUtils.EMPTY_OBJECT_ARRAY;


            @Override
            public boolean add(X x) {
                if (super.add(x)) {
                    cache = null;
                    return true;
                }
                return false;
            }

            @Override
            public boolean remove(Object o) {
                if (super.remove(o)) {
                    cache = null;
                    return true;
                }
                return false;
            }


            protected int update() {
                int s = size();
                if (cache == null) {
                    if (s == 0) {
                        cache = ArrayUtils.EMPTY_OBJECT_ARRAY;
                    } else {
                        cache = new Object[s];
                        Iterator<X> xx = super.iterator();
                        int i = 0;
                        while (xx.hasNext())
                            cache[i++] = xx.next();
                    }
                }
                return s;
            }

            @Override
            public void clear() {
                super.clear();
                cache = ArrayUtils.EMPTY_OBJECT_ARRAY;
            }

            @Override
            public Iterator iterator() {
                update();
                return ArrayIterator.get(cache);
            }

            @Override
            public Stream<X> stream() {
                switch (update()) {
                    case 0:
                        return Stream.empty();
                    case 1:
                        return Stream.of((X)cache[0]);
                    default:
                        return Stream.of(cache).map(x -> (X) x);
                }
            }
        }

        private final static AtomicInteger serials = new AtomicInteger(1);

        public final N id;
        public final int serial, hash;
        public final Collection<Edge<N, E>> in;
        public final Collection<Edge<N, E>> out;

        protected Node(N id) {
            this.serial = serials.getAndIncrement();
            this.id = id;
            this.in =
                    new FastIteratingHashSet<>();
                    //new HashSet<>();
            this.out =
                    //new HashSet<>();
                    new FastIteratingHashSet<>();
            this.hash = id.hashCode();
        }

        public Iterable<Edge<N, E>> edges(boolean in, boolean out) {
            if (out && !in) return this.out;
            else if (!out && in) return this.in;
            else return Iterables.concat(this.out, this.in);
        }

        @Override
        public final boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        public final int ins() {
            return ins(true);
        }

        public int ins(boolean countSelfLoops) {
            if (countSelfLoops) {
                return (int) inStream().count();
            } else {
                return (int) inStream().filter(e -> e.from!=this).count();
            }
        }

        public int outs() {
            return (int) outStream().count();
        }


        protected boolean inAdd(Edge<N, E> e) {
            return in.add(e);
        }

        protected boolean outAdd(Edge<N, E> e) {
            return out.add(e);
        }

        protected boolean inRemove(Edge<N, E> e) {
            //assert inEdges.contains(e);
            return in.remove(e);
        }

        protected boolean outRemove(Edge<N, E> e) {
            //assert outEdges.contains(e);
            return out.remove(e);
        }

        public Stream<Edge<N, E>> inStream() {
            return (in.stream());
        }

        public Stream<Edge<N, E>> outStream() {
            return (out.stream());
        }

        public Stream<N> successors() {
            return outStream().map(e -> e.to.id);
        }
        public Stream<N> predecessors() {
            return inStream().map(e -> e.from.id);
        }

        @Override
        public String toString() {
            return id.toString();
        }

        public void print(PrintStream out) {
            out.println(id);
            outStream().forEach(e -> {
               out.println("\t" + e);
            });
        }


    }

    /**
     *
     * @author Thomas Wuerthinger
     */
    public static class Edge<N, E> {

        private final int hash;
        public final E id;
        public final Node<N, E> from;
        public final Node<N, E> to;

        public Edge(Node<N, E> from, Node<N, E> to, E id) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.hash = Util.hashCombine(id.hashCode(), from.hashCode(), to.hashCode());
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof NodeGraph.Edge)) return false;
            Edge ee = (Edge) obj;
            return from == ee.from && to == ee.to && id.equals(ee.id);
        }

        public boolean isSelfLoop() {
            return from == to;
        }

    //    public void reverse() {
    //
    //        // Remove from current source / dest
    //        from.outRemove(this);
    //        to.inRemove(this);
    //
    //        Node<N, E> tmp = from;
    //        from = to;
    //        to = tmp;
    //
    //        // Add to new source / dest
    //        from.outAdd(this);
    //        to.inAdd(this);
    //    }

        @Override
        public String toString() {
            return from + " => " + id + " => " + to;
        }

        public Node<N,E> to(boolean outOrIn) {
            return outOrIn ? to : from;
        }

        public Node<N,E> from(boolean outOrIn) {
            return outOrIn ? from : to;
        }

        @Nullable
        public NodeGraph.Node<N, E> other(Node<N, E> x) {
            if (from == x) return to;
            else if (to == x) return from;
            else return null;
        }
    }
}
