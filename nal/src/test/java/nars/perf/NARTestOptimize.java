package nars.perf;

import jcog.optimize.AutoTweaks;
import jcog.optimize.Result;
import nars.NAR;
import nars.NARS;
import nars.nal.nal6.NAL6Test;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class NARTestOptimize {

    /** HACK runs all Junit test methods, summing the scores.
     * TODO use proper JUnit5 test runner api but it is a mess to figure out right now */
    static float tests(Supplier<NAR>s, Class<? extends NALTest> c) {

        int cycles = 200;

        return (float) Stream.of(c.getMethods()).filter(x -> x.getAnnotation(Test.class)!=null).parallel().mapToDouble(m -> {
            NALTest t = null;
            try {
                t = c.getConstructor(Supplier.class).newInstance(s);
                m.invoke(t);
                try {
                    t.test.run(cycles, false);
                    return t.test.score;
                } catch (Throwable ee) {
                    return 0f;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0f;
            }

        }).sum();
    }

    public static void main(String[] args) {


        Result<NAR> r = new AutoTweaks<>(()-> NARS.tmp()) {

//            @Override
//            protected boolean includeField(Field f) {
//                return includeFields.contains(f.getName());
//            }
        }
        .optimize(24, (n)->{

            return tests(n, NAL6Test.class);
//            NAL6Test t = new NAL6Test() {
//                @Override
//                protected NAR nar() {
//                    return n.get();
//                }
//            };
//
//            try {
//
//                t.variable_elimination5();
//                t.test.run(t.cycles, false);
//
//                return t.test.score;
//            } catch (Throwable e) {
//                return 0;
//            }

        });

        r.print();
        r.tree(3, 8).print();


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

