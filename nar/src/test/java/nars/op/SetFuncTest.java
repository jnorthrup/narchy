package nars.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.derive.BasicDeriver;
import nars.derive.Derivers;
import nars.eval.Evaluation;
import nars.term.Compound;
import nars.term.Term;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SetFuncTest {

    private final NAR n = NARS.shell();

    @Test
    void testSortDirect() {

        assertEquals(
                Set.of($$("(a,b,c)")),
                Evaluation.eval($$("sort((c,b,a),quote)"), n));
        assertEquals(
                Set.of($$("(1,2)")),
                Evaluation.eval($$("sort({1,2},quote)"), n));
    }

    @Test
    void testSortApply() {
        
        assertEquals(
                Set.of($$("(a,b,(c,d))")),
                Evaluation.eval($$("sort(((c,d),b,a),complexity)"), n));

        assertEquals(
                Set.of($$("sort(((c,d),b,a),complexity,(a,b,(c,d)))")),
                Evaluation.eval($$("sort(((c,d),b,a),complexity,#x)"), n));
    }


    @Test
    void testSortSubst1() {
        assertEquals(
                Set.of($$("sort((2,1),quote,(1,2))")),
                Evaluation.eval($$("sort((2,1),quote,#a)"), n));
        assertEquals(
                Set.of($$("(sort((2,1),quote,(1,2))==>(1,2))")),
                Evaluation.eval($$("(sort((2,1),quote,#a) ==> #a)"), n));
    }


    @Test
    void testSortSubst2() {
        assertEquals(
                Set.of($$("(&&,sort((1,2),quote,(1,2)),append(1,(),1),append(2,(),2))")),
                Evaluation.eval($$("(&&, append(1,(),#a),append(2,(),#b),sort((#a,#b),quote,#sorted))"), n));
    }
    @Test
    void testSortSubst3() {
        assertEquals(
                Set.of($$("(sort((3,2),quote,(2,3))&&add(1,2,3))")),
                Evaluation.eval(
                    $$("(&&,add(1,#x,#a),sort((#a,2),quote,(2,3)))"), n));
    }


    @Test void testMember1_true() {
        assertEquals(
                Set.of($$("member(a,{a,b})")),
                Evaluation.eval($$("member(a,{a,b})"), n));
        assertEquals(
                Set.of($$("member(a,(a,b))")),
                Evaluation.eval($$("member(a,(a,b))"), n));

    }
    @Test void testMember1_false() {
        assertEquals(
                Set.of($$("--member(c,{a,b})")),
                Evaluation.eval($$("member(c,{a,b})"), true, true, n));

    }

    @Test void testMember1_generator() {
        assertEquals(
                Set.of($$("member(a,{a,b})"),$$("member(b,{a,b})")),
                Evaluation.eval($$("member(#x,{a,b})"), n));

        assertEquals(
                Set.of($$("(a)"),$$("(b)")),
                Evaluation.eval($$("(member(#x,{a,b}) && (#x))"), n));

    }
    @Test void testMultiTermute1() {
        nars.term.Term s = $.$$("(((--,(tetris-->((--,#2)&&left))) &&+4710 (member(#1,{right,#2})&&(tetris-->#1))) &&+4000 member(#1,{2,3}))");
        assertEquals(
                Set.of(
                        $$("((--,(tetris-->((--,2)&&left))) &&+4710 (tetris-->2))"),
                        $$("((--,(tetris-->((--,3)&&left))) &&+4710 (tetris-->3))")
                ),
                Evaluation.eval((Compound)s, n));
    }


    @Test void testMember_Combine_Rule() {
        NAR n = NARS.shell();
        new BasicDeriver(Derivers.files(n, "nal2.member.nal"));
        TestNAR t = new TestNAR(n);
        t.believe("(member(#1,{a,b}) && (x(#1), y(#1)))");
        t.believe("(member(#1,{c,d}) && (x(#1), y(#1)))");
        int cycles = 500;
        t.mustBelieve(cycles, "(member(#1,{a,b,c,d}) && (x(#1), y(#1)))", 1, 0.81f);
        t.run(cycles);
    }
    @Test void testMember_Diff_Rule() {
        NAR n = NARS.shell();
        new BasicDeriver(Derivers.files(n, "nal2.member.nal"));
        TestNAR t = new TestNAR(n);
        t.believe("  (member(#1,{a,b,c}) && (x(#1), y(#1)))");
        t.believe("--(member(#1,{b,d  }) && (x(#1), y(#1)))");
        int cycles = 500;
        t.mustBelieve(cycles, "(member(#1,{a,c}) && (x(#1), y(#1)))", 1, 0.81f);
        t.run(cycles);
    }

    @Test void testTermutesShuffled() {

        nars.term.Term s = $.$$("(member(#1,{a,b,c})&&(x-->#1)))");

        Set<List<Term>> permutes = new HashSet();

        for (int i = 0; i < 32; i++) {
            List<Term> f = new FasterList();
            Evaluation.eval(s, n::axioms, f::add);
            permutes.add(f);
        }

        System.out.println(permutes);
        assertEquals(6, permutes.size());
    }
}
