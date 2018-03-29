package spacegraph.space3d.test;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.data.graph.MapNodeGraph;
import spacegraph.space3d.widget.SimpleGraph;

public class SimpleGraphTest {
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
        h.addEdge(("x"), ("xy"), ("y"));
        h.addEdge(("x"), ("xz"), ("z"));
        h.addEdge(("y"), ("yz"), ("z"));
        h.addEdge(("w"), ("wy"), ("y"));

        SimpleGraph sg = new SimpleGraph();
        //sg.commit(g);
        sg.commit(h);
        sg.show(800, 600, false);

    }

}
