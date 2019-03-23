package spacegraph.space2d.container;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.reflect.ExtendedCastGraph;
import org.ujmp.core.Matrix;
import org.ujmp.core.util.matrices.SystemEnvironmentMatrix;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.layout.ForceDirected2D;

import java.util.function.Function;

public enum Graph2DTest {;


    static final MapNodeGraph<Object,Object> h = new MapNodeGraph();
    static {
        h.addNode(("x"));
        h.addNode(("y"));
        h.addNode(("z"));
        h.addNode(("w"));

        h.addEdgeIfNodesExist(("x"), ("xy"), ("y"));
        h.addEdgeIfNodesExist(("x"), ("xz"), ("z"));
        h.addEdgeIfNodesExist(("y"), ("yz"), ("z"));
        h.addEdgeIfNodesExist(("w"), ("wy"), ("y"));
    }
    public static class Graph2DTest1 {
        public static void main(String[] args) {


            Graph2D<Node<Object, Object>> sg = newSimpleGraph();


            SpaceGraph.window(sg, 800, 800);
        }

    }


    public static class Ujmp1 {
        public static void main(String[] args) {
            SpaceGraph.window(newUjmpGraph(), 800, 800);
        }

    }
    public static Surface newTypeGraph() {
        return new Graph2D<Node<Class, Function>>()
                .update(new ForceDirected2D<>())
                .render(new Graph2D.NodeGraphRenderer<>())
                .set(new ExtendedCastGraph()).widget();
    }

    public static Graph2D<Node<Object, Object>> newSimpleGraph() {
        return new Graph2D<Node<Object, Object>>()

                .update(new ForceDirected2D<>())

                .render(new Graph2D.NodeGraphRenderer<>())

                .set(h);
    }
    public static Surface newUjmpGraph() {
        MapNodeGraph<Object,Object> h = new MapNodeGraph();

        SystemEnvironmentMatrix env = Matrix.Factory.systemEnvironment();
        h.addNode("env");
        env.forEach((k,v)->{
            h.addNode(v);
            h.addEdgeIfNodesExist("env",k,v);
        });


        return new Graph2D<Node<Object, Object>>()

                .update(new ForceDirected2D())

                .render(new Graph2D.NodeGraphRenderer())

                .set(h).widget();
    }

}
