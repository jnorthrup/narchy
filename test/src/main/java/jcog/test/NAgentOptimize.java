package jcog.test;

import jcog.lab.Lab;
import jcog.lab.Optilive;
import jcog.test.control.BooleanReactionTest;
import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.agent.NAgent;
import nars.derive.Deriver;
import nars.derive.budget.DefaultDeriverBudgeting;
import nars.derive.impl.BatchDeriver;

import java.util.function.Function;

public class NAgentOptimize {
    public static void main(String[] args) {
        int period = 2;
        new NAgentOptimize(n -> new BooleanReactionTest(n, ()->n.time() % period < period/2, (i,o) -> i==o),
                64, 1);
    }

    public NAgentOptimize(Function<NAR,NAgent> agent, int experimentCycles, int repeats) {


        Lab<NAR> l = new Lab<>(() -> {
            NAR n = NARS.tmp();
            n.random().setSeed(System.nanoTime());
            return n;
        });

        l.hints.put("autoInc", 3);
        l
//                .var("attnCapacity", 4, 128, 8,
//                        (NAR n, int i) -> n.attn.active.setCapacity(i))

//                .var("ttlMax", 6, 20, 3,
//                        (NAR n, int i) -> n.deriveBranchTTL.set(i))
//                .var("linkFanOut", 1, 16, 1,
//                        (NAR n, int f) -> Param.LinkFanoutMax = f)
                .var("activation", 0, 1f, 0.1f, (NAR n, float f) -> n.conceptActivation.set(f))
                .var("memoryDuration", 0, 8f, 0.5f,
                        (NAR n, float f) -> n.memoryDuration.set(f))
                .var("beliefPriDefault", 0, 1f, 0.1f,
                        (NAR n, float f) -> n.beliefPriDefault.set(f))
                .var("questionPriDefault", 0, 1f, 0.1f,
                        (NAR n, float f) -> {
                            n.questionPriDefault.set(f);
                            n.questPriDefault.set(f);
                        })
                .var("goalPriDefault", 0, 1f, 0.1f,
                        (NAR n, float f) -> n.goalPriDefault.set(f))

                .var("derivationComplexityExponent", 1f, 3f, 0.5f,
                        (NAR n, float f) -> Deriver.derivers(n).forEach(x ->
                                ((DefaultDeriverBudgeting)(((BatchDeriver)x).budgeting)).
                                        relGrowthExponent.set(f)))
//                .var("derivationScale", 0.5f, 2f, 0.1f,
//                        (NAR n, float f) -> Deriver.derivers(n).forEach(x ->
//                                ((DefaultDeriverBudgeting)(((BatchDeriver)x).budgeting)).
//                                        scale.set(f)))

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



        Optilive<NAR, NAgent> o = l.optilive(n->agent.apply(n.get()),
                jcog.lab.Optimize.repeat((NAgent t) -> {
                    final float[] rewardSum = {0};
                    t.onFrame(()-> rewardSum[0] += t.reward());
                    try {
                        t.nar().run(experimentCycles);
                    } catch (Throwable ee) {
                        if (Param.DEBUG)
                            ee.printStackTrace();
                        return Float.NEGATIVE_INFINITY;
                    }
                    return rewardSum[0];
                }, repeats)
        );
////            o.sense("numConcepts",
////                (TestNARSuite t) -> t.sum((NAR n) -> n.concepts.size()))
//                    .sense("derivedTask",
//                            (TestNARSuite t) -> t.sum((NAR n)->n.emotion.deriveTask.getValue()))

        o.start();


    }
}
