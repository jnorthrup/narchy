package nars.gui.graph.run;

import nars.NAR;
import nars.NARS;
import nars.test.impl.DeductiveMeshTest;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.windo.Windo;

class ConceptGraph2DTest {
    public static void main(String[] args) {

        NAR n = NARS.tmp(4);
        n.termVolumeMax.set(10);





        new DeductiveMeshTest(n, 5, 5);

        ConceptGraph2D g = new ConceptGraph2D(n);

        SpaceGraph.window(
                
                new Windo(g.widget())
                
                , 1200, 800
        );

        n.startFPS(8f);
    }


}