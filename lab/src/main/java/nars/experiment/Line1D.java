package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import jcog.Util;
import jcog.math.FloatSupplier;
import nars.*;
import nars.control.MetaGoal;
import nars.gui.Vis;
import nars.op.stm.ConjClustering;
import nars.task.DerivedTask;
import nars.test.agent.Line1DSimplest;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import spacegraph.layout.Gridding;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.meter.Plot2D;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.PI;
import static nars.Op.BELIEF;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Gridding.*;

/**
 * Created by me on 3/15/17.
 */
public class Line1D {
    public static class Line1DVis {


        public static void main(String[] args) {


//                InstrumentedExecutor exe =
//                        new InstrumentedExecutor(
//                        new TaskExecutor(256, 0.5f)
//                );
//
            Param.DEBUG = true;

            NAR n = NARS.tmp(); //new NARS().threadable().nal(8);
            //NAR n = NARS.shell();
            //nn.deriverAdd(1,8);
            //nn.deriverAdd(6, 8);
//            nn.deriver(
//                    "B, (A ==> C), time(urgent),  notImpl(B) |- subIfUnifiesAny(C,A,B,\"$\"), (Belief:DeductionRecursivePB, Goal:DeciDeduction)",
//                    "B, (--A ==> C), time(urgent),  notImpl(B) |- subIfUnifiesAny(C,A,B,\"$\"), (Belief:DeductionRecursivePBN, Goal:DeciDeductionN)",
//                    "B, (C ==> A), time(urgent),  notImpl(B) |- subIfUnifiesAny(C,A,B,\"$\"), (Belief:AbductionRecursivePB, Goal:DeciInduction)",
//                    "B, C, belief(\"&&|\"), belief(containsTask), task(\"!\"), time(urgent) |- without(C,B), (Goal:Strong)",
//                    "B, C, belief(\"&&|\"), belief(containsTask), task(\"!\"), time(urgent) |- without(C,--B), (Goal:StrongN)"
//            );
            //NAR n = nn.get();

//            try {

                //n.believe("(i ==> o)");
//            n.goal("(i && (\"+\"-->y))");
//            n.goal("(--i && (\"-\"-->y))");

//                n.goal("(i|o)");
//                n.goal("(--i|--o)");
//                n.believe("(--(i|o) ==> happy)");
//                n.believe("(--(i-o) ==> happy)");
//                n.believe("(--(o-i) ==> happy)");
//            } catch (Narsese.NarseseException e) {
//                e.printStackTrace();
//            }
            n.freqResolution.set(0.02f);
            n.confResolution.set(0.02f);
            n.termVolumeMax.set(12);

            MetaGoal.Perceive.set(n.want, -0.01f);
            MetaGoal.Desire.set(n.want, 0.25f);
            MetaGoal.Action.set(n.want, 2f);


            ConjClustering conjClusterB = new ConjClustering(n,  BELIEF, t ->true, 16, 64);

//            n.onTask(x -> {
//                if (x instanceof DerivedTask) {
//                    System.err.println(x.proof());
//                }
//            });
//            n.onCycle(()->{
//                System.out.println(n.time()+":");
//                n.exe.active().forEach(System.out::println);
//                System.out.println();
//            });
//            n.log();

//            n.runLater(() -> {
//                try {
//                    n.input(""
//                            //"(y,())! %0.5;0.02%",
//                            //"((),y)! %0.5;0.02%"
////                            "$0.99 ((&&, i, --o) &&+1 ((),y))!",
////                            "$0.99 ((&&, --i, o) &&+1 (y,()))!"
////                            "$0.99 ((&|, i, --o) =|> happy).",
////                            "$0.99 ((&|, --i, o) =|> happy)."
//                    );
//
//                    //            n.input(
//                    //                "(i =|> (y,()))."
//                    //            );
//                    //            n.input("(happy =|> --(i-o)).",
//                    //                    "(happy =|> --(o-i)).",
//                    //                    "(i-o)?",
//                    //                    "(o-i)?"
//                    //            );
//                    //            n.input("$.99 ((i-o) &| (y,()))!",
//                    //                    "$.99 ((o-i) &| ((),y))!"
//                    //            );
//                } catch (Narsese.NarseseException e) {
//                    e.printStackTrace();
//                }
//            });


            //n.beliefConfidence(0.9f);
            //n.goalConfidence(0.5f);
//            n.onCycle((nn) -> {
//                nn.stats(System.out);
//            });
            //n.setEmotion(new Emotivation(n));

            Line1DExperiment exp = new Line1DExperiment() {
                @Override
                protected void onStart(Line1DSimplest a) {

                    new Thread(() -> {
                        int history = 64;
                        window(
                                row(
                                        conceptPlot(a.nar, Lists.newArrayList(
                                                () -> (float) a.i.floatValue(),
                                                a.o,
                                                //a.out.feedback.current!=null ? a.out.feedback.current.freq() : 0f,
                                                () -> a.reward
                                                //() -> a.rewardSum
                                                )
                                                ,
                                                history),
                                        col(
                                                new Vis.EmotionPlot(history, a),
                                                new AutoSurface<>(a),
                                                Vis.beliefCharts(history,
                                                        Iterables.concat(a.sensors.keySet(), a.actions.keySet()), a.nar)
                                        )
                                )
                                , 900, 900);

                    }).start();
                    a.nar.onTask(t -> {
                        if (!t.isInput() && t instanceof DerivedTask
                                && t.isGoal()) {

                            //String s = t.term().toString();
                            //if (s.equals("(y,())") || s.equals("((),y)"))
                                System.err.println(t.proof());
                        }
                    });

//                  Term[] c = Util.map((String s) -> $.$safe(s), Term[]::new,
////                            "((i)==>happy)",
////                            "((i)-(y,\"+\"))",
////                            "((i)-(y,\"-\"))",
////                            "((y,\"+\")-(i))",
////                            "((y,\"-\")-(i))"
//                            "((y,\"+\")-(y,\"-\"))",
//                            "((y,\"-\")-(y,\"+\"))"
//
//                    );
//                    a.nar.onCycle(() -> {
//                        for (Term x : c) {
//                            System.out.println(x + " " + a.nar.beliefTruth(x, a.nar.time()));
//                        }
//                        System.out.println();
//                    });
                }
            };
            exp.floatValueOf(n);

            //new RelationClustering(n, (t)->1f, 4, 16);

            n.time.dur(32);
            exp.agent.curiosity.set(0.1f);
            exp.agent.runDur(2);

            //n.truthResolution.setValue(0.25f);

            //new RLBooster(exp.agent, HaiQAgent::new, 5);

//            n.beliefConfidence(0.5f);
//            n.goalConfidence(0.5f);

//            new Implier(n, exp.agent.actions.keySet().stream().map(x->x.term).collect(toList()),
//                    0f,1f, 2f, 3f, 4f);


            //n.goal("--(x~y)");
            //n.goal("--(y~x)");
//            Term xMinY = $.$safe("(x~y)");
//            Term yMinX = $.$safe("(y~x)");
//            n.onCycle(()->{
//                   System.out.println(xMinY + ": " + n.belief(xMinY) + "\t" + yMinX + ": " + n.belief(yMinX));
//            });
            //n.start();
            //n.run(10000);
            //n.log();
            n.run(15);
            n.startFPS(100f);
            //n.run();

//            n.concepts().forEach(x -> {
//                if (x.op() == IMPL) {
//                    x.print();
//                }
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

    static class Line1DExperiment implements FloatFunction<NAR> {


        float tHz = 0.05f; //in time units
        float yResolution = 0.05f; //in 0..1.0
//        float periods = 16;

//        final int runtime = Math.round(periods / tHz);
        public Line1DSimplest agent;

        @Override
        public float floatValueOf(NAR n) {

            //n.truthResolution.setValue(0.05f);


            AtomicBoolean AUTO = new AtomicBoolean(true);
            agent = new Line1DSimplest(n) {
                public final AtomicBoolean auto = AUTO;

//                final FloatAveraged rewardAveraged = new FloatAveraged(()->super.act(), 10);


                @Override
                protected float act() {
                    return (float) Math.pow(super.act(), 3);
                }
            };


            onStart(agent);


            agent.in.resolution(yResolution);
//            for (GoalActionConcept g : new GoalActionConcept[]{agent.up, agent.down})
//                g.resolution(yResolution);

            agent.curiosity.set(
                    0.05f
                    //(2/yResolution)*tHz);
            );

            //            a.in.beliefs().capacity(0, 100, a.nar);
            //            a.out.beliefs().capacity(0, 100, a.nar);
            //            a.out.goals().capacity(0, 100, a.nar);

            //Line1DTrainer trainer = new Line1DTrainer(a);

            //new RLBooster(agent, HaiQAgent::new, 3);

            //ImplicationBooster.implAccelerator(a);


            agent.onFrame((z) -> {

                if (AUTO.get()) {
                    agent.target(
                            //Math.signum(Math.sin(a.nar.time() * tHz * 2 * PI) ) > 0 ? 1f : -1f
                            Util.round((float) (0.5f + 0.5f * Math.sin(agent.nar.time() * tHz * 2 * PI / agent.nar.dur())), yResolution)
                            //(float) ( Math.sin(a.nar.time() * tHz * 2 * PI) )
                            //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                            //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
                    );
                }

                //Util.pause(1);
            });

//            a.runCycles(runtime);

            //return agent.rewardSum / runtime;
            return 0f;

        }

        protected void onStart(Line1DSimplest a) {

        }
    }

//
//    public static class Line1DOptimize {
//
//        public static void main(String[] args) {
//
//            int maxIterations = 1024;
//            int repeats = 2;
//
//            Optimize<NAR> o = new MeshOptimize<NAR>("d1", () -> {
//
//                NAR n = new NARS().get();
//                n.random().setSeed(System.nanoTime());
//
//                n.time.dur(5);
//                n.termVolumeMax.set(12);
//
//                return n;
//            })/*.tweak("beliefConf", 0.1f, 0.9f, 0.1f, (y, x) -> {
//                x.beliefConfidence(y);
//            })*/.tweak("goalConf", 0.1f, 0.9f, 0.1f, (y, x) -> {
//                x.goalConfidence(y);
//            }).tweak("belfPri", 0.1f, 1.0f, 0.1f, (y, x) -> {
//                x.DEFAULT_BELIEF_PRIORITY = (y);
//            }).tweak("goalPri", 0.1f, 1.0f, 0.1f, (y, x) -> {
//                x.DEFAULT_GOAL_PRIORITY = (y);
//            }).tweak("questionPri", 0.1f, 1.0f, 0.1f, (y, x) -> {
//                x.DEFAULT_QUESTION_PRIORITY = (y);
//                x.DEFAULT_QUEST_PRIORITY = (y);
//            })
//
//                    /*.tweak("termVolMax", 10, 25, 1, (y, x) -> {
//                x.termVolumeMax.setValue(y);
////            }).tweak("exeRate", 0.1f, 0.9f, 0.1f, (y, x) -> {
////                ((TaskExecutor) x.exe).exePerCycleMax.setValue(y);
//            })*//*.tweak("activation", 0.1f, 1f, 0.1f, (y, x) -> {
//                x.in.streams.values().forEach(s -> s.setValue(y));
//            })*//*.tweak("stmSize", 1, 2, 1, (y, x) -> {
//                ((Default) x).stmLinkage.capacity.setValue(y);
//            })*//*.tweak("confMin", 0.05f, 0.5f, 0.01f, (y, x) -> {
//                x.confMin.setValue(y);
//            })*//*.tweak("truthResolution", 0.01f, 0.04f, 0.01f, (y, x) -> {
//                x.truthResolution.setValue(y);
//            })*/;
//
//            Optimize.Result r = o.run(maxIterations, repeats, new Line1DExperiment());
//
//            r.print();
//
//            RealDecisionTree t = r.tree(3, o.tweaks.tweaks.size());
//            t.print();
//
////            float predictedScore = t.get(0, 1, 0, 1, Float.NaN);
////            System.out.println(predictedScore);
//
//            //t.root().recurse().forEach(x -> System.out.println(x));
//
////            System.out.println(t.leaves().collect(toList()));
//
//
//        }
//
//
//    }

    public static class Line1DTrainer {

        public static final int trainingRounds = 20;
        private float lastReward;
        int consecutiveCorrect;
        int lag;
        //int perfect = 0;
        int step;


        final LinkedHashSet<Task>
                current = new LinkedHashSet();

        private final Line1DSimplest a;

        //how long the correct state must be held before it advances to next step
        int completionThreshold;

        float worsenThreshold;

        public Line1DTrainer(Line1DSimplest a) {
            this.a = a;
            this.lastReward = a.reward;

            NAR n = a.nar;


            float speed = a.speed.floatValue();
            this.worsenThreshold = speed / 2f;
            this.completionThreshold = n.dur() * 32;
            float rewardThresh = 0.75f; //reward to be considered correct in this frame

            n.onTask(x -> {
                if (step > trainingRounds && x.isGoal() && !x.isInput()

                    //&& x.term().equals(a.out.term())
                        ) {
                    current.add(x);
                }
            });

            a.onFrame((z) -> {


                //System.out.println(a.reward);
                if (a.reward > rewardThresh)
                    consecutiveCorrect++;
                else
                    consecutiveCorrect = 0; //start over

                if (consecutiveCorrect > completionThreshold) {
                    //int lagCorrected = lag - perfect;
                    System.out.println(lag);

                    float next = Util.round(n.random().nextFloat(), speed);
                    //perfect = (int) Math.floor((next - a.target()) / speed);
                    a.target(next);

                    step++;
                    consecutiveCorrect = 0;
                    lag = 0;

                    if (step < trainingRounds) {
                        //completionThreshold += n.dur(); //increase completion threshold
                    } else {
                        if (a.curiosity.floatValue() > 0)
                            System.err.println("TRAINING FINISHED - DISABLING CURIOSITY");
                        a.curiosity.set(0f); //disable curiosity
                    }
                } else {

                    if (lag > 1) { //skip the step after a new target has been selected which can make it seem worse

                        float worsening = lastReward - a.reward;
                        if (step > trainingRounds && worsening > worsenThreshold) {
                            //print tasks suspected of faulty logic
                            current.forEach(x -> {
                                System.err.println(worsening + "\t" + x.proof());
                            });
                        }
                    }

                    lag++;

                }

                lastReward = a.reward;

                current.clear();
            });
        }

    }

}


//    private static class InteractiveFirer extends FireConcepts.DirectConceptBagFocus {
//
//        private Premise premise;
//
//        public InteractiveFirer(NAR n) {
//            super(n, ((Default) n).newConceptBag(1024), ((Default) n).newPremiseBuilder());
//        }
//
//        final Set<Task> derived = new HashSet(1024);
//
//        @Override
//        protected synchronized void cycle() {
//
//            new PremiseMatrix(1, 1, new MutableIntRange(1,1)).accept(nar);
//
//            if (!derived.isEmpty()) {
//                System.out.println(premise);
//
//                List<Task> l = new FasterList(derived);
//                l.sort((a, b)->{
//                   int x = Float.compare(b.budget().pri(), a.pri());
//                   if (x == 0)
//                       return 1;
//                   else
//                       return x;
//                });
//
//                derived.clear();
//
//                for (Task x : l) {
//                    System.out.println("\t" + x);
//                }
//                try {
//                    System.in.read();
//                } catch (IOException e) {
//
//                }
//            }
//        }
//
//
//
//        @Override
//        public void accept(DerivedTask derivedTask) {
//            //nar.input(derivedTask);
//            premise = derivedTask.premise;
//            derived.add(derivedTask);
//        }
//    }
