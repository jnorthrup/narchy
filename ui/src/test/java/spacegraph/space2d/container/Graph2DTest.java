package spacegraph.space2d.container;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.data.graph.MapNodeGraph;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.Graph2D;

public class Graph2DTest {


    static class Graph2DTest1 {
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
            for (int i = 0; i < 100; i++) {
                h.addNode("_" + i);
            }
            h.addEdge(("x"), ("xy"), ("y"));
            h.addEdge(("x"), ("xz"), ("z"));
            h.addEdge(("y"), ("yz"), ("z"));
            h.addEdge(("w"), ("wy"), ("y"));

            Graph2D<Object> sg = new Graph2D<>();
            sg.layout(new ForceDirected2D());
            //sg.update(g.nodes(), g::incidentEdges)
            //sg.commit(g);
//            sg.commit(h);


            SpaceGraph.window(sg, 800, 800);
        }

    }


}
