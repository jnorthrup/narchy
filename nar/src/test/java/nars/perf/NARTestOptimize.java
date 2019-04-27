package nars.perf;

import jcog.lab.DefaultScientist;
import jcog.lab.Lab;
import jcog.lab.Opti;
import jcog.lab.Optilive;
import nars.NAL;
import nars.NAR;
import nars.NARS;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal5.NAL5Test;
import nars.nal.nal7.NAL7Test;
import nars.test.TestNARSuite;
import nars.test.impl.DeductiveMeshTest;

import java.io.File;
import java.util.function.Supplier;

class NARTestOptimize {

    static {
        //System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "3"); //temporary
    }

    static class NAL1Optimize {
        public static void main(String[] args) {

            boolean parallel = true;
            Class[] testClasses = new Class[] {
                    NAL1Test.class,
//                    NAL2Test.class,
                    NAL3Test.class,
//                    NAL4Test.class,
////                    NAL1MultistepTest.class,
//                    //NAL4MultistepTest.class,
                    NAL5Test.class,
//                    NAL6Test.class,
                    NAL7Test.class,
//                    NAL8Test.class,
            };

            Lab<NAL<NAL<NAR>>> l = new Lab<>(() -> {
                NAL<NAL<NAR>> n = NARS.tmp();
                n.random();
                return n;
            })
//                .var("attnCapacity", 4, 128, 8,
//                        (NAR n, int i) -> n.attn.links.setCapacity(i))

                .var("ttlMax", 1 * NAL.derive.TTL_MIN, 8 * NAL.derive.TTL_MIN, 3,
                        (NAL<NAL<NAR>> n, int i) -> n.deriveBranchTTL.set(i))
//                .var("linkFanOut", 1, 16, 1,
//                        (NAR n, int f) -> Param.LinkFanoutMax = f)
//                .var("conceptActivation", ScalarValue.EPSILONsqrt, 1f, 0.1f,
//                        (NAR n, float f) -> n.attn.activationRate.set(f))
//                .var("linkActivation", 0, 1f, 0.1f,
//                        (NAR n, float f) -> n.taskLinkActivation.set(f))
//                .var("forgetRate", ScalarValue.EPSILONsqrt, 1f, 0.1f,
//                        (NAR n, float f) -> ((AbstractConceptIndex)n.concepts).forgetRate.set(f))
//                .var("linkForgetRate", ScalarValue.EPSILONsqrt, 1f, 0.1f,
//                        (NAR n, float f) -> ((Forgetting.AsyncForgetting)(n.attn.forgetting)).tasklinkForgetRate.set(f))
//
//                .var("beliefPriDefault", ScalarValue.EPSILONsqrt, 1f, 0.1f,
//                        (NAR n, float f) -> n.beliefPriDefault.set(f))
//                .var("questionPriDefault", ScalarValue.EPSILONsqrt, 1f, 0.1f,
//                        (NAR n, float f) -> {
//                            n.questionPriDefault.set(f);
//                            n.questPriDefault.set(f);
//                        })
//                .var("goalPriDefault", 0, 1f, 0.1f,
//                        (NAR n, float f) -> n.goalPriDefault.set(f))

//                .var("derivationComplexityExponent", 1f, 3f, 0.5f,
//                        (NAR n, float f) -> Deriver.derivers(n).forEach(x ->
//                                ((DefaultDerivePri)(n.attn.deriving)).
//                                        relGrowthExponent.set(f)))
//                .var("derivationScale", 0.5f, 2f, 0.1f,
//                        (NAR n, float f) -> Deriver.derivers(n).forEach(x ->
//                                ((DefaultDeriverBudgeting)(((BatchDeriver)x).budgeting)).
//                                        scale.set(f)))
            ;


            int suiteIterations = 2;
            Optilive<NAL<NAL<NAR>>, TestNARSuite> o = l.optilive((Supplier<NAL<NAL<NAR>>> s) ->
                            new TestNARSuite(s, testClasses).run(parallel, suiteIterations),
                (TestNARSuite t) -> (float) t.score());

//            o
////            .sense("numConcepts",
////                (TestNARSuite t) -> t.sum((NAR n) -> n.concepts.size()))
//            .sense("derivedTask",
//                (TestNARSuite t) -> t.sum((NAR n)->n.emotion.deriveTask.getValue()))
//            .sense("duplicateTask",
//                    (TestNARSuite t) -> t.sum((NAR n)->
//                            n.emotion.deriveFailParentDuplicate.getValue()) +
//                            t.sum((NAR n) -> n.emotion.deriveFailDerivationDuplicate.getValue())
//            );

            o.start(new DefaultScientist(), new File("/home/me/naropt"));

//            RealDecisionTree t = o.tree(4, 8);
//            if (t!=null) {
//                t.print();
//                t.printExplanations();
//            }


        }
    }

    static class DeductiveMeshOptimize {
        public static void main(String[] args) {
            Lab<DeductiveMeshTest> l = new Lab<>(() ->
            {
                DeductiveMeshTest d = new DeductiveMeshTest(NARS.tmp(), new int[]{4, 3}, 2000);
                d.test.quiet = true;
                return d;
            }).var("ttlMax", 6, 100, 20, (DeductiveMeshTest t, int i) -> {
                t.test.nar.deriveBranchTTL.set(i);
            });


            Opti<DeductiveMeshTest> o = l.optimize(d -> {
                d.test.test();
            }, d -> d.test.score);
            o.run(64);
            o.print();
            o.tree(4, 6).print();
            System.out.println(o.best());
        }
    }

//    public static void _main(String[] args) {
//
//
//        Lab<NAR, NALTest> opt = new Variables<>(NARS::tmp).discover(new Variables.DiscoveryFilter() {
//
//            final Set<Class> excludeClasses = Set.of(NARLoop.class);
//            final Set<String> excludeFields = Set.of(
//                    "DEBUG",
//                    "dtMergeOrChoose",
//                    "TEMPORAL_SOLVER_ITERATIONS",
//                    "dtDither",
//                    "timeFocus",
//                    "beliefConfDefault",
//                    "goalConfDefault"
//            );
//
//            @Override
//            protected boolean includeClass(Class<?> targetType) {
//                return !excludeClasses.contains(targetType);
//            }
//
//            @Override
//            protected boolean includeField(Field f) {
//                return
//                        !Modifier.isStatic(f.getModifiers()) &&
//                                !excludeFields.contains(f.getName());
//
//
//            }
//
//        })
//                .tweak("PERCEIVE", -1f, +1f, 0.25f, (NAR n, float p) ->
//                        n.emotion.want(MetaGoal.Perceive, p)
//                )
//                .tweak("BELIEVE", -1f, +1f, 0.25f, (NAR n, float p) ->
//                        n.emotion.want(MetaGoal.Believe, p)
//                )
//                .optimize((Function<NAR, NALTest>) (s -> test(s, randomTest(
//                        NAL1Test.class,
//                        NAL1MultistepTest.class,
//                        NAL2Test.class,
//                        NAL3Test.class,
//                        NAL5Test.class,
//                        NAL6Test.class,
//                        NAL7Test.class,
//                        NAL8Test.class
//                ))), new Sensor.FloatSensor<>("completeFast", 1f, t ->
//                        t != null ? t.test.score : 0
//                ), new Sensor.FloatSensor<>("deriveUniquely", 0.25f, t ->
//                {
//                    if (t == null)
//                        return 0;
//
//                    float dups = t.test.nar.emotion.deriveFailDerivationDuplicate.getValue().floatValue()
//                            +
//                            t.test.nar.emotion.deriveFailParentDuplicate.getValue().floatValue();
//                    float derives = t.test.nar.emotion.deriveTask.getValue().floatValue();
//
//                    if (derives + dups < Float.MIN_NORMAL)
//                        return 0;
//
//                    return derives / (derives + dups);
//                }
//                ));
//
//        opt.saveOnShutdown("/tmp/" + NARTestOptimize.class.getName() + "_" + System.currentTimeMillis() + ".arff");
//
//
//        ExecutorService pool = Executors.newFixedThreadPool(threads);
//
//        while (true) {
//            Lab.Result r = opt.run( /*32*1024*/ 16, 32, pool);
//
//            System.out.println();
//            System.out.println();
//            System.out.println();
//
//
//            for (DecisionTree d: r.forest(4, 3))
//                d.print();
//
//            r.tree(3, 8).print();
//
//        }
//
//
//    }

}












































