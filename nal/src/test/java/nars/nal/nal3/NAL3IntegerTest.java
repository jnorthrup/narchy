package nars.nal.nal3;

import nars.$;
import nars.term.atom.Int;
import nars.util.time.Tense;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

public class NAL3IntegerTest extends NALTest {

    public static final int cycles = 2500;


    @Test
    public void testIntRangeStructuralDecomposition() {

        test
                .nar.believe($.inh(Int.range(1, 3), $.the("a")), Tense.Eternal, 1f, 0.9f);
        test
                .mustBelieve(cycles, "a:1", 1.0f, 0.81f); //structural decomposition

    }

    @Test
    public void testIntRangeStructuralDecomposition2d() {

        test
                .nar.believe(
                $.inh($.p(Int.range(1, 3), Int.range(1, 3)), $.the("a")
                ), Tense.Eternal, 1f, 0.9f);
        test
                .log()
                .mustBelieve(cycles, "a(2,2)", 1.0f, 0.59f); //structural decomposition

    }

}
