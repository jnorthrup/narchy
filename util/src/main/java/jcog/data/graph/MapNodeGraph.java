package jcog.data.graph;

import com.google.common.collect.Iterables;
import com.google.common.graph.SuccessorsFunction;
import jcog.data.graph.edge.ImmutableDirectedEdge;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.list.FasterList;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


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

    protected final Map<N, Node<N, E>> nodes;

    public MapNodeGraph() {
        this(new LinkedHashMap<>());
    }

    public MapNodeGraph(SuccessorsFunction<N> s, Iterable<N> start) {
        this();
        ArrayDeque<N> queue = new ArrayDeque();
        for (N n : start) {
            queue.add(n);
        }

        N x;
        Collection<N> traversed = new HashSet();
        while ((x = queue.poll()) != null) {

            MutableNode<N, E> xx = addNode(x);
            Iterable<? extends N> xs = s.successors(x);
            //System.out.println(x + " " + xs);
            for (N y : xs) {
                if (traversed.add(y))
                    queue.add(y);

                addEdgeByNode(xx, (E) "->" /* HACK */, addNode(y));
            }

        }
    }

    public MapNodeGraph(Map<N, Node<N, E>> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void clear() {
        nodes.clear();
    }

    public boolean removeNode(N key) {
        Node<N, E> removed = nodes.remove(key);
        if (removed != null) {
            removed.edgeIterator(true, false).forEachRemaining(this::edgeRemoveOut);
            removed.edgeIterator(false, true).forEachRemaining(this::edgeRemoveIn);
            onRemoved(removed);
            return true;
        }
        return false;
    }
    public boolean removeNode(N key, Consumer<FromTo<Node<N, E>, E>> inEdges, Consumer<FromTo<Node<N, E>, E>> outEdges) {
        Node<N, E> removed = nodes.remove(key);
        if (removed != null) {
            removed.edgeIterator(true, false).forEachRemaining(inEdges.andThen(this::edgeRemoveOut));
            removed.edgeIterator(false, true).forEachRemaining(outEdges.andThen(this::edgeRemoveIn));
            onRemoved(removed);
            return true;
        }
        return false;
    }

    /** ensures each root node is added before searching */
    public boolean bfsAdd(Iterable<N> roots, Search<N, E> search) {
        return bfs(StreamSupport.stream(roots.spliterator(), false).map(r -> addNode(r).id).collect(Collectors.toList()), search);
    }
    /** ensures each root node is added before searching */
    public boolean bfsAdd(N root, Search<N, E> search) {
        return bfs(addNode(root).id, search);
    }

    public final MutableNode<N, E> addNode(N key) {
        return addNode(key, true);
    }

    private MutableNode<N, E> addNode(N key, boolean returnNodeIfExisted) {
        boolean[] created = {false};
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
        return addNode(key, false) != null;
    }

    @Override
    protected Node<N, E> newNode(N data) {
        return new MutableNode<>(data);
    }

    protected void onAdd(Node<N, E> r) {

    }

    protected void onRemoved(Node<N, E> r) {

    }

    /** creates the nodes if they do not exist yet */
    public boolean addEdge(N from, E data, N to) {
        MutableNode<N,E> f = addNode(from);
        MutableNode<N,E> t = addNode(to);
        return addEdgeByNode(f, data, t);
    }

    public boolean addEdgeIfNodesExist(N from, E data, N to) {
        Node<N, E> f = node(from);
        Node<N, E> t = node(to);
        return addEdgeByNode((MutableNode<N,E>) f, data, (MutableNode<N,E>) t);
    }

    public final boolean addEdgeByNode(MutableNode<N, E> from, E data, MutableNode<N, E> to) {
        return addEdge(from, to, new ImmutableDirectedEdge<>(from, data, to));
    }

    public final boolean addEdge(MutableNode<N, E> from, E data, N to) {
        return addEdgeByNode(from, data, addNode(to));
    }
    public final boolean addEdge(N from, E data, MutableNode<N,E> to) {
        return addEdgeByNode(addNode(from), data, to);
    }

    public boolean addEdge(FromTo<Node<N, E>, E> ee) {
        return addEdge((MutableNode<N,E>)(ee.from()), (MutableNode<N,E>)(ee.to()), ee);
    }

    static <N,E> boolean addEdge(MutableNode<N, E> from, MutableNode<N, E> to, FromTo<Node<N, E>, E> ee) {
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

    public Collection<Node<N, E>> nodes() {
        return nodes.values();
    }

    public Iterable<N> nodeIDs() {
        return Iterables.transform(nodes(), Node::id);
    }

    @Override int nodeCount() {
        return nodes.size();
    }

    @Override
    public void forEachNode(Consumer<Node<N, E>> n) {
        for (Node<N, E> neNode : nodes.values()) {
            n.accept(neNode);
        }
    }

    public boolean edgeRemove(FromTo<Node<N, E>, E> e) {
        if (edgeRemoveOut(e)) {
            boolean removed = edgeRemoveIn(e);
            assert (removed);
            return true;
        }
        return false;
    }

    private boolean edgeRemoveIn(FromTo<Node<N, E>, E> e) {
        return ((MutableNode) e.to()).removeIn(e);
    }

    private boolean edgeRemoveOut(FromTo<Node<N, E>, E> e) {
        return ((MutableNode) e.from()).removeOut(e);
    }


    public Stream<FromTo<Node<N, E>, E>> edges() {
        return nodes().stream().flatMap(Node::streamOut);
    }

    @Override
    public String toString() {

        StringBuilder s = new StringBuilder();
        s.append("Nodes: ");
        for (Node<N, E> n : nodes()) {
            s.append(n).append('\n');
        }

        s.append("Edges: ");

        edges().forEach(e -> s.append(e).append('\n'));

        return s.toString();
    }

    public boolean containsNode(Object x) {
        return nodes.containsKey(x);
    }


    /**
     * relinks all edges in 'from' to 'to' before removing 'from'
     */
    public boolean mergeNodes(N from, N to) {
        MutableNode<N, E> fromNode = (MutableNode<N, E>) nodes.get(from);
        MutableNode<N, E> toNode = (MutableNode<N, E>) nodes.get(to);
        if (fromNode != null && toNode != null) {
            if (fromNode != toNode) {

                int e = fromNode.ins() + fromNode.outs();
                if (e > 0) {
                    List<FromTo> removed = new FasterList(e);
                    fromNode.edgeIterator(true, false).forEachRemaining(inEdge -> {
                        removed.add(inEdge);
                        MutableNode x = (MutableNode) (inEdge.from());
                        if (x != fromNode)
                            addEdgeByNode(x, inEdge.id(), toNode);
                    });
                    fromNode.edgeIterator(false, true).forEachRemaining(outEdge -> {
                        removed.add(outEdge);
                        MutableNode x = (MutableNode) (outEdge.to());
                        if (x != fromNode)
                            addEdgeByNode(toNode, outEdge.id(), x);
                    });
                    for (FromTo fromTo : removed) {
                        edgeRemove(fromTo);
                    }
                    //assert (fromNode.ins() == 0 && fromNode.outs() == 0);
                }
            }
            removeNode(from);
            return true;
        }
        return false;
    }

}
