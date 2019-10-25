package nars.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.eval.Evaluation;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nars.$.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SetFuncTest {

    private final NAR n = NARS.shell();

    @Test
    void testSortDirect() {

        assertEquals(
                Set.of(INSTANCE.$$("(a,b,c)")),
                Evaluation.eval(INSTANCE.$$("sort((c,b,a),quote)"), n));
        assertEquals(
                Set.of(INSTANCE.$$("(1,2)")),
                Evaluation.eval(INSTANCE.$$("sort({1,2},quote)"), n));
    }

    @Test
    void testSortApply() {
        
        assertEquals(
                Set.of(INSTANCE.$$("(a,b,(c,d))")),
                Evaluation.eval(INSTANCE.$$("sort(((c,d),b,a),complexity)"), n));

        assertEquals(
                Set.of(INSTANCE.$$("sort(((c,d),b,a),complexity,(a,b,(c,d)))")),
                Evaluation.eval(INSTANCE.$$("sort(((c,d),b,a),complexity,#x)"), n));
    }


    @Test
    void testSortSubst1() {
        assertEquals(
                Set.of(INSTANCE.$$("sort((2,1),quote,(1,2))")),
                Evaluation.eval(INSTANCE.$$("sort((2,1),quote,#a)"), n));
        assertEquals(
                Set.of(INSTANCE.$$("(sort((2,1),quote,(1,2))==>(1,2))")),
                Evaluation.eval(INSTANCE.$$("(sort((2,1),quote,#a) ==> #a)"), n));
    }


    @Test
    void testSortSubst2() {
        assertEquals(
                Set.of(INSTANCE.$$("(&&,sort((1,2),quote,(1,2)),append(1,(),1),append(2,(),2))")),
                Evaluation.eval(INSTANCE.$$("(&&, append(1,(),#a),append(2,(),#b),sort((#a,#b),quote,#sorted))"), n));
    }
    @Test
    void testSortSubst3() {
        assertEquals(
                Set.of(INSTANCE.$$("(sort((3,2),quote,(2,3))&&add(1,2,3))")),
                Evaluation.eval(
                    INSTANCE.$$("(&&,add(1,#x,#a),sort((#a,2),quote,(2,3)))"), n));
    }


    @Test void TermutesShuffled() {

        nars.term.Term s = $.INSTANCE.$$("(member(#1,{a,b,c})&&(x-->#1)))");

        Set<List<Term>> permutes = new HashSet();

        for (int i = 0; i < 32; i++) {
            List<Term> f = new FasterList();
            Evaluation.eval(s, f::add, n::axioms);
            permutes.add(f);
        }

        System.out.println(permutes);
        assertEquals(6, permutes.size());
    }
}
