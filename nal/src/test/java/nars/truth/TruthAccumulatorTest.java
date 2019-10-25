package nars.truth;

import nars.$;
import nars.truth.util.TruthAccumulator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Created by me on 3/22/17.
 */
class TruthAccumulatorTest {

    @Test
    void test1() {
        TruthAccumulator a = new TruthAccumulator();
        assertNull(a.commitAverage());
        a.add($.INSTANCE.t(0, 0.5f));
        assertEquals("%0.0;.50%", a.peekAverage().toString());
        assertEquals("%0.0;.50%", a.peekSum().toString());
        a.add($.INSTANCE.t(1f, 0.5f));
        assertEquals("%.50;.50%", a.peekAverage().toString());
        assertEquals("%.50;.67%", a.peekSum().toString());
        a.commit();
        assertNull(a.peekAverage());
        assertNull(a.commitAverage());
    }
}