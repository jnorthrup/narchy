package nars.experiment;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.task.DerivedTask;
import nars.test.agent.Line1DSimplest;
import nars.truth.PreciseTruth;
import spacegraph.container.Gridding;
import spacegraph.widget.meter.Plot2D;

import java.util.Collection;
import java.util.List;

import static java.lang.Math.PI;
import static jcog.Texts.n2;
import static jcog.Texts.n4;
import static spacegraph.container.Gridding.VERTICAL;

public class Line1DCalibrate {


    public static void main(String[] args) {

        Param.DEBUG = true;
        NAR n = NARS.threadSafe();

        //new STMTemporalLinkage(n, 2, false);
        n.time.dur(1);
        n.termVolumeMax.set(16);
        //n.beliefConfidence(0.9f);
        //n.goalConfidence(0.5f);
//            n.onCycle((nn) -> {
//                nn.stats(System.out);
//            });

        //n.truthResolution.setValue(0.05f);

        Line1DSimplest a = new Line1DSimplest() {

//                final FloatAveraged rewardAveraged = new FloatAveraged(()->super.act(), 10);

            @Override
            protected float act() {

                float r = super.act();
                System.out.println("reward: " + now + "\t^" + n2(i.floatValue()) + "\t@" + n2(o.floatValue()) + "\t\t= " + r);
                return r;
            }
        };

        float tHz = 0.05f; //in time units
        float yResolution = 0.1f; //in 0..1.0
        float periods = 16;

        final int runtime = Math.round(periods / tHz);

        Collection actions = a.actions.values(); //Set.of(a.up.term(), a.down.term());
        n.onTask(t -> {
            if (t instanceof DerivedTask) {
                if (t.isGoal()) {
                    if (actions.contains(t.term())) {

                        float dir = new PreciseTruth(t.freq(), t.evi(a.nar.time(), a.nar.dur()), false).freq() - 0.5f;

                        //TEST POLARITY
                        float i = a.i.floatValue();
                        float o = a.o.floatValue();
                        float neededDir = (i - o);
                        boolean good = Math.signum(neededDir) == Math.signum(dir);
                        /*if (!good)*/
                        System.err.println(n4(dir) + "\t" + good + " " + i + " <-? " + o);
                        System.err.println(t.proof());
                        System.out.println();
                    }
                    if (t.isGoal())
                        System.err.println(t.proof());

                } else {

                    //System.err.println(t.toString(n));
                }
            }
        });

        a.speed.set(yResolution);

//            a.up.resolution.setValue(yResolution);
//            a.down.resolution.setValue(yResolution);
        a.in.resolution(yResolution);
        a.curiosity.set(
                0.1f
                //(2/yResolution)*tHz);
        );

        //            a.in.beliefs().capacity(0, 100, a.nar);
        //            a.out.beliefs().capacity(0, 100, a.nar);
        //            a.out.goals().capacity(0, 100, a.nar);

        //Line1DTrainer trainer = new Line1DTrainer(a);

        //new RLBooster(a, new HaiQAgent(), 5);

        //ImplicationBooster.implAccelerator(a);


        a.onFrame((z) -> {

            a.target(
                    //Math.signum(Math.sin(a.nar.time() * tHz * 2 * PI) ) > 0 ? 1f : -1f
                    Util.round((float) (0.5f + 0.5f * Math.sin(a.nar.time() * tHz * 2 * PI)), yResolution)
                    //(float) ( Math.sin(a.nar.time() * tHz * 2 * PI) )
                    //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                    //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
            );

            //Util.pause(1);
        });


//            a.runCycles(runtime);

//                    new Thread(() -> {
//                        //NAgentX.chart(a);
//                        int history = 800;
//                        window(
//                                row(
//                                        conceptPlot(a.nar, Lists.newArrayList(
//                                                () -> (float) a.i.floatValue(),
//                                                a.o,
//                                                //a.out.feedback.current!=null ? a.out.feedback.current.freq() : 0f,
//                                                () -> a.reward
//                                                //() -> a.rewardSum
//                                                )
//                                                ,
//                                                history),
//                                        col(
//                                                new Vis.EmotionPlot(history, a),
//                                                new ReflectionSurface<>(a),
//                                                Vis.beliefCharts(history,
//                                                        Iterables.concat(a.sensors.keySet(), a.actions.keySet()), a.nar)
//                                        )
//                                )
//                                , 900, 900);
//
//                    }).start();

        //n.startFPS(100);
        n.run(2000);
//            n.tasks().forEach(x -> {
//               if (x.isBelief() && x.op()==IMPL) {
//                   System.out.println(x.proof());
//               }
//            });


    }

    public static Gridding conceptPlot(NAR nar, Iterable<FloatSupplier> concepts, int plotHistory) {

        //TODO make a lambda Grid constructor
        Gridding grid = new Gridding(VERTICAL);
        List<Plot2D> plots = $.newArrayList();
        for (FloatSupplier t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line);
            p.add(t.toString(), t::asFloat, 0f, 1f);
            grid.add(p);
            plots.add(p);
        }
        grid.layout();

        nar.onCycle(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }


}
