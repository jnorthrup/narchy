package nars;

import jcog.Util;
import nars.agent.NAgent;
import nars.attention.AttNode;
import nars.gui.DurSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.layout.ForceDirected2D;
import spacegraph.util.MutableFloatRect;

public class AttentionUI {

    public static Surface attentionGraph(NAR n, NAgent a) {
        AttNode aa = a.attn;
        Graph2D<AttNode> aaa = new Graph2D<AttNode>()
                .render(new Graph2D.Graph2DRenderer<AttNode>() {
                    @Override
                    public void node(Graph2D.NodeVis<AttNode> node, Graph2D.GraphEditing<AttNode> graph) {
                        node.id.children.forEach(c ->
                                {
                                    Graph2D.EdgeVis<AttNode> e = graph.edge(node, c);
                                    e.weight(1f);
                                    e.color(0.5f, 0.5f, 0.5f);
                                }
                        );
                        float s = node.id.supply.pri();
                        float d = node.id.demand.floatValue();
//                            float r = Math.min(1, s/d);
                        node.color(Math.min(d, 1), Math.min(s, 1), 0);
                    }
                })
                .update(new ForceDirected2D<>() {
                    @Override
                    protected void size(MutableFloatRect<AttNode> m, float a) {
                        float q = Math.max(
                                    m.node.id.demand.floatValue(),
                                    m.node.id.supply.pri()
                                );

                        float s = (float)(Math.sqrt((Math.max(0, q))));
                        s = Util.clamp(s, 0.1f, 2) * a;
                        m.size(s, s);
                    }
                });
        return DurSurface.get(aaa.widget(), n, () -> aaa.set(aa.childrenStreamRecurse()));
    }
}
