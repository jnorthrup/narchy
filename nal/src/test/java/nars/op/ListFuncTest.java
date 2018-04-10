package nars.op;

import nars.NAR;
import nars.NARS;
import nars.term.Solution;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static nars.Op.False;
import static nars.Op.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListFuncTest {
    
    final NAR n = NARS.shell();

    @Test
    public void testAppendTransform() {
        

        assertEquals(
                Set.of($$("(x,y)")),
                Solution.solve($$("append((x),(y))"), n));
        assertEquals(
                Set.of($$("append(#x,(y))")),
                Solution.solve($$("append(#x,(y))"), n));

    }

    @Test
    public void testAppendResult() {
        


        //solve result
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),(y),#what)"), n));

        //solve result in multiple instances
        assertEquals(
                Set.of($$("(append((x),(y),(x,y)) && ((x,y)<->solution))")),
                Solution.solve($$("(append((x),(y),#what) && (#what<->solution))"), n));

    }


    @Test
    public void testTestResult() {
        


        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),(y),(x,y))"), n));

        assertEquals(
                Set.of($$("append(x,y,(x,y))")),
                Solution.solve($$("append(x,y,(x,y))"), n));

        assertEquals(
                Set.of(False),
                Solution.solve($$("append((x),(y),(x,y,z))"), n));

    }

    @Test
    public void testAppendTail() {
        


        //solve tail
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),#what,(x,y))"), n));

        //solve tail with non-list prefix that still matches
        assertEquals(
                Set.of($$("append(x,(y),(x,y))")),
                Solution.solve($$("append(x,#what,(x,y))"), n));

        //solve tail but fail
        assertEquals(
                Set.of(Null),
                Solution.solve($$("append((z),#what,(x,y))"), n));

        //solve result in multiple instances
        assertEquals(
                Set.of($$("(append((x),(),(x)) && (()<->solution))")),
                Solution.solve($$("(append((x),#what,(x)) && (#what<->solution))"), n));

    }

    @Test
    public void testAppendHeadAndTail() {
        


        assertEquals(
                Set.of(
                        $$("append((x,y,z),(),(x,y,z))"),
                        $$("append((x,y),(z),(x,y,z))"),
                        $$("append((x),(y,z),(x,y,z))"),
                        $$("append((),(x,y,z),(x,y,z))")
                ),
                Solution.solve($$("append(#x,#y,(x,y,z))"), n));
    }
    @Test
    public void testAppendHeadAndTailMulti() {
        


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
            Solution.solve($$("(append(#x,#y,(x,y)), append(#a,#b,(a,b)))"), n));

        assertEquals(
                Set.of(
                        $$("(append((),(x,y),(x,y)),append((),(x,b),(x,b)))"),
                        $$("(append((x),(y),(x,y)),append((x),(b),(x,b)))")
                ),
                Solution.solve($$("(append(#x,#y,(x,y)), append(#x,#b,(x,b)))"), n));

        assertEquals(
                Set.of(
                        (Term)False,
                        $$("(append((),(x,y),(x,y)) && append((),(x,b),(x,b)))"),
                        $$("(append((x),(y),(x,y)) && append((x),(b),(x,b)))")
                ),
                Solution.solve($$("(&&,append(#x,#y,(x,y)),append(#a,#b,(x,b)),equal(#x,#a))"), n));

    }

    @Test
    public void testAppendHead() {
        
        

        //solve head
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append(#what,(y),(x,y))"), n));

        assertEquals(
                Set.of($$("append((),(x,y),(x,y))")),
                Solution.solve($$("append(#what,(x,y),(x,y))"), n));

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
