package nars;

import jcog.data.iterator.ArrayIterator;
import nars.agent.NAgent;
import nars.attention.AttNode;
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
        public final AttNode node;

        NodeUI(AttNode node) {
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

    public static Surface attentionGraph(NAR n, NAgent... a) {
        Graph2D<AttNode> aaa = new Graph2D<AttNode>()
                .render((Graph2D.Graph2DRenderer<AttNode>) (node, graph) -> {
                    node.id.children.forEach(c -> {
                        Graph2D.EdgeVis<AttNode> e = graph.edge(node, c);
                        if (e!=null) {
                            e.weight(1f);
                            e.color(0.5f, 0.5f, 0.5f);
                        }
                    });
                    float s = node.id.pri.pri();
                    float d = 0.5f * node.id.factor.floatValue();
//                            float r = Math.min(1, s/d);
                    node.color(Math.min(d, 1), Math.min(s, 1), 0);
                    if (!(node.the() instanceof NodeUI)) {
                        node.set(new NodeUI(node.id));
                    }
                })
                .update(new ForceDirected2D<>() {
                    @Override
                    protected void size(MutableRectFloat<AttNode> m, float a) {
                        float q =
                                    m.node.id.pri.pri();

                        float s = (float)(Math.sqrt((Math.max(0, q))));
                        s = Math.max(Math.min(s, 2) * a, 32);
                        m.size(s, s);
                    }
                });
        return DurSurface.get(aaa.widget(), n, () -> {

            aaa.set(ArrayIterator.stream(a).flatMap(aa -> aa.attn.childrenStreamRecurse())::iterator);

        } );
    }
}
