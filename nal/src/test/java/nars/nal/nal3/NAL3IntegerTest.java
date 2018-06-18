package nars.nal.nal3;

import nars.$;
import nars.term.atom.Int;
import nars.test.NALTest;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

class NAL3IntegerTest extends NALTest {

    private static final int cycles = 2500;


    @Test
    void testIntRangeStructuralDecomposition() {

        test
                .nar.believe($.inh(Int.range(1, 3), $.the("a")), Tense.Eternal, 1f, 0.9f);
        test
                .mustBelieve(cycles, "a:1", 1.0f, 0.81f); 

    }

    @Test
    void testIntRangeStructuralDecomposition2d() {

        test
                .nar.believe(
                $.inh($.p(Int.range(1, 3), Int.range(1, 3)), $.the("a")
                ), Tense.Eternal, 1f, 0.9f);
        test
                .mustBelieve(cycles, "a(2,2)", 1.0f, 0.59f); 

    }

}
