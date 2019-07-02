package nars.op;

import nars.NAR;
import nars.NARS;
import nars.derive.BatchDeriver;
import nars.derive.Derivers;
import nars.eval.Evaluation;
import nars.test.TestNAR;
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

    @Test void testMember_Combine_Rule() {
        NAR n = NARS.shell();
        new BatchDeriver(Derivers.files(n, "nal2.member.nal"));
        TestNAR t = new TestNAR(n);
        t.believe("(member(#1,{a,b}) && (x(#1), y(#1)))");
        t.believe("(member(#1,{c,d}) && (x(#1), y(#1)))");
        int cycles = 500;
        t.mustBelieve(cycles, "(member(#1,{a,b,c,d}) && (x(#1), y(#1)))", 1, 0.81f);
        t.run(cycles);
    }
    @Test void testMember_Diff_Rule() {
        NAR n = NARS.shell();
        new BatchDeriver(Derivers.files(n, "nal2.member.nal"));
        TestNAR t = new TestNAR(n);
        t.believe("  (member(#1,{a,b,c}) && (x(#1), y(#1)))");
        t.believe("--(member(#1,{b,d  }) && (x(#1), y(#1)))");
        int cycles = 500;
        t.mustBelieve(cycles, "(member(#1,{a,c}) && (x(#1), y(#1)))", 1, 0.81f);
        t.run(cycles);
    }
}
