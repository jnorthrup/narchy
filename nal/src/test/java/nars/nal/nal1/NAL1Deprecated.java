package nars.nal.nal1;

import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class NAL1Deprecated extends NALTest {

    static final int cycles = 20;

    @Test
    @Disabled void inheritanceToSimilarity3() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f);
        tester.ask("<bird <-> swan>");
        tester.mustBelieve(cycles, "<bird <-> swan>", 0.9f, 0.45f);

    }

    @Test
    @Disabled void conversion() {
        test.believe("<bird --> swimmer>")
                .ask("<swimmer --> bird>")
                .mustOutput(cycles, "<swimmer --> bird>. %1.00;0.47%");
    }
}
