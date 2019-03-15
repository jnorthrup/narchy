package nars;

import com.google.common.collect.Iterables;
import jcog.data.graph.Node;
import nars.attention.PriNode;
import nars.gui.DurSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.layout.ForceDirected2D;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.MutableRectFloat;

public class AttentionUI {

    static class NodeUI extends Gridding {
        public final PriNode node;

        NodeUI(PriNode node) {
            this.node = node;
            add(new VectorLabel(node.toString()));
            add(new ObjectSurface(node));
        }
    }

//    public static GraphEdit serviceGraph(NAR n, NAgent a) {
//        GraphEdit<Surface> g = GraphEdit.window(800, 500);
//        g.add(new ExpandingChip(a.toString(), (x)->{
//            new ObjectSurface(a).forEach
//        }));
//        return g;
//    }

    public static Surface attentionGraph(NAR n) {
        Graph2D<PriNode> aaa = new Graph2D<PriNode>()
                .render((Graph2D.Graph2DRenderer<PriNode>) (node, graph) -> {
                    n.attn.graph.node(node.id).nodes(false,true).forEach(c -> {
                        Graph2D.EdgeVis<PriNode> e = graph.edge(node, c.id());
                        if (e!=null) {
                            e.weight(1f);
                            e.color(0.5f, 0.5f, 0.5f);
                        }
                    });
                    float s = node.id.pri();
                    float d = 0.5f * node.id.pri();
//                            float r = Math.min(1, s/d);
                    node.color(Math.min(d, 1), Math.min(s, 1), 0);
                    if (!(node.the() instanceof NodeUI)) {
                        node.set(new NodeUI(node.id));
                    }
                })
                .update(new ForceDirected2D<>() {
                    @Override
                    protected void size(MutableRectFloat<PriNode> m, float a) {
                        float q =
                                    m.node.id.pri();

                        float s = (float)(Math.sqrt((Math.max(0, q))));
                        s = Math.max(Math.min(s, 2) * a, 32);
                        m.size(s, s);
                    }
                });
        return DurSurface.get(aaa.widget(), n, () -> {
            aaa.set(Iterables.transform(n.attn.graph.nodes(), Node::id));
        } );
    }
}
