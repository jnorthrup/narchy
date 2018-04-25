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
                Evaluation.solveAll($$("sort((c,b,a))"), n));
        assertEquals(
                Set.of($$("(1,2)")),
                Evaluation.solveAll($$("sort({1,2})"), n));
    }
    @Test public void testSortSubst1() {
        assertEquals(
                Set.of($$("sort((2,1),(1,2))")),
                Evaluation.solveAll($$("sort((2,1),#a)"), n));
        assertEquals(
                Set.of($$("(sort((2,1),(1,2))==>(1,2))")),
                Evaluation.solveAll($$("(sort((2,1),#a) ==> #a)"), n));
    }


    @Test public void testSortSubst2() {
        assertEquals(
                Set.of($$("(&&,sort((1,2),(1,2)),append(1,(),1),append(2,(),2))")),
                Evaluation.solveAll($$("(&&, append(1,(),#a),append(2,(),#b),sort((#a,#b),#sorted))"), n));
    }

}
