package nars.perf;

import jcog.optimize.AutoTweaks;
import jcog.optimize.Result;
import nars.NAR;
import nars.NARS;
import nars.nal.nal6.NAL6Test;

public class NARTestOptimize {

    public static void main(String[] args) {


        Result<NAR> r = new AutoTweaks<>(()-> NARS.tmp()) {

//            @Override
//            protected boolean includeField(Field f) {
//                return includeFields.contains(f.getName());
//            }
        }
        .optimize(16, (n)->{

            NAL6Test t = new NAL6Test() {
                @Override
                protected NAR nar() {
                    return n;
                }
            };

            try {

                t.variable_elimination5();
                t.test.run(t.cycles, true);

                return t.test.score;
            } catch (Throwable e) {
                return 0;
            }

        });

        r.print();
        r.tree(4, 4).print();


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

