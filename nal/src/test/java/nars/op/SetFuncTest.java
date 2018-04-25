package nars.op;

import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static nars.term.Evaluation.solve;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SetFuncTest {

    final NAR n = NARS.shell();

    @Test
    public void testSortDirect() {

        assertEquals(
                Set.of($$("(a,b,c)")),
                solve($$("sort((c,b,a))"), n));
        assertEquals(
                Set.of($$("(1,2)")),
                solve($$("sort({1,2})"), n));
    }
    @Test public void testSortSubst1() {
        assertEquals(
                Set.of($$("sort((2,1),(1,2))")),
                solve($$("sort((2,1),#a)"), n));
        assertEquals(
                Set.of($$("(sort((2,1),(1,2))==>(1,2))")),
                solve($$("(sort((2,1),#a) ==> #a)"), n));
    }


    @Test public void testSortSubst2() {
        //TODO requires topological sort of the variable dependencies to determine critical evaluation order
        assertEquals(
                Set.of($$("(&&,sort((1,2),(1,2)),append(1,(),1),append(2,(),2))")),
                solve($$("(&&, append(1,(),#a),append(2,(),#b),sort((#a,#b),#sorted))"), n));
    }

}
