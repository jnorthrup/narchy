package nars.op;

import jcog.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import nars.term.atom.Int;
import nars.term.util.TermTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static nars.$.$$;
import static nars.term.atom.Bool.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** arithmetic operators and arithmetic introduction tests */
class ArithmeticTest {
    static final NAR n = NARS.shell();

    @Test
    void testAddSolve() throws Narsese.NarseseException {

        n.believe("(add(1,$x,3)==>its($x))");
        n.run(2);
        //TODO
    }

    @Test
    void testAdd_2_const_pos_pos() {
        assertEval(Int.the(2), "add(1,1)");
    }
    @Test
    void testAdd_2_const_pos_neg() {
        assertEval(Int.the(1), "add(2,-1)");
    }

    @Test void testAdd_1_const_1_var() {
        assertEval($.varDep(1), "add(#1,0)");
    }



    @Test
    void testAddCommutive() throws Narsese.NarseseException {

        String fwd = n.eval($.$("add(#x,1)")).toString();
        String rev = n.eval($.$("add(1,#x)")).toString();
        assertEquals("add(#1,1)", fwd);
        assertEquals(fwd, rev);

    }

    @Test
    void testAddIdentity() {

        assertEval($.varDep(1), "add(#1,0)");
        assertEval($.varDep(1), "add(0,#1)");
    }
    @Test
    void testMulIdentity() {
        assertEval(Int.the(0), "mul(x,0)");
        assertEval($$("x"), "mul(x,1)");
        assertEval($.varDep(1), "mul(1,#1)");
        assertEval($.varDep(1), "mul(#1,1)");

    }


    private final Random rng = new XoRoShiRo128PlusRandom(1);
    
    @Test
    void test1() throws Narsese.NarseseException {
        assertArith("(2,3)", "((#1,add(#1,1))&&equal(#1,2))", "(cmp(#1,#2,-1)&&(#1,#2))");
    }

    private void assertArith(String q, String... p) {
        Set<String> each = new TreeSet();
        Set<String> s = Set.of(p);
        for (int i = 0; i < p.length*4; i++) {
            each.add(Arithmeticize.apply($$(q), rng).toString());
        }
        assertEquals(s, each);
    }


    @Test
    void test2() {
        assertArith("x(2,3)", "(x(#1,add(#1,1))&&equal(#1,2))", "(cmp(#1,#2,-1)&&x(#1,#2))");
    }
    @Test
    void test2_impl_subj() {
        assertArith("(x(2,3) ==> y)",
                "((x(#1,add(#1,1))==>y)&&equal(#1,2))",
                "((x(#1,#2)==>y)&&cmp(#1,#2,-1))");
    }
    @Test
    void testContradictionResultsInFalse() {
        assertEval(Null, "(add(1,1,#2) && add(#2,1,1))");
    }

    static void assertEval(Term out, String in) {
        assertEquals( out,
                NARS.shell().eval($$(in))
        );
    }


    @Disabled
    @Test
    void testSimBackSubstitution() throws Narsese.NarseseException {
        assertSolves("(&&,(#1,#2),(#2 <-> 3))", "(#1,3)");
    }


    static void assertSolves(String q, String a) throws Narsese.NarseseException {
        //1.
        //assertEval($$(a),  q);

        //2.
        NAR n = NARS.tmp(2);
        //n.termVolumeMax.setAt(14);
        TestNAR t = new TestNAR(n);
        t.mustBelieve(16, a, 1f,1f, 0.35f,0.9f);
        n.input(q + ".");
        t.test();
    }

    @Test
    void testCompleteAddInduction() {
        NAR n = NARS.tmp();
//        n.log();

        new Arithmeticize.ArithmeticIntroduction( n, 16);

        final int cycles = 500;

        TestNAR t = new TestNAR(n);
        t.confTolerance(0.8f);
        n.freqResolution.set(0.25f);
        n.termVolMax.set(19);


        for (int a = 1; a <= 2; a++) {
            t.believe(("(a," + a + ")"));
        }

        for (int x = 3; x <= 4; x++) {
            //t.input("(a," + x + ")?");
            t.mustBelieve(cycles, "(a," + x + ")", 1f, 0.5f);
        }
        t.test();
    }

    @Test
    void testComparator_Inline() {
        TermTest.assertEq("-1", n.eval($$("cmp(1,2)")));
    }
    @Test
    void testComparator1() {
        TermTest.assertEq("cmp(1,2,-1)", n.eval($$("cmp(1,2,#x)")));
    }
    @Test
    void testComparator2() {
        TermTest.assertEq("cmp(#1,2,#2)", n.eval($$("cmp(#1,2,#x)")));
    }
    @Test
    void testComparator3() {
        assertArithmetic("(f(1)==>f(2))", "[((f(#1)==>f(#2))&&cmp(#1,#2,-1)), ((f(#1)==>f(add(#1,1)))&&equal(#1,1))]");
    }
    @Test
    void testComparator4() {
        assertArithmetic("(f(2)==>f(1))", "[((f(#1)==>f(#2))&&cmp(#2,#1,-1)), ((f(add(#1,1))==>f(#1))&&equal(#1,1))]");
    }

    @Test
    void testCmpReduceToEqual1() {
        TermTest.assertEq("equal(#1,#2)", n.eval($$("cmp(#x,#y,0)")));
        TermTest.assertEq("equal(#1,#2)", n.eval($$("cmp(#y,#x,0)")));
    }
    @Test
    void testCmpReduceToEqual2() {
        TermTest.assertEq("equal((a,#1),(b,#2))", n.eval($$("cmp((a,#x),(b,#y),0)")));
        TermTest.assertEq("equal((a,#2),(b,#1))", n.eval($$("cmp((b,#x),(a,#y),0)")));
    }

    @Test
    void testComparatorOrdering1() {
        TermTest.assertEq("cmp(1,2,-1)", n.eval($$("cmp(2,1,#x)")));
    }
    @Test
    void testComparatorOrderingConstant() {

        TermTest.assertEq("cmp(#1,2,-1)", n.eval($$("cmp(#1,2,-1)")));
        TermTest.assertEq("cmp(#1,2,-1)", n.eval($$("cmp(2,#1,1)")));
    }

    @Test
    void testComparatorOrdering_withVars() {
        TermTest.assertEq("cmp(1,2,-1)", n.eval($$("cmp(2,1,#x)")));
    }

    @Test
    void testComparatorCondition_1() {
        TermTest.assertEq("f(4)", n.eval($$("(&&, cmp(#1,3,1), f(#1), equal(#1,4))")));
    }

    @Test
    void testComparatorCondition_2() {
        //backwards solve possible because cmp==0
        TermTest.assertEq("f(4,4)", n.eval($$("(&&, cmp(#1,#2,0), f(#1,#2), equal(#1,4))")));

    }

    @Test
    void testComparatorWithVars_DontEval() {
        TermTest.assertEq("cmp(x(1),x(2),-1)", n.eval($$("cmp(x(1),x(2),#c)"))); //constant
        TermTest.assertEq("cmp(x(#1),x(#2),#3)", n.eval($$("cmp(x(#a),x(#b),#c)"))); //variable
        TermTest.assertEq("cmp(x(#1),x(#1),0)", n.eval($$("cmp(x(#a),x(#a),#c)"))); //variable, but equality known
    }

    @Test void testNonConjArithmeticize() {
        assertArithmetic("x(1,1)", null); //nothing to do
        assertArithmetic("x(1,2)", "[(cmp(#1,#2,-1)&&x(#1,#2)), (x(#1,add(#1,1))&&equal(#1,1))]");
        //assertArithmetic("((1 && --2)-->x)", "[(((#1~add(#1,1))-->x)&&equal(#1,1)), (((#2~#1)-->x)&&cmp(#2,#1,-1))]");
        //assertArithmetic("(((1,1)~(2,3))-->x)", "4 of them");
    }

    @Test void testXOR() {
        assertEval($$("xor(true,false)"), "xor(true,false)");
        assertEval($$("xor(#1,true)"), "xor(#x,true)");
        assertEval($$("(--,xor(true,true))"), "xor(true,true)");
    }

    private void assertArithmetic(String x, String y) {
        Set<String> solutions = new TreeSet();
        for (int i = 0; i < 10; i++) {
            Term s = Arithmeticize.apply($$(x), rng);
            if (s == null) {
                assertNull(y);
                return;
            } else
                solutions.add(s.toString());
        }
        assertEquals(y, solutions.toString());
    }
}