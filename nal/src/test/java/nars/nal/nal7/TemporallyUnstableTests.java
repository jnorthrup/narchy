package nars.nal.nal7;

import nars.NARS;
import nars.Param;
import org.junit.jupiter.api.Test;

/** temporal stability tests still remaning to be completely solved */
public class TemporallyUnstableTests {

    static {
        Param.DEBUG = true;
    }

    @Test
    void testTemporalStabilityConjInvertor () {
        new TemporalStabilityTests.T1(TemporalStabilityTests.conjInvertor, new int[]{1, 6, 11}, 1, 16).test(TemporalStabilityTests.CYCLES, NARS.tmp());
    }
    @Test
    void testTemporalStabilityLinkedTemporalConjOverlapping () {
        new TemporalStabilityTests.T1(TemporalStabilityTests.conjSeq2, new int[]{1, 3}, 1, 16).test(100, NARS.tmp());
    }
}
