package nars.op;

import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.$.$$;
import static nars.Op.False;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** arithmetic operators and arithmetic introduction tests */
class ArithmeticTest {


    @Test
    void testAdd() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        assertEquals("2",
                $.$("add(1,1)").eval(t, false).toString());
        assertEquals("1",
                $.$("add(2,-1)").eval(t, false).toString());
        assertEquals("#1",
                $.$("add(#1,0)").eval(t, false).toString());
    }
    @Test
    void testMul() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        assertEquals("0",
                $.$("mul(x,0)").eval(t, false).toString());
        assertEquals("x",
                $.$("mul(x,1)").eval(t, false).toString());
    }

    @Test
    void testAddCommutive() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        String fwd = $.$("add(#x,1)").eval(t, false).toString();
        String rev = $.$("add(1,#x)").eval(t, false).toString();
        assertEquals(
                "add(#1,1)",
                fwd);
        assertEquals(
                fwd,
                rev);

    }

    @Test
    void testAddMulIdentity() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        assertEquals("#1",
                $.$("add(#1,0)").eval(t, false).toString());
        assertEquals("#1",
                $.$("add(0,#1)").eval(t, false).toString());
        assertEquals("#1",
                $.$("mul(1,#1)").eval(t, false).toString());
        assertEquals("#1",
                $.$("mul(#1,1)").eval(t, false).toString());

    }

    private final Random rng = new XoRoShiRo128PlusRandom(1);
    
    @Test
    void test1() throws Narsese.NarseseException {
        assertEquals(
                //"((#1,add(#1,1))&&(#1<->2))",
                "((#1,add(#1,1))&&(#1<->2))",
                
                
                ArithmeticIntroduction.apply($.$("(2,3)"), true, rng).toString());
    }

    @Test
    void test2() throws Narsese.NarseseException {
        assertEquals(
                "(x(#1,add(#1,1))&&(#1<->2))",
                
                ArithmeticIntroduction.apply($.$("x(2,3)"), true, rng).toString());
    }
    @Test
    void test2b() throws Narsese.NarseseException {
        assertEquals(
                "(x(#1,add(#1,1))&|(#1<->2))",
                
                ArithmeticIntroduction.apply($.$("x(2,3)"), false, rng).toString());

    }

    @Test
    void testContradictionResultsInFalse() {
        assertEquals(
                False,
                $$("(add(1,1,#2) && add(#2,1,1))").eval(NARS.shell(), false)
        );
    }

    @Test
    void testEqBackSubstitution() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.termVolumeMax.set(12);
        TestNAR t = new TestNAR(n);
        //t.log();
        t.mustBelieve(100, "((#1,4)&&(#1,3))", 1f, 0.9f);
        n.input("(&&,(#1,add(#2,1)),equal(#2,3),(#1,#2)).");
        t.test();
    }
    @Test
    void testSimBackSubstitution() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.termVolumeMax.set(14);
        TestNAR t = new TestNAR(n);
        //t.mustBelieve(100, "((#1,4)&&(#1,3))", 1f, 0.9f);
        //n.input("(&&,(#1,add(#2,1)),(#1,#2),(#2 <-> 3)).");
        n.input("(&&,(#1,#2),(#2 <-> 3)).");
        t.mustBelieve(100, "(#1,3)", 1f, 0.81f);
        t.test();
    }

    @Test
    void testSimBackSubstitution2() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.termVolumeMax.set(14);
        TestNAR t = new TestNAR(n);
        n.input("(&&,(#1,add(#2,1)),(#1,#2),(#2 <-> 3)).");
        t.mustBelieve(100, "((#1,4)&&(#1,3))", 1f, 0.81f);
        t.test();
    }

    @Test
    void testCompleteAddInduction() {
        NAR n = NARS.tmp(6);
        new ArithmeticIntroduction(8, n);

        final int cycles = 4000;

        TestNAR t = new TestNAR(n);
        t.confTolerance(0.8f);
        n.freqResolution.set(0.1f);
        n.termVolumeMax.set(16);
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