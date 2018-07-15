package jcog.data.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import jcog.data.graph.search.Search;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class NodeGraph<N, E> {

    abstract public Node<N, E> node(Object key);


    abstract public void forEachNode(Consumer<Node<N,E>> n);

    protected abstract Node<N,E> newNode(N data);


    /**
     * can override in mutable subclass implementations
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * gets existing node, or creates and adds a node if missing
     * can override in mutable subclass implementations
     */
    public Node<N, E> addNode(N key) {
        throw new UnsupportedOperationException();
    }

    public boolean dfs(N startingNode, Search<N, E> tv) {
        return dfs(List.of(startingNode), tv);
    }

    public boolean bfs(N startingNode, Search<N, E> tv) {
        return bfs(List.of(startingNode), tv, new ArrayDeque());
    }

    public boolean dfs(Iterable<N> startingNodes, Search<N, E> tv) {
        return dfsNodes(Iterables.transform(startingNodes, this::node), tv);
    }

    public boolean bfs(Iterable<N> startingNodes, Search<N, E> tv, Queue<Pair<List<BooleanObjectPair<ImmutableDirectedEdge<N, E>>>, Node<N, E>>> q) {
        return bfsNodes(Iterables.transform(startingNodes, this::node), tv, q);
    }

    /** q is recycled between executions automatically. just pre-alloc it
     * ArrayDeque or similar recommended.  */
    private boolean bfsNodes(Iterable<Node<N, E>> startingNodes, Search<N, E> search, Queue<Pair<List<BooleanObjectPair<ImmutableDirectedEdge<N, E>>>, Node<N, E>>> q) {
        search.start();
        try {

            for (Node n : startingNodes) {
                q.clear();
                if (!search.bfs(n, q))
                    return false;
            }

            return true;
        } finally {
            search.stop();
            q.clear();
        }
    }

    private boolean dfsNodes(Iterable<Node<N,E>> startingNodes, Search<N, E> search) {

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


    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        forEachNode((node) -> node.print(out));
    }


    abstract public static class Node<N, E> {
        private final static AtomicInteger serials = new AtomicInteger(1);
        public final N id;
        public final int serial, hash;


        public Node(N id) {
            this.serial = serials.getAndIncrement();
            this.id = id;
            this.hash = id.hashCode();
        }

        @Override
        public final boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        public Stream<N> successors() {
            return streamOut().map(e -> e.to.id);
        }

        public Stream<N> predecessors() {
            return streamIn().map(e -> e.from.id);
        }

        @Override
        public String toString() {
            return id.toString();
        }

        public void print(PrintStream out) {
            out.println(id);
            streamOut().forEach(e -> {
                out.println("\t" + e);
            });
        }

        public Stream<ImmutableDirectedEdge<N, E>> streamIn() {
            return Streams.stream(edges(true, false));
        }

        public Stream<ImmutableDirectedEdge<N, E>> streamOut() {
            return Streams.stream(edges(false, true));
        }

        abstract public Iterable<ImmutableDirectedEdge<N, E>> edges(boolean in, boolean out);
    }

    public static class MutableNode<N, E> extends Node<N, E> {


        public final Collection<ImmutableDirectedEdge<N, E>> in;
        public final Collection<ImmutableDirectedEdge<N, E>> out;

        public int edgeCount() {
            return ins() + outs();
        }

        protected static <N,E> MutableNode<N,E> withEdgeSets(N id) {
            return withEdgeSets(id, 0);
        }

        protected static <N,E> MutableNode<N,E> withEdgeSets(N id, int inOutInitialCapacity) {
            return new MutableNode<>(id,
                    new ArrayHashSet<>(inOutInitialCapacity),
                    new ArrayHashSet<>(inOutInitialCapacity)
                    
                    
                    
                    
            );
        }

        protected MutableNode(N id, Collection<ImmutableDirectedEdge<N, E>> in, Collection<ImmutableDirectedEdge<N, E>> out) {
            super(id);
            this.in = in;
            this.out = out;
        }

        @Override
        public Iterable<ImmutableDirectedEdge<N, E>> edges(boolean in, boolean out) {
            if (out && !in) return this.out;
            else if (!out && in) return this.in;
            else {
                boolean ie = this.in.isEmpty();
                boolean oe = this.out.isEmpty();
                if (ie && oe) return List.of();
                if (ie) return this.out;
                if (oe) return this.in;
                return Iterables.concat(this.out, this.in);
            }
        }

        public final int ins() {
            return ins(true);
        }

        public int ins(boolean countSelfLoops) {
            if (countSelfLoops) {
                return in.size(); 
            } else {
                return (int) streamIn().filter(e -> e.from != this).count();
            }
        }

        public int outs() {
            
            return out.size();
        }


        protected boolean addIn(ImmutableDirectedEdge<N, E> e) {
            return in.add(e);
        }

        protected boolean addOut(ImmutableDirectedEdge<N, E> e) {
            return out.add(e);
        }

        protected boolean removeIn(ImmutableDirectedEdge<N, E> e) {
            
            return in.remove(e);
        }

        protected boolean removeOut(ImmutableDirectedEdge<N, E> e) {
            
            return out.remove(e);
        }

        @Override public Stream<ImmutableDirectedEdge<N, E>> streamIn() {
            return (in.stream());
        }

        @Override public Stream<ImmutableDirectedEdge<N, E>> streamOut() {
            return (out.stream());
        }



    }

}
