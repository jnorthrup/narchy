package jcog.data.graph;

import com.google.common.graph.SuccessorsFunction;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;


/**
 * graph rooted in a set of vertices (nodes),
 * providing access to edges only indirectly through them
 * (ie. the edges are not stored in their own index but only secondarily as part of the vertices)
 * <p>
 * TODO abstract into subclasses:
 * HashNodeGraph backed by HashMap node and edge containers
 * BagNodeGraph backed by Bag's/Bagregate's
 * <p>
 * then replace TermWidget/EDraw stuff with BagNodeGraph
 */
public class MapNodeGraph<N, E> extends NodeGraph<N, E> {

    protected final Map<N, Node<N,E>> nodes;

    public MapNodeGraph() {
        this(new LinkedHashMap<>());
    }

    public MapNodeGraph(SuccessorsFunction<N> s, Iterable<N> start) {
        this();
        Set<N> traversed = new HashSet();
        ArrayDeque<N> queue = new ArrayDeque();
        start.forEach(queue::add);

        N x;
        while ((x = queue.poll()) != null) {
            {

                NodeGraph.MutableNode<N, E> xx = addNode(x);
                Iterable<? extends N> xs = s.successors(x);
                System.out.println(x + " " + xs);
                xs.forEach(y -> {
                    if (traversed.add(y))
                        queue.add(y);

                    addEdge(xx, (E) "->" /* HACK */, addNode(y));
                });
            }

        }
    }

    public MapNodeGraph(Map<N, Node<N,E>> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void clear() {
        nodes.clear();
    }

    public boolean removeNode(N key) {
        Node<N,E> removed = nodes.remove(key);
        if (removed != null) {
            onRemoved(removed);
            return true;
        }
        return false;
    }

    public MutableNode<N, E> addNode(N key) {


        final boolean[] created = {false};
        Node<N, E> r = nodes.computeIfAbsent(key, (x) -> {
            created[0] = true;
            return newNode(x);
        });
        if (created[0]) {
            onAdd(r);
        }
        return (MutableNode<N, E>) r;
    }

    @Override
    protected Node<N,E> newNode(N data) {
        return MutableNode.withEdgeSets(data);
    }

    protected void onAdd(Node<N, E> r) {

    }

    protected void onRemoved(Node<N,E> r) {

    }

    public boolean addEdge(N from, E data, N to) {
        Node<N,E> f = node(from);
        if (f == null)
            throw new NullPointerException();
        if (!(f instanceof Node))
            throw new UnsupportedOperationException();
        Node<N,E> t = node(to);
        if (t == null)
            throw new NullPointerException();
        if (!(t instanceof Node))
            throw new UnsupportedOperationException();
        return addEdge((MutableNode) f, data, (MutableNode) t);
    }

    public boolean addEdge(MutableNode<N, E> from, E data, MutableNode<N, E> to) {
        FromTo<Node<N,E>,E> ee = new ImmutableDirectedEdge<>(from, to, data);
        if (from.addOut(ee)) {
            boolean a = to.addIn(ee);
            assert (a);
            return true;
        }
        return false;
    }

    @Override
    public Node<N, E> node(Object key) {
        return nodes.get(key);
    }

    public Collection<Node<N,E>> nodes() {
        return nodes.values();
    }

    @Override
    public void forEachNode(Consumer<Node<N,E>> n) {
        nodes.values().forEach(n);
    }

    public boolean edgeRemove(ImmutableDirectedEdge<N, E> e) {
        if (((MutableNode) e.from).removeOut(e)) {
            boolean removed = ((MutableNode) e.to).removeIn(e);
            assert (removed);
            return true;
        }
        return false;
    }


    public Stream<FromTo<Node<N,E>,E>> edges() {
        return nodes().stream().flatMap(Node::streamOut);
    }

    @Override
    public String toString() {

        StringBuilder s = new StringBuilder();
        s.append("Nodes: ");
        for (Node<N,E> n : nodes()) {
            s.append(n).append('\n');
        }

        s.append("Edges: ");

        edges().forEach(e -> {
            s.append(e).append('\n');
        });

        return s.toString();
    }

    public boolean containsNode(Object x) {
        return nodes.containsKey(x);
    }


}
