package nars.nal.nal7;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.term.Term;
import org.junit.jupiter.api.Test;

public class NAL7ImplProjectionTest {

    @Test public void test1() {
        int eventTime = 0, implTime = 0;
        int implDT = 5;
        int dur = 1;

        Term x = $.the("x");
        Term y = $.the("y");

        Param.DEBUG = true;
        NAR n = NARS.tmp();
        n.log();
        n.time.dur(dur);
        n.inputAt(eventTime, "x. :|:");
        n.inputAt(implTime, "(x ==>+" + implDT + " y). :|:");
        n.run(Math.max(eventTime, implTime+implDT)+1);

        n.concept(x).print();
        n.concept(y).print();
    }
}
