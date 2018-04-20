package nars.gui.graph.run;

import jcog.pri.PLink;
import jcog.tree.rtree.rect.RectFloat2D;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.exe.AbstractExec;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Graph2D;
import spacegraph.space2d.container.ForceDirected2D;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.windo.Widget;

public class SimpleConceptGraph2D {
    public static void main(String[] args) throws Narsese.NarseseException {

        NAR n = NARS.tmp();
        n.input("a:b.");
        n.input("b:c.");
        n.input("c:d.");
        n.input("d:e.");
        n.run(10);

        Graph2D<Concept> g = new Graph2D<Concept>()
                .layout(new ForceDirected2D<>() {
                    @Override
                    public void layout(Graph2D<Concept> g, int dtMS) {
                        g.forEachValue(nn->{
                            float pri = ((AbstractExec)n.exe).active.pri(nn.id, 0f);
                            float p = (float) (5f + Math.sqrt(pri) * 10f);
                            nn.pos(RectFloat2D.XYWH(nn.cx(), nn.cy(), p, p));
                        });
                        super.layout(g, dtMS);
                    }
                })
                .layer((gg, node, edges)->{
                    node.id.termlinks().forEach(l -> {
                        Concept tgtConcept = n.concept(l.get());
                        if (tgtConcept!=null) {
                            Graph2D.EdgeVis<Concept> e = edges.apply(tgtConcept);
                            if (e!=null) {
                                float p = l.priElseZero();
                                e.color((0.9f * p) + 0.1f, 0, 0);
                            }
                        }
                    });
                })
                .layer((gg, node, edges)->{
                    node.id.tasklinks().forEach(l -> {
                        Concept tgtConcept = n.concept(l.term());
                        if (tgtConcept!=null) {
                            Graph2D.EdgeVis<Concept> e = edges.apply(tgtConcept);
                            if (e!=null) {
                                float p = l.priElseZero();
                                e.color(0, (0.9f * p) + 0.1f, 0);
                            }
                        }
                    });
                })
                ;

        SpaceGraph.window(
            new Widget(
                    new Splitting(/*new Clipped*/(g), g.configWidget(), 0.1f)
            )
            ,800, 800
        );
        n.onCycle(()->{
            g.update(
                ()->n.conceptsActive().map(PLink::get).iterator(),
            true);
        });
        n.startFPS(1f);
    }
}
