package nars.op;

import jcog.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import nars.term.atom.Int;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.$.$$;
import static nars.Op.False;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** arithmetic operators and arithmetic introduction tests */
class ArithmeticTest {

    @Test
    void testAddSolve() throws Narsese.NarseseException {
        NAR n = NARS.shell();
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
        NAR t = NARS.shell();
        String fwd = $.$("add(#x,1)").eval(t).toString();
        String rev = $.$("add(1,#x)").eval(t).toString();
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
                //"((#1,add(#1,1))&&(#1<->2))",
                "((#1,add(#1,1))&&equal(#1,2))",
                ArithmeticIntroduction.apply($.$("(2,3)"), true, rng).toString());
    }

    @Test
    void test2() {
        assertEquals(
                "(x(#1,add(#1,1))&&equal(#1,2))",
                ArithmeticIntroduction.apply($.$$("x(2,3)"), true, rng).toString());
    }
    @Test
    void test2b() {
        assertEquals(
                "(x(#1,add(#1,1))&|equal(#1,2))",
                ArithmeticIntroduction.apply($.$$("x(2,3)"), false, rng).toString());

    }

    @Test
    void testContradictionResultsInFalse() {
        assertEval(False, "(add(1,1,#2) && add(#2,1,1))");
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
        assertEval($$(a),  q);

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
        NAR n = NARS.tmp(6);
        new ArithmeticIntroduction(8, n);

        final int cycles = 500;

        TestNAR t = new TestNAR(n);
        t.confTolerance(0.8f);
        n.freqResolution.set(0.1f);
        n.termVolumeMax.set(12);
        //t.log();


        for (int a = 0; a <= 2; a++) {
            t.believe(("(a," + a + ")"));
        }

        for (int x = 3; x <= 4; x++) {
            //t.input("(a," + x + ")?");
            t.mustBelieve(cycles, "(a," + x + ")", 1f, 0.5f);
        }
        t.test();
    }

}