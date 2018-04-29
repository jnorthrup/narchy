package nars.op;

import nars.NAR;
import nars.NARS;
import nars.term.Evaluation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SetFuncTest {

    final NAR n = NARS.shell();

    @Test
    public void testSortDirect() {

        assertEquals(
                Set.of($$("(a,b,c)")),
                Evaluation.solveAll($$("sort((c,b,a),quote)"), n));
        assertEquals(
                Set.of($$("(1,2)")),
                Evaluation.solveAll($$("sort({1,2},quote)"), n));
    }

    @Test
    public void testSortApply() {
        //complexity(a)==complexity(b) so must in which case it will sort to the natural order
        assertEquals(
                Set.of($$("(a,b,(c,d))")),
                Evaluation.solveAll($$("sort(((c,d),b,a),complexity)"), n));

        assertEquals(
                Set.of($$("sort(((c,d),b,a),complexity,(a,b,(c,d)))")),
                Evaluation.solveAll($$("sort(((c,d),b,a),complexity,#x)"), n));
    }


    @Test public void testSortSubst1() {
        assertEquals(
                Set.of($$("sort((2,1),quote,(1,2))")),
                Evaluation.solveAll($$("sort((2,1),quote,#a)"), n));
        assertEquals(
                Set.of($$("(sort((2,1),quote,(1,2))==>(1,2))")),
                Evaluation.solveAll($$("(sort((2,1),quote,#a) ==> #a)"), n));
    }


    @Test public void testSortSubst2() {
        assertEquals(
                Set.of($$("(&&,sort((1,2),quote,(1,2)),append(1,(),1),append(2,(),2))")),
                Evaluation.solveAll($$("(&&, append(1,(),#a),append(2,(),#b),sort((#a,#b),quote,#sorted))"), n));
    }
    @Test public void testSortSubst3() {
        assertEquals(
                Set.of($$("(sort((3,2),quote,(2,3))&&add(1,2,3))")),
                Evaluation.solveAll(
                    $$("(&&,add(1,#x,#a),sort((#a,2),quote,(2,3)))"), n));
    }

}
