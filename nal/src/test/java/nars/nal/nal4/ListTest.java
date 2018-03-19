package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.derive.Deriver;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

public class ListTest {

    @Test
    public void test1() {
        NAR n = NARS.tmp(3);
        Deriver listDeriver = new Deriver(n, "list.nal");

//                "motivation.nal"
//                //, "goal_analogy.nal"
//        ).apply(n).deriver, n) {
        TestNAR t = new TestNAR(n);
    }
}
