package jcog.data.graph.search;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.NodeGraph;
import jcog.data.graph.ObjectGraph;
import jcog.list.FasterList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeGraphTest {

    @Test
    void testDFS() {
        NodeGraph n = g1();
        List<String> trace = new FasterList();
        n.dfs("a", new Search() {

            @Override
            protected boolean next(BooleanObjectPair move, NodeGraph.Node next) {
                trace.add(path.toString());
                return true;
            }
        });
        assertEquals(4, trace.size());
        assertEquals("[[true:a => ab => b], [true:a => ab => b, true:b => bc => c], [true:a => ab => b, true:b => bc => c, true:c => cd => d], [true:a => ae => e]]", trace.toString());
    }

    @Test
    void testBFS() {
        NodeGraph n = g1();
        List<String> trace = new FasterList();
        n.bfs("a", new Search() {
            @Override protected boolean next(BooleanObjectPair move, NodeGraph.Node next) {
                trace.add(path.toString());
                return true;
            }
        });
        assertEquals(4, trace.size());
        assertEquals("[[true:a => ab => b], [true:a => ae => e], [true:a => ab => b, true:b => bc => c], [true:a => ab => b, true:b => bc => c, true:c => cd => d]]", trace.toString());
    }


    private static NodeGraph g1() {
        MapNodeGraph n = new MapNodeGraph();
        n.addNode("a");
        n.addNode("b");
        n.addNode("c");
        n.addNode("d");
        n.addNode("e");
        edge(n, "a", "b");
        edge(n, "b", "c");
        edge(n, "c", "d");
        edge(n, "a", "e");
        return n;
    }

    private static void edge(MapNodeGraph n, String a, String b) {
        n.addEdge(a, a+b, b);
    }

    @Test
    void testObjectGraph() {
        MapNodeGraph<Object, Object> h = new MapNodeGraph<>();
        h.addEdge(h.addNode("y"), "yx", h.addNode("x"));

        ObjectGraph o = new ObjectGraph(3, h) {

            @Override
            protected boolean access(Object root, FasterList<Pair<Class, ObjectGraph.Accessor>> path, Object target) {
                System.out.println(root + " -> " + target + "\n\t" + path);
                return true;
            }

            @Override
            public boolean includeValue(Object value) {
                return true;
            }

            @Override
            public boolean includeClass(Class<?> c) {
                return !c.isPrimitive();
            }

            @Override
            public boolean includeField(Field f) {
                return true;
            }
        };
        o.print();
    }
}