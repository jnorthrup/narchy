package nars.truth;

import jcog.Util;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.random.XorShift128PlusRandom;
import nars.NAL;
import nars.truth.func.TruthFunctions;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.$.t;
import static nars.NAL.truth.TRUTH_EPSILON;
import static nars.truth.func.TruthFunctions.w2cSafe;
import static org.junit.jupiter.api.Assertions.*;


class TruthTest {

    @Test
    void testRounding() {
        int discrete = 10;
        float precision = 1f / discrete;

        for (float x = 0.19f; x < 0.31f; x += 0.01f / 2) {
            int i = Util.toInt(x, discrete);
            float y = Util.toFloat(i, discrete);
            System.out.println(x + " -> " + i + " -> " + y + ", d=" + (Math.abs(y - x)));
            assertTrue(
                    Util.equals(
                            x,
                            y,
                            precision / 2f)
            );
        }
    }

    @Test
    void testEternalize() {
        assertEquals(0.47f, w2cSafe(t(1f, 0.9f).eviEternalized()), 0.01f);
        assertEquals(0.31f, w2cSafe(t(1f, 0.45f).eviEternalized()), 0.01f);
        assertEquals(0.09f, w2cSafe(t(1f, 0.10f).eviEternalized()), 0.01f);
    }

    @Test
    void testPreciseTruthEquality() {

        float insignificant = NAL.truth.TRUTH_EPSILON / 5f;
        float significant = NAL.truth.TRUTH_EPSILON;

        assertEquals(t(0.5f, 0.5f),
                t(0.5f, 0.5f));
        assertEquals(t(0.5f, 0.5f),
                t(0.5f + insignificant, 0.50f));
        assertNotEquals(t(0.5f, 0.5f),
                t(0.5f + significant, 0.50f));

        assertEquals(t(0.5f, 0.5f),
                t(0.5f, 0.5f));
        assertEquals(t(0.5f, 0.5f),
                t(0.5f, 0.50f + insignificant));
        assertNotEquals(t(0.5f, 0.5f),
                t(0.5f, 0.50f + significant));

    }

    @Test
    void testDiscreteTruth_FreqEquality() {
        Truth a = new DiscreteTruth(1.0f, 0.9f);
        Truth aCopy = new DiscreteTruth(1.0f, 0.9f);
        assertEquals(a, aCopy);

        float ff = 1.0f - NAL.truth.TRUTH_EPSILON / 2.5f;
        assertTrue(1 == Util.round(ff, TRUTH_EPSILON));
        Truth aEqualWithinThresh = new DiscreteTruth(
                ff /* slightly less than half */,
                0.9f);
        assertEquals(a, aEqualWithinThresh);
        assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());

        Truth aNotWithinThresh = new DiscreteTruth(1.0f - NAL.truth.TRUTH_EPSILON * 1.0f, 0.9f);
        assertNotEquals(a, aNotWithinThresh);
        assertNotEquals(a.hashCode(), aNotWithinThresh.hashCode());

    }

    @Test
    void testConfEquality() {
        Truth a = new DiscreteTruth(1.0f, 0.5f);

        Truth aEqualWithinThresh = new DiscreteTruth(1.0f, 0.5f - NAL.truth.TRUTH_EPSILON / 2.1f /* slightly less than half the epsilon */);
        assertEquals(a, aEqualWithinThresh);
        assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());

        Truth aNotWithinThresh = new DiscreteTruth(1.0f, 0.5f - NAL.truth.TRUTH_EPSILON * 1.0f);
        assertNotEquals(a, aNotWithinThresh);
        assertNotEquals(a.hashCode(), aNotWithinThresh.hashCode());
    }


    @Test
    void testTruthHash() {
        assertEquals(new DiscreteTruth(0.5f, 0.5f).hashCode(), new DiscreteTruth(0.5f, 0.5f).hashCode());
        assertNotEquals(new DiscreteTruth(1.0f, 0.5f).hashCode(), new DiscreteTruth(0.5f, 0.5f).hashCode());
        assertNotEquals(new DiscreteTruth(0.51f, 0.5f).hashCode(), new DiscreteTruth(0.5f, 0.5f).hashCode());
        assertNotEquals(new DiscreteTruth(0.506f, 0.5f).hashCode(), new DiscreteTruth(0.5f, 0.5f).hashCode());

        assertEquals(new DiscreteTruth(0, 0.01f).hashCode(), new DiscreteTruth(0, 0.01f).hashCode());


        assertEquals(new DiscreteTruth(0.504f, 0.5f, 0.01f).hashCode(), new DiscreteTruth(0.5f, 0.5f, 0.01f).hashCode());
        assertEquals(new DiscreteTruth(0.004f, 0.01f, 0.01f).hashCode(), new DiscreteTruth(0, 0.01f, 0.01f).hashCode());
        assertNotEquals(new DiscreteTruth(0.006f, 0.01f, 0.01f).hashCode(), new DiscreteTruth(0, 0.01f, 0.01f).hashCode());


    }

    @Test
    void testTruthHashUnhash() {
        XorShift128PlusRandom rng = new XorShift128PlusRandom(2);
        for (int i = 0; i < 1000; i++)
            hashUnhash(rng.nextFloat(), rng.nextFloat() * (1f - NAL.truth.TRUTH_EPSILON * 2));
    }

    private static void hashUnhash(float f, float c) {
        Truth t = new DiscreteTruth(f, c);
        Truth u = Truth.intToTruth(t.hashCode());
        assertNotNull(u, () -> t + " unhased to null via hashCode " + t.hashCode());
        assertEquals(t, u);
    }


    @Test
    void testExpectation() {
        assertEquals(0.75f, new DiscreteTruth(1f, 0.5f).expectation(), 0.01f);
        assertEquals(0.95f, new DiscreteTruth(1f, 0.9f).expectation(), 0.01f);
        assertEquals(0.05f, new DiscreteTruth(0f, 0.9f).expectation(), 0.01f);
    }

    static void printTruthChart() {
        float c = 0.9f;
        for (float f1 = 0f; f1 <= 1.001f; f1 += 0.1f) {
            for (float f2 = 0f; f2 <= 1.001f; f2 += 0.1f) {
                Truth t1 = new DiscreteTruth(f1, c);
                Truth t2 = new DiscreteTruth(f2, c);
                System.out.println(t1 + " " + t2 + ":\t" +
                        TruthFunctions.comparison(t1, t2, TRUTH_EPSILON));
            }
        }
    }

    @Test
    void testTruthPolarity() {
        assertEquals(0f, t(0.5f, 0.9f).polarity(), 0.01f);
        assertEquals(1f, t(0f, 0.9f).polarity(), 0.01f);
        assertEquals(1f, t(1f, 0.9f).polarity(), 0.01f);
        assertEquals(1f, t(1f, 0.5f).polarity(), 0.01f);
    }

//    @Disabled
//    @Test
//    void testEvidenceHorizonDistortion() {
//        Truth a = t(1f, 0.9f);
//        double eviA = a.evi();
//        Truth b = t(1f, 0.9f);
//        double eviB = b.evi();
//        float eviABintersect = TruthFunctions.c2w(0.81f);
//        double eviABintersectRaw = eviA * eviB;
//        double eviABintersectRawToConf = w2c(eviA * eviB);
//        System.out.println();
//    }


//    @Test void PostFn() {
//        /* bad
//        $ .02 (right-->trackXY)! 3492⋈3500 %.28;.09% {3492: 1;36;3 A;3 K;3N;3O}
//            $.17 (trackXY-->happy)! %1.0;.90% {0: 1}
//            $.01 ((right-->$1) ==>-4 ($1-->happy)). 3486⋈3494 %.28;.34% {3490: 36;3A;3K;3N;3O} */
//
//        assertNull(TruthFunctions2.post(t(1, 0.9f), t(0.28f, 0.34f), true, 0));
//
//        assertTrue(
//                TruthFunctions2.post(t(1, 0.9f), t(0.75f, 0.9f), true, 0).expectation()
//                >
//                TruthFunctions2.post(t(1, 0.9f), t(0.65f, 0.9f), true, 0).expectation()
//        );
////        assertTrue(
////                TruthFunctions2.post(t(0.75f, 0.9f), t(0.75f, 0.9f), true, 0).expectation()
////                        >
////                        TruthFunctions2.post(t(1, 0.9f), t(0.65f, 0.9f), true, 0).expectation()
////        );
//    }

    /** a sanity test, but an algebraic proof would suffice instead */
    @Test void AssociativityOfOrFunction() {
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (int i = 0; i < 100; i++) {
            float a = rng.nextFloat(), b = rng.nextFloat(), c = rng.nextFloat();
            assertEquals(
                Util.or(Util.or(a, b), c),
                Util.or(Util.or(a, c), b),
                0.01f
            );
            assertEquals(
                Util.or(Util.or(a, b), c),
                Util.or(Util.or(a, c), b),
                0.01f
            );
        }
    }
}
