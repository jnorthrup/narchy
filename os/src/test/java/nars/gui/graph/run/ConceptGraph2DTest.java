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

            NAR n = NARS.tmp(4);
            n.termVolumeMax.set(14);


            Surface g = BagregateConceptGraph2D.get(n);

            SpaceGraph.window(g, 1200, 800 );

            new DeductiveMeshTest(n, 5,5);

            n.startFPS(24f);
        }

    }
    static class GraphRDFTest1 {
        public static void main(String[] args) throws FileNotFoundException {


            NAR n = NARS.tmp();
            n.log();

            NQuadsRDF.input(n, new File("/home/me/d/valueflows.nquad"));

            Surface g = BagregateConceptGraph2D.get(n);

            SpaceGraph.window( new Windo(g), 1200, 800 );

            n.startFPS(16f);
        }

    }

}