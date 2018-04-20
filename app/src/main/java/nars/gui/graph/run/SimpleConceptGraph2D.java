package nars.gui.graph.run;

import jcog.pri.PriReference;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.term.Term;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Graph2D;
import spacegraph.space2d.container.ForceDirected2D;

public class SimpleConceptGraph2D {
    public static void main(String[] args) throws Narsese.NarseseException {

        NAR n = NARS.tmp();
        n.input("a:b.");
        n.input("b:c.");
        n.input("c:d.");
        n.input("d:e.");
        n.run(10);

        Graph2D<Concept> g;
        SpaceGraph.window(
            g = new Graph2D().setLayout(new ForceDirected2D()),
            800, 800
        );
        n.onCycle(()->{
            g.update(()->n.conceptsActive().map(x->x.get()).iterator(),
                (c)->{
                    return c.termlinks();
            }, (PriReference<Term> l, Graph2D.Link<Concept> link)->{
                Concept tgt = n.concept(l.get());
                if (tgt!=null) {
                    float p = l.priElseZero();
                    link.color(p, p/2f, 0);
                    return tgt;
                }
                return null;
            }, true);
        });
        n.startFPS(2f);
    }
}
