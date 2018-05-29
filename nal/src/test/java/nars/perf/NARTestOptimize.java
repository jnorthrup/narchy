package nars.perf;

import jcog.Util;
import jcog.optimize.Optimizing;
import jcog.optimize.Tweaks;
import nars.NAR;
import nars.NARLoop;
import nars.NARS;
import nars.nal.nal1.NAL1MultistepTest;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal5.NAL5Test;
import nars.nal.nal6.NAL6Test;
import nars.nal.nal7.NAL7Test;
import nars.nal.nal8.NAL8Test;
import nars.util.NALTest;
import org.intelligentjava.machinelearning.decisiontree.DecisionTree;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static nars.perf.JUnitNAR.randomTest;
import static nars.perf.JUnitNAR.test;

public class NARTestOptimize {

    static final int threads =
            Util.concurrencyDefault(1);
            //Math.max(1,Runtime.getRuntime().availableProcessors()-1);
            //4;


    public static void main(String[] args) {

//        try {
//            PrintStream out = System.out;
//            OutputStream fout = new FileOutputStream(new File("/tmp/" + NARTestOptimize.class.getSimpleName() + ".csv"));
//            System.setOut(new PrintStream(new MultiOutputStream(out, fout)));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }




        Optimizing<NAR,NALTest> opt = new Tweaks<>(NARS::tmp).discover(new Tweaks.DiscoveryFilter() {

            final Set<Class> excludeClasses = Set.of(NARLoop.class);
            final Set<String> excludeFields = Set.of(
                    "DEBUG",
                    "dtMergeOrChoose",
                    "TEMPORAL_SOLVER_ITERATIONS",
                    "dtDither",
                    "timeFocus",
                    "beliefConfDefault",
                    "goalConfDefault"
            );

            @Override
            protected boolean includeClass(Class<?> targetType) {
                return !excludeClasses.contains(targetType);
            }

            @Override
            protected boolean includeField(Field f) {
//                return
//                        !Modifier.isStatic(f.getModifiers()) &&
//                                !excludeFields.contains(f.getName());
                return f.getName().equals("activateConceptRate");
            }

        })
//                .tweak("PERCEIVE", -1f, +1f, 0.25f, (NAR n, float p) ->
//                        n.emotion.want(MetaGoal.Perceive, p)
//                )
//                .tweak("BELIEVE", -1f, +1f, 0.25f, (NAR n, float p) ->
//                        n.emotion.want(MetaGoal.Believe, p)
//                )
                .optimize((Function<NAR,NALTest>)(s-> test(s, randomTest(
                        NAL1Test.class,
                        NAL1MultistepTest.class,
                        NAL2Test.class,
                        NAL3Test.class,
                        NAL5Test.class,
                        NAL6Test.class,
                        NAL7Test.class,
                        NAL8Test.class
                ))), new Optimizing.Score<>("completeFast", 1f, t->
                    t!=null ? t.test.score : 0
                ), new Optimizing.Score<>("deriveUniquely", 0.25f, t ->
                {
                    if (t == null)
                        return 0;

                    float dups = t.test.nar.emotion.deriveFailDerivationDuplicate.getValue().floatValue()
                            +
                            t.test.nar.emotion.deriveFailParentDuplicate.getValue().floatValue();
                    float derives = t.test.nar.emotion.deriveTask.getValue().floatValue();

                    if (derives+dups < Float.MIN_NORMAL)
                        return 0;

                    return derives/(derives + dups);
                }
                ));

        opt.saveOnShutdown("/tmp/" + NARTestOptimize.class.getName() + "_" + System.currentTimeMillis() + ".arff");

        //TODO ensure threads are unique
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        while (true) {
            Optimizing.Result r = opt.run( /*32*1024*/ 32, 32, pool);

            System.out.println();
            System.out.println();
            System.out.println();

//            r.cull(0f, 0.7f);

            for (DecisionTree d : r.forest(4, 3))
                d.print();

            r.tree(3, 10).print();

        }


    }

}


//    public static void main(String[] args) {
//
//        System.setProperty("junit.jupiter.extensions.autodetection.enabled", "true");
//
//
//        Optimize.Result r = new Optimize<NALTest>(() -> {
//
//            return new NAL1MultistepTest();
//
//        }).tweak("ttlFactor", 4, 64, (float x, NALTest t) -> {
//
//            t.test.nar.matchTTLmin.set(x);
//            t.test.nar.matchTTLmax.set(x*2);
//
//        }).tweak("termVolumeMax", 8, 32, (float x, NALTest t) -> {
//
//            t.test.nar.termVolumeMax.set(x);
//
//        }).run(129, (n) -> {
//
//
//            try {
//                //((NAL1Test)n).backwardInference();
//                //((NAL1Test) n).abduction();
//
//                ((NAL1MultistepTest)n).multistepSim4();
//                n.end(null);
//                return 1f / (1 + n.test.time());
//
//            } catch (Throwable e) {
//                e.printStackTrace();
//                return Float.NEGATIVE_INFINITY;
//            }
//
//
//        });
//
//        r.print();
//
//
//    }

