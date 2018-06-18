package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

class MathFuncTest {

    @Test
    void testAddSolve() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.log();
        n.believe("(add(1,$x,3)==>its($x))");
        n.run(2);
    }
}
