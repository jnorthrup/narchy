package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.term.Solution;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static nars.Op.False;
import static nars.Op.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListTest {


    @Test
    public void testAppendTransform() {
        NAR n = NARS.shell();
        
        assertEquals(
                Set.of($$("(x,y)")),
                Solution.solve($$("append((x),(y))"), n.concepts.functors));
        assertEquals(
                Set.of($$("append(#x,(y))")),
                Solution.solve($$("append(#x,(y))"), n.concepts.functors));

    }

    @Test
    public void testAppendResult() {
        NAR n = NARS.shell();
        

        //solve result
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),(y),#what)"), n.concepts.functors));

        //solve result in multiple instances
        assertEquals(
                Set.of($$("(append((x),(y),(x,y)) && ((x,y)<->solution))")),
                Solution.solve($$("(append((x),(y),#what) && (#what<->solution))"), n.concepts.functors));

    }


    @Test
    public void testTestResult() {
        NAR n = NARS.shell();
        

        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),(y),(x,y))"), n.concepts.functors));

        assertEquals(
                Set.of($$("append(x,y,(x,y))")),
                Solution.solve($$("append(x,y,(x,y))"), n.concepts.functors));

        assertEquals(
                Set.of(False),
                Solution.solve($$("append((x),(y),(x,y,z))"), n.concepts.functors));

    }

    @Test
    public void testAppendTail() {
        NAR n = NARS.shell();
        

        //solve tail
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),#what,(x,y))"), n.concepts.functors));

        //solve tail with non-list prefix that still matches
        assertEquals(
                Set.of($$("append(x,(y),(x,y))")),
                Solution.solve($$("append(x,#what,(x,y))"), n.concepts.functors));

        //solve tail but fail
        assertEquals(
                Set.of(Null),
                Solution.solve($$("append((z),#what,(x,y))"), n.concepts.functors));

        //solve result in multiple instances
        assertEquals(
                Set.of($$("(append((x),(),(x)) && (()<->solution))")),
                Solution.solve($$("(append((x),#what,(x)) && (#what<->solution))"), n.concepts.functors));

    }

    @Test
    public void testAppendHeadAndTail() {
        NAR n = NARS.shell();
        

        assertEquals(
                Set.of(
                        $$("append((x,y,z),(),(x,y,z))"),
                        $$("append((x,y),(z),(x,y,z))"),
                        $$("append((x),(y,z),(x,y,z))"),
                        $$("append((),(x,y,z),(x,y,z))")
                ),
                Solution.solve($$("append(#x,#y,(x,y,z))"), n.concepts.functors));
    }
    @Test
    public void testAppendHeadAndTailMulti() {
        NAR n = NARS.shell();
        

        assertEquals(
            Set.of(
                    $$("(append((),(x,y),(x,y)),append((a),(b),(a,b)))"),
                    $$("(append((x),(y),(x,y)),append((),(a,b),(a,b)))"),
                    $$("(append((),(x,y),(x,y)),append((a,b),(),(a,b)))"),
                    $$("(append((x),(y),(x,y)),append((a,b),(),(a,b)))"),
                    $$("(append((x,y),(),(x,y)),append((a,b),(),(a,b)))"),
                    $$("(append((),(x,y),(x,y)),append((),(a,b),(a,b)))"),
                    $$("(append((x),(y),(x,y)),append((a),(b),(a,b)))"),
                    $$("(append((x,y),(),(x,y)),append((a),(b),(a,b)))"),
                    $$("(append((x,y),(),(x,y)),append((),(a,b),(a,b)))")
            ),
            Solution.solve($$("(append(#x,#y,(x,y)), append(#a,#b,(a,b)))"), n.concepts.functors));

        assertEquals(
                Set.of(
                        $$("(append((),(x,y),(x,y)),append((),(x,b),(x,b)))"),
                        $$("(append((x),(y),(x,y)),append((x),(b),(x,b)))")
                ),
                Solution.solve($$("(append(#x,#y,(x,y)), append(#x,#b,(x,b)))"), n.concepts.functors));

        assertEquals(
                Set.of(
                        $$("(append((),(x,y),(x,y)) && append((),(x,b),(x,b)))"),
                        $$("(append((x),(y),(x,y)) && append((x),(b),(x,b)))")
                ),
                Solution.solve($$("(&&,append(#x,#y,(x,y)),append(#a,#b,(x,b)),equal(#x,#a))"), n.concepts.functors));

    }

    @Test
    public void testAppendHead() {
        NAR n = NARS.shell();
        

        //solve head
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append(#what,(y),(x,y))"), n.concepts.functors));

        assertEquals(
                Set.of($$("append((),(x,y),(x,y))")),
                Solution.solve($$("append(#what,(x,y),(x,y))"), n.concepts.functors));

    }
//    @Test
//    public void test1() {
//        NAR n = NARS.tmp(3);
//        Deriver listDeriver = new Deriver(n, "list.nal");
//
////                "motivation.nal"
////                //, "goal_analogy.nal"
////        ).apply(n).deriver, n) {
//        TestNAR t = new TestNAR(n);
//    }
}
