package spacegraph.space3d.test;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.data.graph.MapNodeGraph;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.widget.SimpleGraph;

public class SimpleGraphTest {
    public static void main(String[] args) {

        MutableGraph g = GraphBuilder.directed().build();
        g.putEdge(("x"), ("y"));
        g.putEdge(("y"), ("z"));
        g.putEdge(("y"), ("w"));

        MapNodeGraph h = new MapNodeGraph();
        h.addNode(("x"));
        h.addNode(("y"));
        h.addNode(("z"));
        h.addNode(("a"));
        h.addEdge(("x"), ("xy"), ("y"));
        h.addEdge(("x"), ("xz"), ("z"));
        h.addEdge(("y"), ("yz"), ("z"));
        h.addEdge(("a"), ("ay"), ("y"));

        SimpleGraph cs = new SimpleGraph() {
            @Override
            public void start(SpaceGraphPhys3D space) {
                super.start(space);
                commit(h);
            }
        };

        cs.show(800, 600, false);

    }

}
