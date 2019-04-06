package nars.gui.graph.run;

import nars.NAR;
import nars.NARS;
import nars.op.rdfowl.NQuadsRDF;
import nars.test.impl.DeductiveMeshTest;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.Windo;

import java.io.File;
import java.io.FileNotFoundException;

class ConceptGraph2DTest {

    static class GraphMesh {
        public static void main(String[] args) {

            NAR n = NARS
                    //.tmp(4);
                    .threadSafe(4);
            n.attn.decay.set(0.9f);
            n.termVolumeMax.set(14);

            SpaceGraph.surfaceWindow(BagregateConceptGraph2D.get(n), 1200, 800 );

            n.startFPS(24f);

            new DeductiveMeshTest(n, 5,5);

        }

    }
    static class InhibitionTest {
        public static void main(String[] args) {
            NAR n = NARS
                    //.tmp(4);
                    .threadSafe(4);
            n.termVolumeMax.set(5);

            SpaceGraph.surfaceWindow(BagregateConceptGraph2D.get(n), 1200, 800 );

            n.startFPS(4f);

            n.want("reward");
            n.believe("(good ==> reward)", 1, 0.9f);
            n.believe("(--bad ==> reward)", 1, 0.9f);
            //.mustGoal(cycles, "good", 1.0f, 0.81f)
            //.mustGoal(cycles, "bad", 0.0f, 0.81f);
        }
    }
    static class GraphRDFTest1 {
        public static void main(String[] args) throws FileNotFoundException {


            NAR n = NARS.tmp();
            n.log();

            NQuadsRDF.input(n, new File("/home/me/d/valueflows.nquad"));

            Surface g = BagregateConceptGraph2D.get(n);

            SpaceGraph.surfaceWindow( new Windo(g), 1200, 800 );

            n.startFPS(16f);
        }

    }

}