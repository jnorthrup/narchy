package nars.gui.graph.run;

import jcog.pri.PLink;
import jcog.tree.rtree.rect.RectFloat2D;
import nars.NAR;
import nars.NARS;
import nars.concept.Concept;
import nars.exe.AbstractExec;
import nars.test.DeductiveMeshTest;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.ForceDirected2D;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.Graph2D;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class SimpleConceptGraph2D {
    public static void main(String[] args) {

        NAR n = NARS.tmp(4);
        n.termVolumeMax.set(10);
//        n.input("a:b.");
//        n.input("b:c.");
//        n.input("c:d.");
//        n.input("d:e.");
//        n.run(10);
        new DeductiveMeshTest(n, 3, 3);

        Graph2D<Concept> g = new Graph2D<Concept>()
                .layout(new ForceDirected2D<>() {
                    @Override
                    public void layout(Graph2D<Concept> g, int dtMS) {
                        g.forEachValue(nn->{
                            float pri = ((AbstractExec)n.exe).active.pri(nn.id, 0f);
                            nn.color(pri, pri/2f, 0f);

                            float p = (float) (20f + Math.sqrt(pri) * 60f);
                            nn.pos(RectFloat2D.XYWH(nn.cx(), nn.cy(), p, p));
                        });
                        super.layout(g, dtMS);
                    }
                })
                .layer(new TermlinkVis(n))
                .layer(new TasklinkVis(n))
                ;

        SpaceGraph.window(
            //new Widget(
                    new Splitting(/*new Clipped*/(g), g.configWidget(), 0.1f)
            //)
            ,800, 800
        );
        n.onCycle(()->{
            g.update(
                ()->n.conceptsActive().map(PLink::get).iterator(),
            true);
        });
        n.startFPS(40f);
    }

    private static class TermlinkVis implements Graph2D.Graph2DLayer<Concept> {
        final NAR n;

        public final AtomicBoolean termlinks = new AtomicBoolean(true);

        private TermlinkVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(Graph2D<Concept> gg, Graph2D.NodeVis<Concept> node, Function<Concept, Graph2D.EdgeVis<Concept>> edges) {
            if (!termlinks.get())
                return;

            node.id.termlinks().forEach(l -> {
                Concept tgtConcept = n.concept(l.get());
                if (tgtConcept != null) {
                    Graph2D.EdgeVis<Concept> e = edges.apply(tgtConcept);
                    if (e != null) {
                        float p = l.priElseZero();
                        e.color((0.9f * p) + 0.1f, 0, 0);
                    }
                }
            });
        }
    }
    private static class TasklinkVis implements Graph2D.Graph2DLayer<Concept> {
        final NAR n;

        public final AtomicBoolean tasklinks = new AtomicBoolean(true);

        private TasklinkVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(Graph2D<Concept> gg, Graph2D.NodeVis<Concept> node, Function<Concept, Graph2D.EdgeVis<Concept>> edges) {
            if (!tasklinks.get())
                return;
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

        }
    }

}
