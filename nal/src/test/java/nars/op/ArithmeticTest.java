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

/** arithmetic operators and arithmetic introduction tests */
class ArithmeticTest {
    static final NAR n = NARS.shell();

    @Test
    void testAddSolve() throws Narsese.NarseseException {

        n.log();
        n.believe("(add(1,$x,3)==>its($x))");
        n.run(2);
        //TODO
    }

    @Test
    void testAdd() {
        assertEval(Int.the(2), "add(1,1)");
        assertEval(Int.the(1), "add(2,-1)");
        assertEval($.varDep(1), "add(#1,0)");
    }



    @Test
    void testAddCommutive() throws Narsese.NarseseException {

        String fwd = $.$("add(#x,1)").eval(n).toString();
        String rev = $.$("add(1,#x)").eval(n).toString();
        assertEquals("add(#1,1)", fwd);
        assertEquals(fwd, rev);

    }

    @Test
    void testAddMulIdentity() {

        assertEval($.varDep(1), "add(#1,0)");
        assertEval($.varDep(1), "add(0,#1)");
        assertEval(Int.the(0), "mul(x,0)");
        assertEval($$("x"), "mul(x,1)");
        assertEval($.varDep(1), "mul(1,#1)");
        assertEval($.varDep(1), "mul(#1,1)");

    }

    private final Random rng = new XoRoShiRo128PlusRandom(1);
    
    @Test
    void test1() throws Narsese.NarseseException {
        assertEquals(
                "((#1,add(#1,1))&&equal(#1,2))",
                Arithmeticize.apply($.$("(2,3)"), true, rng).toString());
    }

    @Test
    void test2() {
        assertEquals(
                "(x(#1,add(#1,1))&&equal(#1,2))",
                Arithmeticize.apply($.$$("x(2,3)"), true, rng).toString());
    }
    @Test
    void test2b() {
        assertEquals(
                "(x(#1,add(#1,1))&|equal(#1,2))",
                Arithmeticize.apply($.$$("x(2,3)"), false, rng).toString());

    }

    @Test
    void testContradictionResultsInFalse() {
        assertEval(Null, "(add(1,1,#2) && add(#2,1,1))");
    }

    static void assertEval(Term out, String in) {
        assertEquals( out,
                $$(in).eval(NARS.shell())
        );
    }

    @Test
    void testEqBackSubstitution() throws Narsese.NarseseException {
        assertSolves("(&&,(#1,add(#2,1)),equal(#2,3),(#1,#2))", "((#1,4)&&(#1,3))");

    }

    @Disabled
    @Test
    void testSimBackSubstitution() throws Narsese.NarseseException {
        assertSolves("(&&,(#1,#2),(#2 <-> 3))", "(#1,3)");
    }

    @Test
    void testSimBackSubstitution2() throws Narsese.NarseseException {
        assertSolves("(&&,(#1,add(#2,1)),(#1,#2),equal(#2,3))", "((#1,3)&&(#1,4))");
    }

    static void assertSolves(String q, String a) throws Narsese.NarseseException {
        //1.
        //assertEval($$(a),  q);

        //2.
        NAR n = NARS.tmp(2);
        //n.termVolumeMax.set(14);
        TestNAR t = new TestNAR(n);
        t.mustBelieve(16, a, 1f,1f, 0.35f,0.9f);
        n.input(q + ".");
        t.test();
    }

    @Test public void testEqualSolutionAddInverse() {
        assertEval($$("x(0)"), "(x(#1) && equal(add(#1,1),1))");
    }

    @Test public void testEqualSolutionComplex() {
        /*

        (&&,(--,(g(add(#1,1),0,(0,add(add(#1,1),9)))&&equal(add(#1,1),1))),(--,chronic(add(#1,1))),(--,add(#1,1)),(--,down))

        "equal(add(#1,1),1)" ===> (1 == 1+x) ===> (0 == x)

        drastic simplification:
            (&&,(--,(g(add(#1,1),0,(0,add(#1,10))&&equal(#1, 0)),(--,chronic(add(#1,1))),(--,add(#1,1)),(--,down))
            etc (&&,(--,(g(add(#1,1),0,(0,10)),(--,chronic(1)),(--,0),(--,down))
         */

        String t = "(&&,(--,(g(add(#1,1),0,(0,add(add(#1,1),9)))&&equal(add(#1,1),1))),(--,c(add(#1,1))),(--,add(#1,1)),(--,down))";

        assertEval($$("(&&,(--,g(1,0,(0,10))),(--,c(1)),(--,down),(--,1))"), t);
    }

    @Test
    void testCompleteAddInduction() {
        NAR n = NARS.tmp();
        n.log();
        new Arithmeticize.ArithmeticIntroduction( n, 16);

        final int cycles = 500;

        TestNAR t = new TestNAR(n);
        t.confTolerance(0.8f);
        n.freqResolution.set(0.1f);
        n.termVolumeMax.set(19);


        for (int a = 0; a <= 2; a++) {
            t.believe(("(a," + a + ")"));
        }

        for (int x = 3; x <= 4; x++) {
            //t.input("(a," + x + ")?");
            t.mustBelieve(cycles, "(a," + x + ")", 1f, 0.5f);
        }
        t.test();
    }

    @Test
    void testComparator() {
        TermTest.assertEq("-1", $$("cmp(1,2)").eval(n));
        TermTest.assertEq("cmp(1,2,-1)",$$("cmp(1,2,#x)").eval(n));
        TermTest.assertEq("cmp(1,2,-1)",$$("cmp(2,1,#x)").eval(n));
        TermTest.assertEq("cmp(#1,2,#2)",$$("cmp(#1,2,#x)").eval(n));
//        TermTest.assertEq("less(#1,2)", "less(#1,2)");
//        TermTest.assertEq(False, $.$$("less(1,1)"));
//        TermTest.assertEq(False, $.$$("less(2,1)"));

        assertComparator("(x(1)==>x(2))", "[((x(#1)==>x(#2))&&cmp(#1,#2,-1)), ((x(#1)==>x(add(#1,1)))&&equal(#1,1))]");
        assertComparator("(x(2)==>x(1))", "[((x(#1)==>x(#2))&&cmp(#2,#1,-1)), ((x(add(#1,1))==>x(#1))&&equal(#1,1))]");

        TermTest.assertEq("do(#1)", $$("(&&, cmp(#1,3,1), do(#1), equal(#1,4))").eval(n));

        //backwards solve because cmp==0
        TermTest.assertEq("do(4,4)", $$("(&&, cmp(#1,#2,0), do(#1,#2), equal(#1,4))").eval(n));

    }

    private void assertComparator(String x, String y) {
        Set<String> solutions = new TreeSet();
        for (int i = 0; i < 10; i++) {
            Term s = Arithmeticize.apply($$(x), true, rng);
            solutions.add(s.toString());
        }
        assertEquals(y, solutions.toString());
    }
}