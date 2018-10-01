package nars.perf;

import jcog.lab.Lab;
import jcog.lab.util.Opti;
import jcog.lab.util.Optimization;
import nars.NAR;
import nars.NARS;
import nars.nal.nal1.NAL1MultistepTest;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal5.NAL5Test;
import nars.test.TestNARSuite;
import nars.test.impl.DeductiveMeshTest;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;

import java.util.function.Supplier;

class NARTestOptimize {

    static class NAL1Optimize {
        public static void main(String[] args) {

            boolean parallel = true;
            Class[] testClasses = new Class[] {
                    NAL1Test.class, NAL1MultistepTest.class, NAL2Test.class, NAL3Test.class,//
//                    NAL4Test.class, NAL4MultistepTest.class,
                    NAL5Test.class,
//                    NAL6Test.class,
//                    NAL7Test.class, NAL8Test.class,
            };

            Lab<NAR> l = new Lab<>(() -> {
                NAR n = NARS.tmp();
                n.random().setSeed(System.nanoTime());
                return n;
            })
//                .var("ttlMax", 6, 20, 3,
//                        (NAR n, int i) -> n.deriveBranchTTL.set(i))
                .var("termlinkBalance", 0, 1f, 0.1f,
                        (NAR n, float f) -> n.termlinkBalance.set(f))
                .var("activationRate", 0, 1f, 0.1f,
                            (NAR n, float f) -> n.activateConceptRate.set(f))
                .var("forgetRate", 0, 1f, 0.1f,
                        (NAR n, float f) -> n.forgetRate.set(f))
                .var("beliefPriDefault", 0, 1f, 0.1f,
                        (NAR n, float f) -> n.beliefPriDefault.set(f))
                .var("questionPriDefault", 0, 1f, 0.1f,
                        (NAR n, float f) -> {
                            n.questionPriDefault.set(f);
                            n.questPriDefault.set(f);
                        })
                .var("goalPriDefault", 0, 1f, 0.1f,
                        (NAR n, float f) -> n.goalPriDefault.set(f))

//                .var("derivationComplexityExponent", 1f, 2.5f, 0.5f,
//                        (NAR n, float f) -> Deriver.derivers(n).forEach(x ->
//                                ((DefaultDeriverBudgeting)(((MatrixDeriver)x).budgeting)).
//                                        relGrowthExponent.set(f)))
//                .var("derivationScale", 0.5f, 2f, 0.1f,
//                        (NAR n, float f) -> Deriver.derivers(n).forEach(x ->
//                                ((DefaultDeriverBudgeting)(((MatrixDeriver)x).budgeting)).
//                                        scale.set(f)))
            ;


            int suiteIterations = 2;
            int samples = 128;
            Optimization<NAR, TestNARSuite> o = l.optimize((Supplier<NAR> s) -> {
                TestNARSuite t = new TestNARSuite(s, testClasses);
                t.run(parallel, suiteIterations);
                return t;
            },
                (TestNARSuite t) -> (float) t.score()
            , samples);

            o
//            .sense("numConcepts",
//                (TestNARSuite t) -> t.sum((NAR n) -> n.concepts.size()))
            .sense("derivedTask",
                (TestNARSuite t) -> t.sum((NAR n)->n.emotion.deriveTask.getValue()))
            .sense("duplicateTask",
                    (TestNARSuite t) -> t.sum((NAR n)->
                            n.emotion.deriveFailParentDuplicate.getValue()) +
                            t.sum((NAR n) -> n.emotion.deriveFailDerivationDuplicate.getValue())
            );

            o.runSync().print();

            RealDecisionTree t = o.tree(4, 8);
            t.print();
            t.printExplanations();


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
            }).var("forgetRate", 0, 1f, 0.2f, (DeductiveMeshTest t, float f) -> {
                t.test.nar.forgetRate.set(f);
            });


            Opti<DeductiveMeshTest> o = l.optimize(d -> {
                d.test.test();
            }, d -> d.test.score, 16);
            o.run();
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












































