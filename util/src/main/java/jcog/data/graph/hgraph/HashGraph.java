/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
package jcog.data.graph.hgraph;

import com.google.common.collect.Iterables;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Thomas Wuerthinger
 */
public class HashGraph<N, E> {

    protected Map<N, Node<N, E>> nodes;

    public HashGraph() {
        this(new LinkedHashMap<>(), new LinkedHashMap());
    }

    public HashGraph(Map<N, Node<N, E>> nodes, Map<E, Edge<N, E>> edges) {
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

    public void traverseBFS(Node<N, E> startingNode, Search tv, boolean longestPath) {

//        if (longestPath) {
//            markReachable(startingNode);
//        }

//        tv.start();
//        try {
//
//            Queue<Node<N, E>> queue = new LinkedList<>();
//            queue.add(startingNode);
//            startingNode.setVisited(true);
//            int layer = 0;
//            Node<N, E> lastOfLayer = startingNode;
//            final Node[] lastAdded = {null};
//
//            while (!queue.isEmpty()) {
//
//                Node<N, E> current = queue.poll();
//                tv.visitNode(current, layer);
//                current.setActive(false);
//
//
//                current.out().forEach(e -> {
//                    if (!e.to.isVisited()) {
//
//                        final boolean[] allow = {true};
//                        if (longestPath) {
//                            e.to.in().allMatch(pred -> {
//                                Node<N, E> p = pred.to;
//                                if ((!p.isVisited() || p.isActive()) && p.isReachable()) {
//                                    allow[0] = false;
//                                    return false;
//                                }
//                                return true;
//                            });
//                        }
//
//                        if (allow[0]) {
//                            queue.offer(e.to);
//                            lastAdded[0] = e.to;
//                            e.to.setVisited(true);
//                            e.to.setActive(true);
//                        }
//                    }
//                });
//
//                if (current == lastOfLayer && !queue.isEmpty()) {
//                    lastOfLayer = lastAdded[0];
//                    layer++;
//                }
//            }
//        } finally {
//            tv.stop();
//        }
    }



    public boolean dfs(N startingNode, Search<N, E> tv) {
        return dfs(List.of(startingNode), tv);
    }

    public boolean dfs(Iterable<N> startingNodes, Search<N, E> tv) {
        return dfsNodes(Iterables.transform(startingNodes, this::add), tv);
    }

    public boolean dfsNodes(Iterable<Node<N, E>> startingNodes, Search<N, E> search) {

        search.start();
        try {

            boolean result = false;

            for (Node n : startingNodes) {
                if (!search.visit(n))
                    return false;
            }

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
        return nodes().stream().flatMap(Node::out);
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
