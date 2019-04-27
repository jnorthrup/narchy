//package nars.experiment;
//
//import com.google.common.collect.Iterables;
//import com.google.common.collect.Lists;
//import jcog.Util;
//import jcog.math.FloatSupplier;
//import nars.$;
//import nars.NAR;
//import nars.NARS;
//import nars.Task;
//import nars.gui.EmotionPlot;
//import nars.gui.NARui;
//import ConjClustering;
//import nars.task.DerivedTask;
//import nars.test.agent.Line1DSimplest;
//import nars.util.TimeAware;
//import org.eclipse.collections.api.block.function.primitive.FloatFunction;
//import spacegraph.SpaceGraph;
//import spacegraph.space2d.container.grid.Gridding;
//import spacegraph.space2d.widget.meta.AutoSurface;
//import spacegraph.space2d.widget.meter.Plot2D;
//
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import static java.lang.Math.PI;
//import static nars.Op.BELIEF;
//import static spacegraph.space2d.container.grid.Gridding.*;
//
///**
// * Created by me on 3/15/17.
// */
//public class Line1D {
//    public static class Line1DVis {
//
//
//        public static void main(String[] args) {
//
//
//
//
//
//
//
//
//
//            NAR n = NARS.tmp();
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//            n.freqResolution.setAt(0.02f);
//            n.confResolution.setAt(0.02f);
//            n.termVolumeMax.setAt(12);
//
//
//
//
//
//
//            ConjClustering conjClusterB = new ConjClustering(n,  BELIEF, t ->true, 16, 64);
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//            Line1DExperiment exp = new Line1DExperiment() {
//                @Override
//                protected void onStart(Line1DSimplest a) {
//
//                    new Thread(() -> {
//                        int history = 64;
//                        SpaceGraph.window(
//                                row(
//                                        conceptPlot(a.nar(), Lists.newArrayList(
//                                                () -> (float) a.i.floatValue(),
//                                                a.o,
//
//                                                () -> a.reward
//
//                                                )
//                                                ,
//                                                history),
//                                        col(
//                                                new EmotionPlot(history, a),
//                                                new AutoSurface<>(a),
//                                                NARui.beliefCharts(history,
//                                                        Iterables.concat(a.sensors.keySet(), a.actions.keySet()), a.nar())
//                                        )
//                                )
//                                , 900, 900);
//
//                    }).start();
//                    a.nar().onTask(t -> {
//                        if (!t.isInput() && t instanceof DerivedTask
//                                && t.isGoal()) {
//
//
//
//                                System.err.println(t.proof());
//                        }
//                    });
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//                }
//            };
//            exp.floatValueOf(n);
//
//
//
//            n.time.dur(32);
//            exp.agent.curiosity.setAt(0.1f);
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//            n.run(15);
//            n.startFPS(100f);
//
//
//
//
//
//
//
//
//
//        }
//
//        public static Gridding conceptPlot(NAR nar, Iterable<FloatSupplier> concepts, int plotHistory) {
//
//
//            Gridding grid = new Gridding(VERTICAL);
//            List<Plot2D> plots = $.newArrayList();
//            for (FloatSupplier t : concepts) {
//                Plot2D p = new Plot2D(plotHistory, Plot2D.Line);
//                p.addAt(t.toString(), t::asFloat, 0f, 1f);
//                grid.addAt(p);
//                plots.addAt(p);
//            }
//            grid.layout();
//
//            nar.onCycle(f -> {
//                plots.forEach(Plot2D::update);
//            });
//
//            return grid;
//        }
//
//    }
//
//    static class Line1DExperiment implements FloatFunction<TimeAware> {
//
//
//        float tHz = 0.05f;
//        float yResolution = 0.05f;
//
//
//
//        public Line1DSimplest agent;
//
//        @Override
//        public float floatValueOf(TimeAware n) {
//
//
//
//
//            AtomicBoolean AUTO = new AtomicBoolean(true);
//            agent = new Line1DSimplest() {
//                public final AtomicBoolean auto = AUTO;
//
//
//
//
//                @Override
//                protected float act() {
//                    return (float) Math.pow(super.act(), 3);
//                }
//            };
//
//
//            onStart(agent);
//
//
//            agent.in.resolution(yResolution);
//
//
//
//            agent.curiosity.setAt(
//                    0.05f
//
//            );
//
//
//
//
//
//
//
//
//
//
//
//
//            TimeAware timeAware = agent.nar();
//            agent.onFrame((z) -> {
//
//                if (AUTO.get()) {
//                    agent.target(
//
//                            Util.round((float) (0.5f + 0.5f * Math.sin(timeAware.time() * tHz * 2 * PI / timeAware.dur())), yResolution)
//
//
//
//                    );
//                }
//
//
//            });
//
//
//
//
//            return 0f;
//
//        }
//
//        protected void onStart(Line1DSimplest a) {
//
//        }
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    public static class Line1DTrainer {
//
//        public static final int trainingRounds = 20;
//        private float lastReward;
//        int consecutiveCorrect;
//        int lag;
//
//        int step;
//
//
//        final LinkedHashSet<Task>
//                current = new LinkedHashSet();
//
//        private final Line1DSimplest a;
//
//
//        int completionThreshold;
//
//        float worsenThreshold;
//
//        public Line1DTrainer(Line1DSimplest a) {
//            this.a = a;
//            this.lastReward = a.reward;
//
//            NAR n = a.nar();
//
//
//            float speed = a.speed.floatValue();
//            this.worsenThreshold = speed / 2f;
//            this.completionThreshold = n.dur() * 32;
//            float rewardThresh = 0.75f;
//
//            n.onTask(x -> {
//                if (step > trainingRounds && x.isGoal() && !x.isInput()
//
//
//                        ) {
//                    current.addAt(x);
//                }
//            });
//
//            a.onFrame((z) -> {
//
//
//
//                if (a.reward > rewardThresh)
//                    consecutiveCorrect++;
//                else
//                    consecutiveCorrect = 0;
//
//                if (consecutiveCorrect > completionThreshold) {
//
//                    System.out.println(lag);
//
//                    float next = Util.round(n.random().nextFloat(), speed);
//
//                    a.target(next);
//
//                    step++;
//                    consecutiveCorrect = 0;
//                    lag = 0;
//
//                    if (step < trainingRounds) {
//
//                    } else {
//                        if (a.curiosity.floatValue() > 0)
//                            System.err.println("TRAINING FINISHED - DISABLING CURIOSITY");
//                        a.curiosity.setAt(0f);
//                    }
//                } else {
//
//                    if (lag > 1) {
//
//                        float worsening = lastReward - a.reward;
//                        if (step > trainingRounds && worsening > worsenThreshold) {
//
//                            current.forEach(x -> {
//                                System.err.println(worsening + "\t" + x.proof());
//                            });
//                        }
//                    }
//
//                    lag++;
//
//                }
//
//                lastReward = a.reward;
//
//                current.clear();
//            });
//        }
//
//    }
//
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
