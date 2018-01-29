package nars.perf;

import jcog.optimize.Optimize;
import jcog.optimize.Result;
import nars.nal.nal1.NAL1Test;
import nars.util.NALTest;

public class NARTestOptimize {

    /*

//            Class mainClass = classloader.loadClass(entryClass);
//
//            System.out.println(Arrays.toString(mainClass.getMethods()));
//
//            Method main = mainClass.getMethod("main2", String[].class);
//
//            main.invoke(null, new Object[]{ArrayUtils.EMPTY_STRING_ARRAY});
     */

    public static void main(String[] args) {

        System.setProperty("junit.jupiter.extensions.autodetection.enabled", "true");


        Result r = new Optimize<NALTest>(()-> new NAL1Test())
//                .tweak(128, 128, 1, "nars.Param", "TTL_MUTATE")
//                .tweak(1, 2, 1, "nars.Param", "TTL_DERIVE_TASK_SUCCESS")


        .tweak("ttlFactor", 4, 64, (float x, NALTest t) -> {

            t.test.nar.matchTTLmean.set(x);

        }).tweak("termVolumeMax", 8, 32, (float x, NALTest t) -> {

            t.test.nar.termVolumeMax.set(x);

        }).run(16, (t)->{
            t.test.run(100, true);
            //t.test.test();
            return t.test.score;
        });

                //.run(16, new MyNAL1MultistepTest());

        r.print();


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

