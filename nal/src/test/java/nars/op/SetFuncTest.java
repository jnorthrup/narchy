package nars.op;

import nars.NAR;
import nars.NARS;
import nars.eval.Evaluation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SetFuncTest {

    private final NAR n = NARS.shell();

    @Test
    void testSortDirect() {

        assertEquals(
                Set.of($$("(a,b,c)")),
                Evaluation.query($$("sort((c,b,a),quote)"), n));
        assertEquals(
                Set.of($$("(1,2)")),
                Evaluation.query($$("sort({1,2},quote)"), n));
    }

    @Test
    void testSortApply() {
        
        assertEquals(
                Set.of($$("(a,b,(c,d))")),
                Evaluation.query($$("sort(((c,d),b,a),complexity)"), n));

        assertEquals(
                Set.of($$("sort(((c,d),b,a),complexity,(a,b,(c,d)))")),
                Evaluation.query($$("sort(((c,d),b,a),complexity,#x)"), n));
    }


    @Test
    void testSortSubst1() {
        assertEquals(
                Set.of($$("sort((2,1),quote,(1,2))")),
                Evaluation.query($$("sort((2,1),quote,#a)"), n));
        assertEquals(
                Set.of($$("(sort((2,1),quote,(1,2))==>(1,2))")),
                Evaluation.query($$("(sort((2,1),quote,#a) ==> #a)"), n));
    }


    @Test
    void testSortSubst2() {
        assertEquals(
                Set.of($$("(&&,sort((1,2),quote,(1,2)),append(1,(),1),append(2,(),2))")),
                Evaluation.query($$("(&&, append(1,(),#a),append(2,(),#b),sort((#a,#b),quote,#sorted))"), n));
    }
    @Test
    void testSortSubst3() {
        assertEquals(
                Set.of($$("(sort((3,2),quote,(2,3))&&add(1,2,3))")),
                Evaluation.query(
                    $$("(&&,add(1,#x,#a),sort((#a,2),quote,(2,3)))"), n));
    }

}
