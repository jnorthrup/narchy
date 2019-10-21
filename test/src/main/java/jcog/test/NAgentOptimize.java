package jcog.test;

import jcog.lab.Lab;
import jcog.lab.Optilive;
import jcog.test.control.BooleanReactionTest;
import nars.NAL;
import nars.NAR;
import nars.NARS;
import nars.game.Game;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class NAgentOptimize {
    public static void main(String[] args) {

        new NAgentOptimize(new Function<NAR, Game>() {
            @Override
            public Game apply(NAR n) {
                return new BooleanReactionTest(n,
                        new BooleanSupplier() {
                            @Override
                            public boolean getAsBoolean() {
                                int period = 8;
                                return n.time() % (long) period < (long) (period / 2);
                            }
                        },
                        new BooleanBooleanPredicate() {
                            @Override
                            public boolean accept(boolean i, boolean o) {
                                return i == o;
                            }
                        }
                );
            }
        }

                //..new TrackXY(5,5)

        ,96, 4);
    }

    public NAgentOptimize(Function<NAR, Game> agent, int experimentCycles, int repeats) {


        Lab<NAR> l = new Lab<NAR>(new Supplier<NAR>() {
            @Override
            public NAR get() {
                NAR n = NARS.tmp();
                n.random();

                /* defaults TODO "learn" these from the experiments and reapply them in future experiments */
                n.termVolMax.set(4);
                n.goalPriDefault.pri(0.6f);

                return n;
            }
        });

        l.hints.put("autoInc", 3);
        l
//                .var("attnCapacity", 4, 128, 8,
//                        (NAR n, int i) -> n.attn.active.setCapacity(i))
                .var("termVolMax", 3, 16, 2,
                        new ObjectIntProcedure<NAR>() {
                            @Override
                            public void value(NAR n, int i) {
                                n.termVolMax.set(i);
                            }
                        })
//                .var("ttlMax", 6, 20, 3,
//                        (NAR n, int i) -> n.deriveBranchTTL.setAt(i))
//                .var("linkFanOut", 1, 16, 1,
//                        (NAR n, int f) -> Param.LinkFanoutMax = f)
//                .var("conceptActivation", 0, 1f, 0.1f, (NAR n, float f) -> n.conceptActivation.setAt(f))
//                .var("taskLinkActivation", 0, 1f, 0.1f, (NAR n, float f) -> n.taskLinkActivation.setAt(f))
//                .var("memoryDuration", 0, 8f, 0.25f,
//                        (NAR n, float f) -> n.memoryDuration.setAt(f))
//                .var("beliefPriDefault", 0, 1f, 0.1f,
//                        (NAR n, float f) -> n.beliefPriDefault.setAt(f))
                .var("questionPriDefault", (float) 0, 1f, 0.1f,
                        new ObjectFloatProcedure<NAR>() {
                            @Override
                            public void value(NAR n, float f) {
                                n.questionPriDefault.pri(f);
                                n.questPriDefault.pri(f);
                            }
                        })
                .var("goalPriDefault", (float) 0, 1f, 0.1f,
                        new ObjectFloatProcedure<NAR>() {
                            @Override
                            public void value(NAR n, float f) {
                                n.goalPriDefault.pri(f);
                            }
                        })

//                .var("derivationComplexityExponent", 1f, 3f, 0.5f,
//                        (NAR n, float f) -> Deriver.derivers(n).forEach(x ->
//                                ((DefaultDeriverBudgeting)(((BatchDeriver)x).budgeting)).
//                                        relGrowthExponent.setAt(f)))
//                .var("derivationScale", 0.5f, 2f, 0.1f,
//                        (NAR n, float f) -> Deriver.derivers(n).forEach(x ->
//                                ((DefaultDeriverBudgeting)(((BatchDeriver)x).budgeting)).
//                                        scale.setAt(f)))

//        l.varAuto(new Lab.DiscoveryFilter() {
//
//            private Set<String> excludedFieldNames = Set.of("causeCapacity", "STRONG_COMPOSITION", "want");
//
//            @Override
//            protected boolean includeField(Field f) {
//                String n = f.getName();
//                if (n.startsWith("DEBUG") || n.contains("throttle") || excludedFieldNames.contains(n))
//                    return false;
//
//                return super.includeField(f);
//            }
//        })
        ;


        Optilive<NAR, Game> o = l.optilive(new Function<Supplier<NAR>, Game>() {
                                               @Override
                                               public Game apply(Supplier<NAR> n) {
                                                   return agent.apply(n.get());
                                               }
                                           },
                jcog.lab.Optimize.repeat(new FloatFunction<Game>() {
                    @Override
                    public float floatValueOf(Game t) {
                        double[] rewardSum = {(double) 0}, dexSum = {(double) 0};
                        t.onFrame(new Runnable() {
                            @Override
                            public void run() {
                                rewardSum[0] += t.happiness();
                                dexSum[0] += t.dexterity();
                            }
                        });
                        try {
                            t.nar().run(experimentCycles);
                        } catch (Throwable ee) {
                            if (NAL.DEBUG)
                                ee.printStackTrace();
                            return Float.NEGATIVE_INFINITY;
                        }
                        long time = t.nar().time();
                        double frames = ((double) time) / (double) t.nar().dur();
                        double rewardMean = rewardSum[0] / frames;
                        double dexMean = dexSum[0] / frames;
                        //return rewardSum[0];
                        return (float) ((1.0 + rewardMean) * (1.0 + dexMean));
                        //return rewardSum[0] * (1 + dexSum[0]);
                    }
                }, repeats)
        );
////            o.sense("numConcepts",
////                (TestNARSuite t) -> t.sum((NAR n) -> n.concepts.size()))
//                    .sense("derivedTask",
//                            (TestNARSuite t) -> t.sum((NAR n)->n.emotion.deriveTask.getValue()))

        o.start();


    }
}
