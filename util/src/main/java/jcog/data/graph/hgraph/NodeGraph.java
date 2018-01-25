package jcog.data.graph.hgraph;

import com.google.common.collect.Iterables;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Stream;


/**
 * graph rooted in a set of vertices (nodes),
 * providing access to edges only indirectly through them
 * (ie. the edges are not stored in their own index but only secondarily as part of the vertices)
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
            for (Node n : startingNodes)
                if (!search.bfs(n))
                    return false;

            return true;
        } finally {
            search.stop();
        }
    }
    private boolean dfsNodes(Iterable<Node<N, E>> startingNodes, Search<N, E> search) {

        search.start();
        try {

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
}
