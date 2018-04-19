package nars.gui.graph.run;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.util.graph.TermGraph;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Graph2D;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.ForceDirected2D;

public class SimpleConceptGraph2D {
    public static void main(String[] args) throws Narsese.NarseseException {

        NAR n = NARS.tmp();
        n.input("a:b.");
        n.input("b:c.");
        n.run(10);

        SpaceGraph.window(
            new Graph2D() {
                float dtAccum = 0;
                @Override
                public boolean start(SurfaceBase parent) {
                    if (super.start(parent)) {
                        root().animate((float dt) -> {
                            dtAccum += dt;
                            while (dtAccum > 1f) {
                                dtAccum -= 1f;
                                n.run();
                                commit(TermGraph.termlink(n));
                            }
                            return true;
                        });
                        return true;
                    }
                    return false;
                }

            }
                    .setLayout(new ForceDirected2D()),
            800, 800
        );
    }
}
