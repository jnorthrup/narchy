package nars.concept.dynamic;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.scalar.Scalar;
import nars.control.DurService;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScalarBeliefTableTest {

    @Test
    void test1() {
        NAR n = NARS.shell();

        MutableFloat xx = new MutableFloat(0);
        Scalar x = new Scalar($.the("x"), xx, n);
        DurService xAuto = x.auto(n, 1);
        ScalarBeliefTable xb = (ScalarBeliefTable) x.beliefs();
        n.synch();

        n.run(1);
        assertEquals(1, xb.series.size());
        assertEquals(1, xb.size());

        xx.set(0.5f);
        n.run(1);

        assertEquals(2, xb.series.size());
        assertEquals(2, xb.size());

    }

}