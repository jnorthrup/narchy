package nars.perf;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.optimize.Result;
import jcog.optimize.Tweaks;
import nars.NAR;
import nars.NARLoop;
import nars.NARS;
import nars.Param;
import nars.control.MetaGoal;
import nars.nal.nal1.NAL1MultistepTest;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal5.NAL5Test;
import nars.nal.nal6.NAL6Test;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class NARTestOptimize {

    static final int threads =
            //Math.max(1,Runtime.getRuntime().availableProcessors()-1);
            3;

    /** necessary to do what jdk "parallel" streams refuses to do... WTF */
    static final ExecutorService exe = Executors.newFixedThreadPool(
            threads);

    /** HACK runs all Junit test methods, summing the scores.
     * TODO use proper JUnit5 test runner api but it is a mess to figure out right now */
    static float tests(Supplier<NAR>s, Class<? extends NALTest>... c) {


        List<Method> methods = Stream.of(c)
                .flatMap(cc -> Stream.of(cc.getMethods()).filter(x -> x.getAnnotation(Test.class) != null))
                .collect(toList());

        final CountDownLatch remain = new CountDownLatch(methods.size());
        final AtomicDouble sum = new AtomicDouble(0);
        methods.forEach(m -> {
            exe.submit(()->{
                try {
                    sum.addAndGet(test(s, m));
                } finally {
                    remain.countDown();
                }
            });
        });
        try {
            remain.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return sum.floatValue();
    }

    private static float test(Supplier<NAR> s, Method m) {
        try {
            NALTest t = (NALTest) m.getDeclaringClass().getConstructor().newInstance();
            t.nar = s.get(); //overwrite NAR with the supplier
            t.nar.random().setSeed(System.nanoTime());
            m.invoke(t);
            try {
                Param.DEBUG = false;
                t.test.test(false);
                return t.test.score;
                //return 1 + t.test.score; //+1 for successful completion
            } catch (Throwable ee) {
                return -2f;
                //return 0f;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0f;
        }
    }

    public static void main(String[] args) {


        while (true) {
            Result<NAR> r = new Tweaks<>(() -> {
                NAR n = NARS.tmp();
                return n;
            })
                .learn(Set.of(NARLoop.class))
                .tweak("PERCEIVE", -1f, +1f, 0.1f, (NAR n, float p) ->
                        n.emotion.want(MetaGoal.Perceive, p)
                )
                .tweak("BELIEVE", -1f, +1f, 0.1f, (NAR n, float p) ->
                        n.emotion.want(MetaGoal.Believe, p)
                )
                .optimize(1024, 2, (n) ->
                        tests(n,
                                NAL1Test.class,
                                NAL1MultistepTest.class,
                                NAL2Test.class,
                                NAL3Test.class,
                                NAL5Test.class,
                                NAL6Test.class

                                //NAL7Test.class,
                                //NAL8Test.class
                        ));

            r.print();
            r.tree(2, 8).print();
            System.out.println();
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

