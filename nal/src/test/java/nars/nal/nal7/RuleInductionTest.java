package nars.nal.nal7;

import nars.NAR;
import nars.NARS;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

/**
 * tests the time constraints in which a repeatedly inducted
 * conj/impl belief can or can't "snowball" into significant confidence
 */
public class RuleInductionTest {
    @Test
    public void test1() {
        int dur = 1;
        int loops = 10;
        int period = 10;
        int dutyPeriod = 4;

        NAR n = NARS.tmp();
        n.termVolumeMax.set(8);
        n.log();

        n.time.dur(dur);

        for (int i = 0; i < loops; i++) {
            n.believe("a", Tense.Present, 1, 0.9f);
            n.run(dutyPeriod);
            n.believe("b", Tense.Present, 1, 0.9f);
            n.run(period-dutyPeriod); //delay
        }
    }
}
