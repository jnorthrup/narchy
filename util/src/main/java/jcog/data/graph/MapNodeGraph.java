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
        Collection<N> traversed = new HashSet();
        ArrayDeque<N> queue = new ArrayDeque();
        start.forEach(queue::add);

        N x;
        while ((x = queue.poll()) != null) {

            MutableNode<N, E> xx = addNode(x);
            Iterable<? extends N> xs = s.successors(x);
            System.out.println(x + " " + xs);
            xs.forEach(y -> {
                if (traversed.add(y))
                    queue.add(y);

                addEdge(xx, (E) "->" /* HACK */, addNode(y));
            });

        }
    }

    MapNodeGraph(Map<N, Node<N, E>> nodes) {
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


    public final MutableNode<N, E> addNode(N key) {
        return addNode(key, true);
    }
    private MutableNode<N,E> addNode(N key, boolean returnNodeIfExisted) {
        final boolean[] created = {false};
        MutableNode<N, E> r = (MutableNode<N, E>) nodes.computeIfAbsent(key, (x) -> {
            created[0] = true;
            return newNode(x);
        });
        if (created[0]) {
            onAdd(r);
            return r;
        } else {
            return returnNodeIfExisted ? r : null;
        }

    }

    public final boolean addNewNode(N key) {
        return addNode(key, true) != null;
    }

    @Override
    protected Node<N,E> newNode(N data) {
        return new MutableNode(data);
    }

    protected void onAdd(Node<N, E> r) {

    }

    protected void onRemoved(Node<N, E> r) {

    }

    public boolean addEdge(N from, E data, N to) {
        Node<N,E> f = node(from);
        Node<N,E> t = node(to);
        return addEdge((MutableNode) f, data, (MutableNode) t);
    }

    public final boolean addEdge(MutableNode<N, E> from, E data, MutableNode<N, E> to) {
        FromTo<Node<N,E>,E> ee = new ImmutableDirectedEdge<>(from, data, to);
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
