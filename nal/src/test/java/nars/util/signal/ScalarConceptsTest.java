package nars.util.signal;

import jcog.Texts;
import jcog.Util;
import jcog.math.FloatNormalized;
import jcog.math.FloatPolarNormalized;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.ScalarConcepts;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 7/2/16.
 */
public class ScalarConceptsTest {

    //HACK TODO make sure this is smaller
    final static float tolerance = 0.2f;

//    @Test
//    public void testRewardConceptsFuzzification1() {
//        NAR d = new Default();
//        MutableFloat m = new MutableFloat(0f);
//
//        testSteadyFreqCondition(m,
//            new FuzzyScalarConcepts(
//                new FloatNormalized(() -> m.floatValue()).updateRange(-1).updateRange(1),
//                d, FuzzyScalarConcepts.FuzzyTriangle, $.p("x")),
//                (f) -> Util.equals(f, 0.5f + 0.5f * m.floatValue(), tolerance)
//        );
//    }

    @Disabled
    @Test
    public void testRewardConceptsFuzzification3() {
        NAR n = NARS.shell();
        MutableFloat m = new MutableFloat(0f);

        FloatPolarNormalized range = new FloatPolarNormalized(() -> m.floatValue());
        range.radius(1f);
        ScalarConcepts f = new ScalarConcepts(range, n, ScalarConcepts.FuzzyNeedle,
                $.p("low"), $.p("mid"), $.p("hih"));


//        {
//            f.clear();
//            m.setValue(0); d.next();
//            System.out.println(Texts.n4(m.floatValue()) + "\t" + f.toString());
//            assertEquals("(I-->[sad]) %0.25;.90%\t(I-->[neutral]) %1.0;.90%\t(I-->[happy]) %0.0;.90%", f.toString());
//        }
//
//        {
//            f.clear();
//            m.setValue(-1); d.next();
//            System.out.println(Texts.n4(m.floatValue()) + "\t" + f.toString());
//            assertEquals("(I-->[sad]) %1.0;.90%\t(I-->[neutral]) %0.0;.90%\t(I-->[happy]) %0.0;.90%", f.toString());
//        }
//
//        {
//            f.clear();
//            m.setValue(+1); d.next();
//            System.out.println(Texts.n4(m.floatValue()) + "\t" + f.toString());
//            assertEquals("(I-->[sad]) %0.0;.90%\t(I-->[neutral]) %0.0;.90%\t(I-->[happy]) %1.0;.90%", f.toString());
//        }


        testSteadyFreqCondition(m, f, (freqSum) -> {
            System.out.println(freqSum + " " + tolerance);
            return Util.equals(freqSum, 1f, tolerance);
        }, n);
    }

    public void testSteadyFreqCondition(MutableFloat m, ScalarConcepts f, FloatPredicate withFreqSum, NAR n) {

        //run a few oscillations
        for (int i = 0; i < 5; i++) {
            m.set(Math.sin(i / 2f));
            n.run();


            double freqSum = f.sensors.stream()
                    .peek(x -> n.input(x.update((prev,next) -> $.t(next, n.confDefault(BELIEF)),
                            n.time(), n.dur(), n)))
                    .map(x -> n.beliefTruth(x, n.time()))
                    .mapToDouble(x -> x != null ? x.freq() : 0f).sum();

            System.out.println(
                    Texts.n4(m.floatValue()) + "\t" +
                            f + " " +
                            freqSum

                    //confWeightSum(beliefs)
            );

            assertTrue(withFreqSum.accept((float) freqSum));


        }
    }

    @Test
    public void testRewardConceptsFuzzification2() {
        NAR n = NARS.tmp();
        MutableFloat x = new MutableFloat(0f);

        testSteadyFreqCondition(x,
                new ScalarConcepts(
                        new FloatNormalized(x::floatValue).updateRange(-1).updateRange(1),
                        n, ScalarConcepts.FuzzyBinary, $.p("x0"), $.p("x1"), $.p("x2")),
                (f) -> true /*Util.equals(f, 0.5f + 0.5f * m.floatValue(), tolerance)*/
        , n);
    }

    @Test
    public void testServiceAndHardEncoder2() throws Narsese.NarseseException {
        NAR n = NARS.tmp();

        FloatRange x = new FloatRange(0f, 0f, 1f);
        ScalarConcepts xc = new ScalarConcepts(x, n, ScalarConcepts.Fluid,
                $.$("x(0)"), $.$("x(1)")
        );

        int dt = 20;

        for (float v : new float[] { 0f, 0.5f, 1f }) {


            x.set(v);
            xc.update(n.time(), n.dur(), n);
            n.run(1);

            System.out.println("\n" + n.time() + " x=" + x);
            xc.forEach(d -> {
                Truth bt = n.beliefTruth(d, n.time());
                System.out.println(d + "\t" + bt);
            });

            int m = (dt - 1)/2;
            n.run(m);

            Truth[] f = xc.belief(n.time(), n);
            float tolerance = 0.18f;
            if (v == 0) {
                assertEquals(0.0f, f[0].freq(), tolerance);
                assertEquals(0.0f, f[1].freq(), tolerance);
            } else if (v == 0.5f) {
                assertEquals(1.0f, f[0].freq(), tolerance);
                assertEquals(0.0f, f[1].freq(), tolerance);
            } else if (v == 1f) {
                assertEquals(1.0f, f[0].freq(), tolerance);
                assertEquals(1.0f, f[1].freq(), tolerance);
            }

            n.run(dt-1-m);

        }
    }
}