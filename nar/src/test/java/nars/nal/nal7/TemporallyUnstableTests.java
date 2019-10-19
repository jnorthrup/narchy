package nars.nal.nal7;

import nars.NARS;
import org.junit.jupiter.api.Test;

/** temporal stability tests still remaning to be completely solved */
public class TemporallyUnstableTests {

//    static {
//
//    }

    @Test
    void testTemporalStabilityConjInvertor () {
        new TemporalStabilityTests.T1(new int[]{1, 6, 11}, 1, 16, TemporalStabilityTests.conjInvertor).test(TemporalStabilityTests.CYCLES, NARS.tmp());
    }
    @Test
    void testTemporalStabilityLinkedTemporalConjOverlapping () {
        new TemporalStabilityTests.T1(new int[]{1, 3}, 1, 16, TemporalStabilityTests.conjSeq2).test(100, NARS.tmp());
    }
}
