package nars.concept.signal;

import jcog.Util;
import jcog.data.atomic.AtomicFloat;
import jcog.math.FloatNormalized;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.game.sensor.DigitizedScalar;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.junit.jupiter.api.Test;

/**
 * Created by me on 7/2/16.
 */
class DigitizedScalarTest {


    private static final float tolerance = 0.2f;


    @Test
    void testRewardConceptsFuzzification3() {
        NAR n = NARS.shell();
        AtomicFloat m = new AtomicFloat(0f);

        FloatNormalized range = new FloatNormalized(m::floatValue, -1, 1f, true);
        DigitizedScalar f = new DigitizedScalar(range, DigitizedScalar.FuzzyNeedle, n,
                $.p("low"), $.p("mid"), $.p("hih"));


        testSteadyFreqCondition(m, f, (freqSum) -> {
            System.out.println(freqSum + " " + tolerance);
            return Util.equals(freqSum, 1f, tolerance);
        }, n);
    }

    private void testSteadyFreqCondition(AtomicFloat m, DigitizedScalar f, FloatPredicate withFreqSum, NAR n) {


        for (int i = 0; i < 5; i++) {
            m.set(Math.sin(i / 2f));
            n.run();

            //TODO update to latest aPI

//            double freqSum = f.stream()
//                    .peek(x -> n.input(x.update((prev, next) -> $.t(next, n.confDefault(BELIEF)),
//                            n.time(), n.dur(), n)))
//                    .map(x -> n.beliefTruth(x, n.time()))
//                    .mapToDouble(x -> x != null ? x.freq() : 0f).sum();
//
//            assertTrue(withFreqSum.accept((float) freqSum), ()->Texts.n4(m.floatValue()) + "\t" +
//                    Joiner.on(",").join(f.stream().map(x -> x + "=" + x.asFloat()).collect(Collectors.toList())) + " " +
//                    freqSum);


        }
    }

    @Test
    void testRewardConceptsFuzzification2() {
        NAR n = NARS.tmp();
        AtomicFloat x = new AtomicFloat(0f);

        testSteadyFreqCondition(x,
                new DigitizedScalar(
                        new FloatNormalized(x::floatValue).updateRange(-1).updateRange(1),
                        DigitizedScalar.FuzzyBinary, n, $.p("x0"), $.p("x1"), $.p("x2")),
                (f) -> true /*Util.equals(f, 0.5f + 0.5f * m.floatValue(), tolerance)*/
                , n);
    }

//    @Disabled
//    @Test
//    void testServiceAndFluidEncoder() throws Narsese.NarseseException {
//        NAR n = NARS.tmp();
//
//        FloatRange x = new FloatRange(0f, 0f, 1f);
//        DigitizedScalar xc = new DigitizedScalar(x, DigitizedScalar.Fluid, n,
//                $.$("x(0)"), $.$("x(1)")
//        );
//
//        int dt = 20;
//
//        for (float v : new float[]{0f, 0.5f, 1f}) {
//
//
//            x.set(v);
//            xc.updatePrevNow(n.time() - n.dur(), n.time(), null);
//            n.run(1);
//
//            System.out.println("\n" + n.time() + " x=" + x);
//            xc.forEach(d -> {
//                Truth bt = n.beliefTruth(d, n.time());
//                System.out.println(d + "\t" + bt);
//            });
//
//            int m = (dt - 1) / 2;
//            n.run(m);
//
//            Truth[] f = xc.belief(n.time(), n);
//            float tolerance = 0.18f;
//            if (v == 0) {
//                assertEquals(0.0f, f[0].freq(), tolerance);
//                assertEquals(0.0f, f[1].freq(), tolerance);
//            } else if (v == 0.5f) {
//                assertEquals(1.0f, f[0].freq(), tolerance);
//                assertEquals(0.0f, f[1].freq(), tolerance);
//            } else if (v == 1f) {
//                assertEquals(1.0f, f[0].freq(), tolerance);
//                assertEquals(1.0f, f[1].freq(), tolerance);
//            }
//
//            n.run(dt - 1 - m);
//
//        }
//    }
}