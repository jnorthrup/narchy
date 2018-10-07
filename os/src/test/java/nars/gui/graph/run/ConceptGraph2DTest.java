package nars.gui.graph.run;

import nars.NAR;
import nars.NARS;
import nars.op.rdfowl.NQuadsRDF;
import nars.test.impl.DeductiveMeshTest;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.windo.Windo;

import java.io.File;
import java.io.FileNotFoundException;

class ConceptGraph2DTest {

    static class GraphMesh {
        public static void main(String[] args) {

            NAR n = NARS.tmp(4);
            n.termVolumeMax.set(10);

            new DeductiveMeshTest(n, 5, 5);

            ConceptGraph2D g = BagregateConceptGraph2D.get(n);

            SpaceGraph.window( new Windo(g.widget()), 1200, 800 );

            n.startFPS(32f);
        }

    }
    static class GraphRDFTest1 {
        public static void main(String[] args) throws FileNotFoundException {


            NAR n = NARS.tmp();
            n.log();

            NQuadsRDF.input(n, new File("/home/me/d/valueflows.nquad"));

            ConceptGraph2D g = BagregateConceptGraph2D.get(n);

            SpaceGraph.window( new Windo(g.widget()), 1200, 800 );

            n.startFPS(16f);
        }

    }

}