package nars.truth;

import jcog.Texts;
import jcog.Util;
import jcog.random.XorShift128PlusRandom;
import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.task.Revision;
import nars.truth.func.TruthFunctions;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.t;
import static nars.Param.truth.TRUTH_EPSILON;
import static nars.truth.func.TruthFunctions.w2c;
import static nars.truth.func.TruthFunctions.w2cSafe;
import static org.junit.jupiter.api.Assertions.*;


class TruthTest {

    @Test
    void testRounding() {
        int discrete = 10;
        float precision = 1f / discrete;

        for (float x = 0.19f; x < 0.31f; x += 0.01f / 2) {
            int i = Util.floatToInt(x, discrete);
            float y = Util.intToFloat(i, discrete);
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

        float insignificant = Param.truth.TRUTH_EPSILON / 5f;
        float significant = Param.truth.TRUTH_EPSILON;

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

        float ff = 1.0f - Param.truth.TRUTH_EPSILON / 2.5f;
        assertTrue(1 == Util.round(ff, TRUTH_EPSILON));
        Truth aEqualWithinThresh = new DiscreteTruth(
                ff /* slightly less than half */,
                0.9f);
        assertEquals(a, aEqualWithinThresh);
        assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());

        Truth aNotWithinThresh = new DiscreteTruth(1.0f - Param.truth.TRUTH_EPSILON * 1.0f, 0.9f);
        assertNotEquals(a, aNotWithinThresh);
        assertNotEquals(a.hashCode(), aNotWithinThresh.hashCode());

    }

    @Test
    void testConfEquality() {
        Truth a = new DiscreteTruth(1.0f, 0.5f);

        Truth aEqualWithinThresh = new DiscreteTruth(1.0f, 0.5f - Param.truth.TRUTH_EPSILON / 2.1f /* slightly less than half the epsilon */);
        assertEquals(a, aEqualWithinThresh);
        assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());

        Truth aNotWithinThresh = new DiscreteTruth(1.0f, 0.5f - Param.truth.TRUTH_EPSILON * 1.0f);
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
            hashUnhash(rng.nextFloat(), rng.nextFloat() * (1f - Param.truth.TRUTH_EPSILON * 2));
    }

    private static void hashUnhash(float f, float c) {
        Truth t = new DiscreteTruth(f, c);
        if (t == null)
            return;
        Truth u = Truth.intToTruth(t.hashCode());
        assertNotNull(u, t + " unhased to null via hashCode " + t.hashCode());
        assertEquals(t, u);
    }


    @Test
    void testExpectation() {
        assertEquals(0.75f, new DiscreteTruth(1f, 0.5f).expectation(), 0.01f);
        assertEquals(0.95f, new DiscreteTruth(1f, 0.9f).expectation(), 0.01f);
        assertEquals(0.05f, new DiscreteTruth(0f, 0.9f).expectation(), 0.01f);
    }

    @Test
    void testTruthRevision() {
        Truth d = Revision.revise(new DiscreteTruth(1f, 0.1f), new DiscreteTruth(1f, 0.1f));
        assertEquals(1f, d.freq(), 0.01f);
        assertEquals(0.18f, d.conf(), 0.01f);

        Truth a = Revision.revise(new DiscreteTruth(1f, 0.3f), new DiscreteTruth(1f, 0.3f));
        assertEquals(1f, a.freq(), 0.01f);
        assertEquals(0.46f, a.conf(), 0.01f);

        Truth b = Revision.revise(new DiscreteTruth(0f, 0.3f), new DiscreteTruth(1f, 0.3f));
        assertEquals(0.5f, b.freq(), 0.01f);
        assertEquals(0.46f, b.conf(), 0.01f);

        Truth c = Revision.revise(new DiscreteTruth(1f, 0.9f), new DiscreteTruth(1f, 0.9f));
        assertEquals(1f, c.freq(), 0.01f);
        assertEquals(0.95f, c.conf(), 0.01f);
    }

    @Test void testDitheringSanity() {
        NAR n = NARS.shell();
        n.confResolution.set(0.1f);
        int steps = 99;
        float step = 0.01f;
        int subSteps = 9;
        float subStep = 0.001f;
        for (int i = 1; i < steps; i++) {
            for (int j = 1; j < subSteps; j++) {
                float c = step * i + (subStep) * j;
                @Nullable PreciseTruth p = t(1f, c).dither(n);
                if (p!=null)
                    System.out.println(p + "\t" + Texts.n2(c) + '\t' + Texts.n4(c)+ '\t' + c );
            }
        }
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

    @Disabled
    @Test
    void testEvidenceHorizonDistortion() {
        Truth a = t(1f, 0.9f);
        double eviA = a.evi();
        Truth b = t(1f, 0.9f);
        double eviB = b.evi();
        float eviABintersect = TruthFunctions.c2w(0.81f);
        double eviABintersectRaw = eviA * eviB;
        float eviABintersectRawToConf = w2c(eviA * eviB);
        System.out.println();
    }
}
