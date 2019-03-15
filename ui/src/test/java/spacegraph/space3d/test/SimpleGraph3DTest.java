package spacegraph.space3d.test;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.data.graph.MapNodeGraph;
import spacegraph.space3d.widget.SimpleGraph3D;

public class SimpleGraph3DTest {
    public static void main(String[] args) {

        MutableGraph g = GraphBuilder.directed().build();
        g.putEdge(("a"), ("b"));
        g.putEdge(("b"), ("c"));
        g.putEdge(("b"), ("d"));

        MapNodeGraph h = new MapNodeGraph();
        h.addNode(("x"));
        h.addNode(("y"));
        h.addNode(("z"));
        h.addNode(("w"));
        h.addEdgeIfNodesExist(("x"), ("xy"), ("y"));
        h.addEdgeIfNodesExist(("x"), ("xz"), ("z"));
        h.addEdgeIfNodesExist(("y"), ("yz"), ("z"));
        h.addEdgeIfNodesExist(("w"), ("wy"), ("y"));

        SimpleGraph3D sg = new SimpleGraph3D();
        
        sg.commit(h);
        sg.show(800, 600, false);

    }

}
