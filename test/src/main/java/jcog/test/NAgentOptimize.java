package jcog.test;

import jcog.lab.Lab;
import jcog.lab.Optilive;
import jcog.test.control.BooleanReactionTest;
import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.agent.NAgent;

import java.util.function.Function;

public class NAgentOptimize {
    public static void main(String[] args) {

        new NAgentOptimize(n ->
                //new BooleanChoiceTest(n, (prev,next)->next==true)
                new BooleanReactionTest(n,
                        ()-> {
                            int period = 8;
                            return n.time() % period < period / 2;
                        },
                        (i, o) -> i==o
                )

                //..new TrackXY(5,5)

        ,96, 4);
    }

    public NAgentOptimize(Function<NAR,NAgent> agent, int experimentCycles, int repeats) {


        Lab<NAR> l = new Lab<>(() -> {
            NAR n = NARS.tmp();
            n.random().setSeed(System.nanoTime());

            /* defaults TODO "learn" these from the experiments and reapply them in future experiments */
            n.termVolumeMax.set(4);
            n.goalPriDefault.set(0.6f);

            return n;
        });

        l.hints.put("autoInc", 3);
        l
//                .var("attnCapacity", 4, 128, 8,
//                        (NAR n, int i) -> n.attn.active.setCapacity(i))
                .var("termVolMax", 3, 16, 2,
                        (NAR n, int i) -> n.termVolumeMax.set(i))
//                .var("ttlMax", 6, 20, 3,
//                        (NAR n, int i) -> n.deriveBranchTTL.set(i))
//                .var("linkFanOut", 1, 16, 1,
//                        (NAR n, int f) -> Param.LinkFanoutMax = f)
//                .var("conceptActivation", 0, 1f, 0.1f, (NAR n, float f) -> n.conceptActivation.set(f))
                .var("taskLinkActivation", 0, 1f, 0.1f, (NAR n, float f) -> n.taskLinkActivation.set(f))
//                .var("memoryDuration", 0, 8f, 0.25f,
//                        (NAR n, float f) -> n.memoryDuration.set(f))
//                .var("beliefPriDefault", 0, 1f, 0.1f,
//                        (NAR n, float f) -> n.beliefPriDefault.set(f))
                .var("questionPriDefault", 0, 1f, 0.1f,
                        (NAR n, float f) -> {
                            n.questionPriDefault.set(f);
                            n.questPriDefault.set(f);
                        })
                .var("goalPriDefault", 0, 1f, 0.1f,
                        (NAR n, float f) -> n.goalPriDefault.set(f))

//                .var("derivationComplexityExponent", 1f, 3f, 0.5f,
//                        (NAR n, float f) -> Deriver.derivers(n).forEach(x ->
//                                ((DefaultDeriverBudgeting)(((BatchDeriver)x).budgeting)).
//                                        relGrowthExponent.set(f)))
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
                    final double[] rewardSum = {0}, dexSum = { 0 };
                    t.onFrame(()-> {
                        rewardSum[0] += t.reward();
                        dexSum[0] += t.dexterity();
                    });
                    try {
                        t.nar().run(experimentCycles);
                    } catch (Throwable ee) {
                        if (Param.DEBUG)
                            ee.printStackTrace();
                        return Float.NEGATIVE_INFINITY;
                    }
                    long time = t.nar().time();
                    double frames = ((double)time) / t.nar().dur();
                    double rewardMean = rewardSum[0]/frames;
                    double dexMean= dexSum[0]/frames;
                    //return rewardSum[0];
                    return (float) ((1 + rewardMean) * (1 + dexMean));
                    //return rewardSum[0] * (1 + dexSum[0]);
                }, repeats)
        );
////            o.sense("numConcepts",
////                (TestNARSuite t) -> t.sum((NAR n) -> n.concepts.size()))
//                    .sense("derivedTask",
//                            (TestNARSuite t) -> t.sum((NAR n)->n.emotion.deriveTask.getValue()))

        o.start();


    }
}
