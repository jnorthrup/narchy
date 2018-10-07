package spacegraph.space2d.widget.meta;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.Graph2D;
import spacegraph.space2d.container.layout.ForceDirected2D;

/** debugging/meta-view */
public class Inspector extends Bordering {

    private final Graph2D graphView;

    MapNodeGraph<Surface,Object> graph = new MapNodeGraph();



    public Inspector(Surface s) {
        super();


        int depth = 4;
        include(s, depth);

        graphView = new Graph2D<Node<Surface, Object>>()

                .update(new ForceDirected2D())

                .render(new Graph2D.NodeGraphRenderer() {

                })

                .set(graph.nodes());

        set(graphView.widget());
    }

    public void include(Surface s, int depth) {
        if (!graph.addNewNode(s) || depth <= 0)
            return;

        SurfaceBase p = s.parent;
        if (p instanceof Surface) {
            Surface pp = (Surface) p;
            include(pp, depth-1);
            graph.addEdge(pp, "->", s);
        }

        if (s instanceof Container) {
            ((Container)s).forEach(x -> {
                include(x, depth-1);
                graph.addEdge(s, "->", x);
            });
        }
    }

//    private class SurfaceGrapher implements Graph2D.Graph2DRenderer<Surface> {
//        @Override
//        public void node(Graph2D.NodeVis<Surface> node, Graph2D.GraphEditing<Surface> graph) {
//            node.set(new PushButton(node.id.toString()));
//            graph.edge()
//        }
//    }
}
