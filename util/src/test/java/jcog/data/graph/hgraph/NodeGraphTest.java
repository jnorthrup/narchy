package jcog.data.graph.hgraph;

import jcog.list.FasterList;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeGraphTest {

    @Test
    public void testDFS() {
        NodeGraph n = g1();
        List<String> trace = new FasterList();
        n.dfs("a", new Search() {

            @Override
            protected boolean next(BooleanObjectPair move, Node next) {
                trace.add(path.toString());
                return true;
            }
        });
        assertEquals(4, trace.size());
        assertEquals("[[true:a => ab => b], [true:a => ab => b, true:b => bc => c], [true:a => ab => b, true:b => bc => c, true:c => cd => d], [true:a => ae => e]]", trace.toString());
    }

    @Test
    public void testBFS() {
        NodeGraph n = g1();
        List<String> trace = new FasterList();
        n.bfs("a", new Search() {

            @Override
            protected boolean next(BooleanObjectPair move, Node next) {
                trace.add(path.toString());
                return true;
            }
        });
        System.out.println(trace);
        assertEquals(4, trace.size());
        assertEquals("[[true:a => ab => b], [true:a => ae => e], [true:a => ab => b, true:b => bc => c], [true:a => ab => b, true:b => bc => c, true:c => cd => d]]", trace.toString());
    }


    static NodeGraph g1() {
        NodeGraph n = new NodeGraph();
        n.add("a");
        n.add("b");
        n.add("c");
        n.add("d");
        n.add("e");
        edge(n, "a", "b");
        edge(n, "b", "c");
        edge(n, "c", "d");
        edge(n, "a", "e");
        return n;
    }

    static void edge(NodeGraph n, String a, String b) {
        n.edgeAdd(a, a+b, b);
    }
}