package nars.perf;

import jcog.optimize.AutoTweaks;
import jcog.optimize.Result;
import nars.NAR;
import nars.NARLoop;
import nars.NARS;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal5.NAL5Test;
import nars.nal.nal6.NAL6Test;
import nars.nal.nal7.NAL7Test;
import nars.nal.nal8.NAL8Test;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class NARTestOptimize {

    /** HACK runs all Junit test methods, summing the scores.
     * TODO use proper JUnit5 test runner api but it is a mess to figure out right now */
    static float tests(Supplier<NAR>s, Class<? extends NALTest>... c) {



        return (float) Stream.of(c).flatMap(cc -> Stream.of(cc.getMethods())).filter(x -> x.getAnnotation(Test.class)!=null).parallel().mapToDouble(m -> {
            try {
                NALTest t = (NALTest) m.getDeclaringClass().getConstructor().newInstance();
                t.nar = s.get(); //overwrite NAR with the supplier
                m.invoke(t);
                try {
                    t.test.test(false);
                    return t.test.score;
                    //return 1 + t.test.score; //+1 for successful completion
                } catch (Throwable ee) {
                    return -1f;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0f;
            }

        }).sum();
    }

    public static void main(String[] args) {


        Result<NAR> r = new AutoTweaks<>(()->{
            NAR n = NARS.tmp();
            return n;
        })
            .exclude(NARLoop.class)
            .optimize(128, (n)->{
                return tests(n,
                        NAL1Test.class,
                        NAL2Test.class,
                        NAL3Test.class,
                        NAL5Test.class,
                        NAL6Test.class,
                        NAL7Test.class,
                        NAL8Test.class);
            });

        r.print();
        r.tree(2, 8).print();


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

