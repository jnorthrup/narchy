package jcog.data.graph;

import com.google.common.collect.Iterables;
import jcog.data.graph.search.Search;
import jcog.data.set.ArrayHashSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class NodeGraph<N, E> {

    abstract public Node<N, E> node(Object key);


    abstract public void forEachNode(Consumer<Node<N, E>> n);

    protected abstract Node<N, E> newNode(N data);


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

    private boolean dfs(Iterable<N> startingNodes, Search<N, E> search) {
        return search.dfs(Iterables.transform(startingNodes, this::node));
    }

    public boolean bfs(Iterable<N> startingNodes, Search<N, E> search, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>>> q) {
        return search.bfs(Iterables.transform(startingNodes, this::node), q);
    }


    public void print() {
        print(System.out);
    }

    private void print(PrintStream out) {
        forEachNode((node) -> node.print(out));
    }


    public abstract static class AbstractNode<N, E> implements Node<N, E> {
        private final static AtomicInteger serials = new AtomicInteger(1);
        public final N id;
        public final int serial;
        final int hash;


        protected AbstractNode(N id) {
            this.serial = serials.getAndIncrement();
            this.id = id;
            this.hash = id.hashCode();
        }

        @Override
        public final N id() {
            return id;
        }

        //        public Stream<N> successors() {
//            return streamOut().map(e -> e.to().id);
//        }
//
//        public Stream<N> predecessors() {
//            return streamIn().map(e -> e.from().id);
//        }

        @Override
        public final boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return id.toString();
        }


    }

    public static class MutableNode<N, E> extends AbstractNode<N, E> {


        private Collection<FromTo<Node<N, E>, E>> in;
        private Collection<FromTo<Node<N, E>, E>> out;

        public MutableNode(N id) {
            this(id, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        }

        public MutableNode(N id, Collection<FromTo<Node<N, E>, E>> in, Collection<FromTo<Node<N, E>, E>> out) {
            super(id);
            this.in = in;
            this.out = out;
        }

        @Override public int edgeCount(boolean in, boolean out) {
            return (in ? ins() : 0) + (out ? outs() : 0);
        }

        @Override
        public Iterable<FromTo<Node<N, E>, E>> edges(boolean in, boolean out) {
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

        final int ins() {
            return ins(true);
        }

        int ins(boolean countSelfLoops) {
            if (countSelfLoops) {
                return in.size();
            } else {
                return (int) streamIn().filter(e -> e.from() != this).count();
            }
        }

        int outs() {

            return out.size();
        }


        boolean addIn(FromTo<Node<N, E>, E> e) {
            if (in == Collections.EMPTY_LIST)
                in = newEdgeCollection();
            return in.add(e);
        }

        protected Collection<FromTo<Node<N, E>, E>> newEdgeCollection() {
            return new ArrayHashSet<>(2);
        }

        boolean addOut(FromTo<Node<N, E>, E> e) {
            if (out == Collections.EMPTY_LIST)
                out = newEdgeCollection();
            return out.add(e);
        }

        boolean removeIn(FromTo<Node<N, E>, E> e) {
            return in.remove(e);
        }

        boolean removeOut(FromTo<Node<N, E>, E> e) {
            return out.remove(e);
        }

        @Override
        public Stream<FromTo<Node<N, E>, E>> streamIn() {
            return (in.stream());
        }

        @Override
        public Stream<FromTo<Node<N, E>, E>> streamOut() {
            return (out.stream());
        }


    }

}
