package nars.gui.graph.run;

import jcog.Util;
import nars.NAR;
import nars.NARS;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.gui.NARui;
import nars.op.rdfowl.NQuadsRDF;
import nars.test.impl.DeductiveMeshTest;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.video.OrthoSurfaceGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static nars.$.$$;
import static spacegraph.SpaceGraph.window;

class ConceptGraph2DTest {

    static class GraphMesh {
        public static void main(String[] args) {

            NAR n = NARS
                    //.tmp(4);
                    .threadSafe(4);
//            n.attn.decay.set(0.9f);
            n.termVolMax.set(14);

            Surface g = BagregateConceptGraph2D.get(n);

            OrthoSurfaceGraph wg = window(g, 1200, 800);
            wg.dev();


            n.startFPS(4f);

            new DeductiveMeshTest(n, 5,5);

        }

    }
    static class InhibitionTest {
        public static void main(String[] args) {
//            NAR n = NARS
//                    //.tmp(4);
//                    .threadSafe(7);
            NAR n = new NARS.DefaultNAR(0, true).get();
            new Deriver(Derivers.nal(n, 1, 6));
            n.what().onTask(t -> {
               n.proofPrint(t);
//                try {
//                    System.in.read();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            });

            n.time.dur(25);
            n.termVolMax.set(9);
            n.freqResolution.set(0.01f);

            Surface g = BagregateConceptGraph2D.get(n);
            long now = n.time();
            long next = Math.round(now + ((double)n.dur()));
            window(new Splitting(g, 0.2f, new Gridding(
                new Splitting(
                    new Splitting(
                        new PushButton("good+").clicked(()->
                            n.believe($$("good"), now, next, 1f, 0.9f)
                        ), 0.5f,
                        new PushButton("good-").clicked(()->
                            n.believe($$("good"), now, next, 0f, 0.9f)
                        )
                    ),
                    0.5f,
                    new Splitting(
                        new PushButton("bad+").clicked(()->
                            n.believe($$("bad"), now, next, 1f, 0.9f)
                        ), 0.5f,
                        new PushButton("bad-").clicked(()->
                            n.believe($$("bad"), now, next, 0f, 0.9f)
                        )
                    )
                    ),
                new Gridding(
                    NARui.beliefCharts(n, $$("good"), $$("bad"), $$("reward")),
                    NARui.beliefIcons(n, List.of($$("good"), $$("bad"), $$("reward")))
                ),
                new Gridding(
                    new PushButton("reset").clicked(()->n.reset()),
                    new PushButton("print").clicked(()->n.tasks().forEach(System.out::println))
                )
            )), 1200, 800 );

            n.startFPS(4f);

            n.log();

            n.want("reward");
            n.believe("(good ==> reward)", 1, 0.9f);
            n.believe("(bad ==> reward)", 0, 0.9f);
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

            window( new Windo(g), 1200, 800 );

            n.startFPS(16f);
        }

    }

}