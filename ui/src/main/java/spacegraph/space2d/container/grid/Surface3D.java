package spacegraph.space2d.container.grid;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.jogamp.opengl.GL2;
import jcog.data.graph.MapNodeGraph;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space3d.AbstractSpace;
import spacegraph.space3d.SpaceGraph3D;
import spacegraph.space3d.widget.SimpleGraph3D;

import static spacegraph.space2d.container.Bordering.S;

/** adapter for embedding 3d spacegraph in 2d (surface) view */
public class Surface3D extends PaintSurface {

    public Surface3D(AbstractSpace space) {
        SpaceGraph3D<Object> sg = new SpaceGraph3D<>(space);
    }

    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        //TODO

    }

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

        SpaceGraph.window(new Bordering(new Surface3D(sg)).set(S, new PushButton("K")), 800, 800);
    }
}
