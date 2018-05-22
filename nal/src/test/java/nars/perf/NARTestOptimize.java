package nars.perf;

import jcog.optimize.Optimizing;
import jcog.optimize.Result;
import jcog.optimize.Tweaks;
import nars.NAR;
import nars.NARLoop;
import nars.NARS;
import nars.control.MetaGoal;
import nars.nal.nal1.NAL1MultistepTest;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal5.NAL5Test;
import nars.nal.nal6.NAL6Test;
import nars.nal.nal7.NAL7Test;
import nars.nal.nal8.NAL8Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NARTestOptimize {

    static final int threads =
            4;
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




        Optimizing<NAR> opt = new Tweaks<>(() -> {
            NAR n = NARS.tmp();
            return n;
        }).discover(new Tweaks.DiscoveryFilter() {

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
                return
                        !Modifier.isStatic(f.getModifiers()) &&
                                !excludeFields.contains(f.getName());
            }

        })
                .tweak("PERCEIVE", -1f, +1f, 0.25f, (NAR n, float p) ->
                        n.emotion.want(MetaGoal.Perceive, p)
                )
                .tweak("BELIEVE", -1f, +1f, 0.25f, (NAR n, float p) ->
                        n.emotion.want(MetaGoal.Believe, p)
                )
                .optimize((n) ->
                        JUnitNAR.tests(n,
                                NAL1Test.class,
                                NAL1MultistepTest.class,
                                NAL2Test.class,
                                NAL3Test.class,
                                NAL5Test.class,
                                NAL6Test.class,
                                NAL7Test.class,
                                NAL8Test.class
                        ));

        opt.saveOnShutdown("/tmp/" + NARTestOptimize.class.getName() + "_" + System.currentTimeMillis() + ".arff");

        //TODO ensure threads are unique
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        while (true) {
            Result<NAR> r = opt.run( /*32*1024*/ 10, 10, pool);
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

