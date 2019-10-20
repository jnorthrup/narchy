package spacegraph.space2d.container.graph;

import jcog.data.graph.Node;
import jcog.data.graph.path.FromTo;

/**
 * layer which renders NodeGraph nodes and edges
 */
public class NodeGraphRenderer<N, E> implements Graph2D.Graph2DRenderer<N> {
    @Override
    public void node(NodeVis<N> node, Graph2D.GraphEditing<N> graph) {
        if (node.id instanceof Node) {
            node.color(0.5f, 0.5f, 0.5f);
//                node.move((float) Math.random() * 100, (float) Math.random() * 100);
            node.resize(20f, 10f);

            Node<N, E> nn = (Node<N, E>) node.id;
            for (FromTo<Node<N, E>, E> e : nn.edges(false, true)) {
                EdgeVis<N> ee = graph.edge(node, e.other(nn));
                ee.weight = 0.1f;
                ee.a = 0.75f;
                ee.r = ee.g = ee.b = 0.5f;
            }

        }
    }
}
