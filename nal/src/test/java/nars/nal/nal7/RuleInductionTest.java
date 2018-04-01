package nars.nal.nal7;

import com.google.common.math.PairedStatsAccumulator;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.derive.Deriver;
import nars.time.Tense;
import nars.truth.Truth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * tests the time constraints in which a repeatedly inducted
 * conj/impl belief can or can't "snowball" into significant confidence
 */
public class RuleInductionTest {
    @Test
    public void test1() throws Narsese.NarseseException {
        int dur = 1;
        int loops = 10;
        int period = 2;
        int dutyPeriod = 1;

        NAR n = NARS.tmp();
        n.termVolumeMax.set(3);
//        n.log();

        //dense proressing
        Deriver.derivers(n).forEach(d -> d.conceptsPerIteration.set(64));

        n.time.dur(dur);

        float lastAConjB_exp = 0;
        PairedStatsAccumulator aConjB_exp = new PairedStatsAccumulator();
        for (int i = 0; i < loops; i++) {
            n.clear(); //distraction clear

            n.believe("a", Tense.Present, 1, 0.9f);
            if (i > 0) {
                //TODO test that the newest tasklink inserted into concept 'a' is the, or nearly the strongest
                //n.concept("a").tasklinks().print();
            }
            n.run(dutyPeriod);
            n.believe("b", Tense.Present, 1, 0.9f);
            n.run(period-dutyPeriod); //delay

            long now = n.time();
            Truth aConjB = n.belief("(a &&+" + dutyPeriod + " b)", now).truth(now, n.dur());
            System.out.println(aConjB);
            float exp = aConjB.expectation();

            if (!(exp >= lastAConjB_exp)) {
                //for debug
                Task tt = n.belief("(a &&+" + dutyPeriod + " b)", now);

            }
//            assertTrue(exp > lastAConjB_exp); //increasing

            aConjB_exp.add(now, exp);
            lastAConjB_exp = exp;
        }

        System.out.println(aConjB_exp.yStats());
        System.out.println("slope=" + aConjB_exp.leastSquaresFit().slope());
        assertTrue(aConjB_exp.leastSquaresFit().slope() > 0); //rising confidence
    }
}
