package nars.gui.graph.run;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.control.Activate;
import nars.gui.graph.DynamicConceptSpace;
import spacegraph.SubOrtho;
import spacegraph.Surface;
import spacegraph.render.JoglPhysics;
import spacegraph.widget.console.TextEdit;
import spacegraph.widget.meta.AutoSurface;

import java.util.concurrent.atomic.AtomicBoolean;

import static spacegraph.layout.Gridding.grid;

public class SimpleConceptGraph1 extends DynamicConceptSpace {

    private final AtomicBoolean reloadReady = new AtomicBoolean(false);
    private final TextEdit inputbox;

    private SimpleConceptGraph1(NAR nar, int visibleNodes, int maxEdgesPerNodeMax) {
        this(nar, () -> nar.exe.active().iterator(),
                visibleNodes, maxEdgesPerNodeMax);
    }

    private SimpleConceptGraph1(NAR nar, Iterable<Activate> concepts, int maxNodes, int maxEdgesPerNodeMax) {
        super(nar, concepts, maxNodes, maxEdgesPerNodeMax);

        JoglPhysics sg = show(1400, 1000, false);


        sg.add(new SubOrtho(grid(
                new AutoSurface<>(sg.dyn.broadConstraints.get(0) /* FD hack */),
                new AutoSurface<>(vis)
        )).posWindow(0, 0, 1f, 0.2f));

        Surface inputPanel =
                (inputbox = new TextEdit() {
                    @Override
                    protected void onKeyCtrlEnter() {
                        reload();
                    }

                }).surface();
        //new Splitting()

        sg.add(new SubOrtho(
                inputPanel
        ).posWindow(0, 0.8f, 0.2f, 0.6f));

        float fps = 16;
        nar.startFPS(fps);

    }

    @Override
    public void update(JoglPhysics<Concept> s, long dtMS) {
        if (reloadReady.getAndSet(false)) {
//            active.clear();
            reload();
        }

        super.update(s, dtMS);
    }

    private void reload() {
        nar.runLater(() -> {

            nar.clear();
            nar.concepts.clear();
            //n.reset();


            nar.runLater(() -> {
                try {
                    nar.input(inputbox.text());
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                }
            });


        });
    }


    public static void main(String[] args) {

//        Param.DEBUG = true;
        //Param.TRACE = true;
        NAR n = NARS.threadSafe();


        //new DeductiveChainTest(n, 8,  2048, inh);


        SimpleConceptGraph1 g = new SimpleConceptGraph1(n,
                /* TODO */ 128, 5);


        //n.conceptActivation.set(0.2f);

        //csvPriority(n, "/tmp/x.csv");

//        Default n = O.of(new Default.DefaultTermIndex(512, new NARS.ExperimentalConceptBuilder()),
//                new CycleTime(), new BufferedSynchronousExecutor(64, 0.5f)).the(Default.class);

        //Default n = NARBuilder.newMultiThreadNAR(1, new RealTime.DSHalf(true).durSeconds(0.05f));
        //n.nal(1);
//        n.termVolumeMax.setValue(7f);
//        n.DEFAULT_BELIEF_PRIORITY = 0.9f;
//        n.DEFAULT_GOAL_PRIORITY = 0.9f;
//        n.DEFAULT_QUESTION_PRIORITY = 0.01f;
//        n.DEFAULT_QUEST_PRIORITY = 0.01f;

//        n.inputAt(1, "c:a?");
//        n.inputAt(2, "b:a.");
//        n.inputAt(3, "c:b.");

//                "(x:a ==> x:b).",
//                "(x:b ==> x:c).",
//                "(x:c ==> x:d).",
//                "(x:d ==> x:e).",
//                "(x:e ==> x:f)."
//                "(x:f ==> x:g).",
//                "(x:g ==> x:h)."

//        for (int i = 0; i < 10; i++) {
//            n.inputAt(i * 5 , i % 2 == 0 ? "x:c! :|:" : "--x:c! :|:");
//        }


        //n.log();
        //n.loop(2f);

//        for (int i = 1; i < 24; i++)
//            n.inputAt(i*2,"(" + ((char)('a' + i)) + "). :|:");

        //new DeductiveMeshTest(n, new int[]{3, 2}, 16384);


        //n.onCycle(nn->{System.out.println(nn.time() + "\n" + n.exe.stats() + "\n\n");});

        //n.services.printServices(System.out);


        //n.run(600);
        //n.log();
//        n.input(
//                "(a-->b).", "(b-->c)."
//        );
//        n.input(
//                "(c-->d).","(d-->e).","(e-->a).",
//                "(a<->x)!"
//        );
//
//        n.run(1).input("x(set,3)!  :|:");
//        n.run(1).input("x(set,3).  :|:");
//        n.run(1).input("x(get,#x)! :|:");
//        n.run(1).input("x(get,3).  :|:");
//        n.run(1).input("$1.0 x(get,4)!");


//                "{a,b}.", "{b,c}.","{c,d}."
//                ,"{d,e}.","{e,a}.", "(a,b,c,b)! :|:"

        //,"(a &&+1 b). :|:"

        //"$.50 at(SELF,{t001}). :|: %1.0;.90%", "$.70 (at(SELF,{t001}) &&+5 open({t001}))! %1.0;.90%" // //goal_ded_2


        //new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));

        //n.linkFeedbackRate.setValue(0.05f);


        //Param.DEBUG = true;
//        n
//                //.log()
//                //.logSummaryGT(System.out, 0.05f)
//                .input(
////                        "((parent($X,$Y) && parent($Y,$Z)) <=> grandparent($X,$Z)).",
////                        "parent(c, p).",
////                        "parent(p, g).",
////                        "grandparent(p, #g)?"
//                        "x:(a,(b,c))."
////                        "$0.9;0.9;0.9$ (a,(b,(c,(d,e))))."
////
//                );
//                //.run(800);
//


    }


//    public static void csvPriority(NAR n, String path) {
//
//        CSVOutput csv = new CSVOutput(
//                //new File(path),
//                System.out,
//                "time", "ALL_Pri", "NAL_Pri", "ConceptFire_Pri", "busy");
//
//        MultiStatistics<ITask> exeTasks = new MultiStatistics<ITask>()
//                .classify("NALTask", NALTask.class::isInstance)
//                .classify("ConceptFire", Activate.class::isInstance)
//                ;
////        n.onCycle((nn)->{
////            n.exe.forEach(exeTasks);
////            csv.out(
////                    nn.time(),
////
////                    ((StatisticalSummary) exeTasks.cond.get(0)).getSum(),
////
////                    ((StatisticalSummary) exeTasks.cond.get(1)).getSum(),
////
////                    ((StatisticalSummary) exeTasks.cond.get(2)).getSum(),
////
////                    nn.emotion.busyVol.getSum()
////            );
////            exeTasks.clear();
////        });
//    }

}
