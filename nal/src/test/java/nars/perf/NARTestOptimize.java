package nars.perf;

import jcog.Util;
import jcog.lab.Goal;
import jcog.lab.Lab;
import jcog.lab.util.Optimization;
import nars.NARS;
import nars.test.impl.DeductiveMeshTest;

public class NARTestOptimize {

    static final int threads =
            Util.concurrencyDefault(1);


//        l.discover();
//        l.vars.values().forEach(x -> System.out.println(x));

    public static void main(String[] args) {
        Lab<DeductiveMeshTest> l = new Lab<>(() ->
        {
            DeductiveMeshTest d = new DeductiveMeshTest(NARS.tmp(), new int[]{4, 3}, 2000);
            d.test.quiet = true;
            return d;
        });

        l.var()
            .add("ttlMax", 6, 100, 20, (DeductiveMeshTest t, int i) -> {
                t.test.nar.deriveTTL.set(i);
            })
            .add("forgetRate", 0, 1f, 0.2f, (DeductiveMeshTest t, float f) -> {
                t.test.nar.forgetRate.set(f);
            });


        Optimization<DeductiveMeshTest> o = l.optimize(d -> {
                    try {
                        d.test.test();
                    } catch (Throwable t) {

                    }
                },

                new Goal<>("testFast", d -> d.test.score)
                //new Goal<>("lessConcepts", d -> 1f/(1+d.test.nar.concepts.size()))
        );
        o.run();
        o.print();
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












































