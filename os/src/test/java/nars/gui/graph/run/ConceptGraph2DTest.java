package nars.gui.graph.run;

import nars.NAR;
import nars.NARS;
import nars.attention.TaskLinkWhat;
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
                    .threadSafe(8);
            ((TaskLinkWhat)n.what()).links.decay.set(0.5f);
            n.questionPriDefault.pri(0.9f);
            n.beliefPriDefault.pri(0.1f);
            n.termVolMax.set(14);

            Surface g = BagregateConceptGraph2D.get(n);

            window(g, 1200, 800);
            window(NARui.attentionUI(n.what()), 800, 500);
//            wg.dev();

            new DeductiveMeshTest(n, 3,3);

            n.startFPS(16);


        }

    }
    static class InhibitionTest {
        public static void main(String[] args) {
//            NAR n = NARS
//                    //.tmp(4);
//                    .threadSafe(7);
            NAR n = new NARS.DefaultNAR(0, true).get();
            new Deriver(Derivers.nal(n, 1, 8));
            //                try {
            //                    System.in.read();
            //                } catch (IOException e) {
            //                    e.printStackTrace();
            //                }
            n.what().onTask(n::proofPrint);

            n.time.dur(10);
            n.termVolMax.set(9);
            n.freqResolution.set(0.01f);

            Surface g = BagregateConceptGraph2D.get(n);
            window(new Splitting(g, 0.2f, new Gridding(
                new Splitting(
                    new Splitting(
                        new PushButton("good+").clicked(()->
                            n.believe($$("good"), n.time(), Math.round(n.time() + ((double)n.dur())), 1f, 0.9f)
                        ), 0.5f,
                        new PushButton("good-").clicked(()->
                            n.believe($$("good"), n.time(), Math.round(n.time() + ((double)n.dur())), 0f, 0.9f)
                        )
                    ),
                    0.5f,
                    new Splitting(
                        new PushButton("bad+").clicked(()->
                            n.believe($$("bad"), n.time(), Math.round(n.time() + ((double)n.dur())), 1f, 0.9f)
                        ), 0.5f,
                        new PushButton("bad-").clicked(()->
                            n.believe($$("bad"), n.time(), Math.round(n.time() + ((double)n.dur())), 0f, 0.9f)
                        )
                    )
                    ),
                new Gridding(
                    NARui.beliefCharts(n, $$("good"), $$("bad"), $$("reward")),
                    NARui.beliefIcons(n, List.of($$("good"), $$("bad"), $$("reward")))
                ),
                new Gridding(
                    new PushButton("reset").clicked(n::reset),
                    new PushButton("print").clicked(()->n.tasks().forEach(System.out::println))
                )
            )), 1200, 800 );

            n.startFPS(8f);

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