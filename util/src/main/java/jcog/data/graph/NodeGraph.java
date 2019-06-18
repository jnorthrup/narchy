package jcog.data.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.set.ArrayHashSet;
import jcog.data.set.ArrayUnenforcedSortedSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class NodeGraph<N, E> /* TODO merge with guava Graph: implements ValueGraph<N,E> */ {

    abstract public Node<N, E> node(Object key);

    abstract public Iterable<Node<N,E>> nodes();

    abstract int nodeCount();

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
        return bfs(List.of(startingNode),tv);
    }

    /** iterate all nodes, in topologically sorted order */
    public void forEachBF(N root, Consumer<? super N> each) {
        each.accept(root);
        bfs(List.of(root), new Search<>() {
            @Override
            protected boolean go(List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path, Node<N, E> next) {
                each.accept(next.id());
                return true;
            }
        });
    }

    private boolean dfs(Iterable<N> startingNodes, Search<N, E> search) {
        return search.dfs(startingNodes, this);
    }

//    public boolean bfs(Iterable<N> startingNodes, Search<N, E> search) {
//        if (startingNodes instanceof Collection) {
//            return bfs((Collection)startingNodes, search); //potential optimization
//        } else  {
//            return bfs(new ArrayDeque(/*TODO size */), Iterables.transform(startingNodes, this::node), search);
//        }
//    }



    public boolean bfs(Iterable<N> roots, Search<N, E> search) {
        int c = nodeCount();
        switch (c) {
            case 0:
            case 1:
                return true; //nothing
            case 2:
                return dfs(roots, search); //optimization (no queue needed)
        }

        return bfs(roots, new ArrayDeque<>(
                2 * (int)Math.ceil(Math.log(c)) /* estimate */), search);
    }

    public boolean bfs(Iterable startingNodes, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>>> q, Search<N, E> search) {
        return search.bfs(startingNodes, q, this);
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

        MutableNode(N id, Collection<FromTo<Node<N, E>, E>> in, Collection<FromTo<Node<N, E>, E>> out) {
            super(id);
            this.in = in;
            this.out = out;
        }

        @Override public int edgeCount(boolean in, boolean out) {
            return (in ? ins() : 0) + (out ? outs() : 0);
        }

        @Override
        public Iterable<FromTo<Node<N, E>, E>> edges(boolean in, boolean out) {
            boolean ie = !in || this.in.isEmpty();
            boolean oe = !out || this.out.isEmpty();
            if (ie && oe) return List.of();
            else if (ie) return this.out;
            else if (oe) return this.in;
            else return Iterables.concat(this.out, this.in);
        }
        @Override public Iterator<FromTo<Node<N,E>,E>> edgeIterator(boolean in, boolean out) {
            boolean ie = !in || this.in.isEmpty();
            boolean oe = !out || this.out.isEmpty();
            if (ie && oe) return Collections.emptyIterator();
            else if (ie) return this.out.iterator();
            else if (oe) return this.in.iterator();
            else return Iterators.concat(this.out.iterator(), this.in.iterator());
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


        Collection<FromTo<Node<N, E>, E>> newEdgeCollection() {
            return new ArrayHashSet<>(2);
        }

        Collection<FromTo<Node<N, E>, E>> newEdgeCollection(FromTo... ff) {
            Collection<FromTo<Node<N, E>, E>> c = newEdgeCollection();
            for (FromTo f: ff)
                c.add(f);
            return c;
        }

        public boolean addIn(FromTo<Node<N, E>, E> e) {
            return addSet(e, true);

        }

        public boolean addOut(FromTo<Node<N, E>, E> e) {
            return addSet(e, false);
        }

        private boolean addSet(FromTo<Node<N, E>, E> e, boolean inOrOut) {
            boolean result;
            Collection<FromTo<Node<N, E>, E>> s = inOrOut ? in : out;
            if (s == Collections.EMPTY_LIST) {
                //out = newEdgeCollection();
                s = ArrayUnenforcedSortedSet.the(e);
                result = true;
            } else {
                if (s instanceof ArrayUnenforcedSortedSet) {
                    FromTo<Node<N, E>, E> x = ((ArrayUnenforcedSortedSet<FromTo<Node<N, E>, E>>)s).get(0);
                    assert(x!=null);
                    if (!e.equals(x)) {
                        s = newEdgeCollection(x,e);
                        result = true;
                    } else {
                        result = false;
                    }
                } else {
                    result = s.add(e);
                }
            }
            if (result) {
                if (inOrOut) in = s; else out = s;
            }
            return result;
        }

        public boolean removeIn(FromTo<Node<N, E>, E> e) {
            return removeSet(e, true);
        }

        public boolean removeOut(FromTo<Node<N, E>, E> e) {
            return removeSet(e, false);
        }

        public void removeIn(Node<N, E> src) {
            edges(true, false, e->e.to()==src, null).forEach(e -> removeSet(e, true));
        }

        public void removeOut(Node<N, E> target) {
            edges(false, true, e->e.to()==target, null).forEach(e -> removeSet(e, false));
        }

        private boolean removeSet(FromTo<Node<N,E>,E> e, boolean inOrOut) {
            Collection<FromTo<Node<N, E>, E>> s = inOrOut ? in : out;
            if (s == Collections.EMPTY_LIST)
                return false;

            boolean changed;
            if (s instanceof ArrayUnenforcedSortedSet) {
                if (((ArrayUnenforcedSortedSet)s).get(0).equals(e)) {
                    s = Collections.EMPTY_LIST;
                    changed = true;
                } else {
                    changed = false;
                }
            } else {
                changed = s.remove(e);
                if (changed) {
                    switch (s.size()) {
                        case 0:
                            throw new UnsupportedOperationException();
                        case 1:
                            s = ArrayUnenforcedSortedSet.the(((ArrayHashSet<FromTo<Node<N,E>,E>>)s).first());
                            break;
                    }
                }
                //TODO downgrade
            }

            if (changed) {
                if (inOrOut) in = s; else out = s;
            }
            return changed;
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
