package nars.op;

import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** arithmetic operators and arithmetic introduction tests */
public class ArithmeticTest {


    @Test
    public void testAdd() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        assertEquals("2",
                $.$("add(1,1)").eval(t).toString());
        assertEquals("1",
                $.$("add(2,-1)").eval(t).toString());
        assertEquals("#1",
                $.$("add(#1,0)").eval(t).toString());
    }
    @Test
    public void testMul() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        assertEquals("0",
                $.$("mul(x,0)").eval(t).toString());
        assertEquals("x",
                $.$("mul(x,1)").eval(t).toString());
    }

    @Test
    public void testAddCommutive() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        String fwd = $.$("add(#x,1)").eval(t).toString();
        String rev = $.$("add(1,#x)").eval(t).toString();
        assertEquals(
                "add(#1,1)",
                fwd);
        assertEquals(
                fwd,
                rev);

    }

    @Test
    public void testAddMulIdentity() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        assertEquals("#1",
                $.$("add(#1,0)").eval(t).toString());
        assertEquals("#1",
                $.$("add(0,#1)").eval(t).toString());
        assertEquals("#1",
                $.$("mul(1,#1)").eval(t).toString());
        assertEquals("#1",
                $.$("mul(#1,1)").eval(t).toString());

    }

    final Random rng = new XoRoShiRo128PlusRandom(1);
    
    @Test
    public void test1() throws Narsese.NarseseException {
        assertEquals(
                //"((#1,add(#1,1))&&(#1<->2))",
                "((#1,add(#1,1))&&(#1<->2))",
                
                
                ArithmeticIntroduction.apply($.$("(2,3)"), true, rng).toString());
    }

    @Test public void test2() throws Narsese.NarseseException {
        assertEquals(
                "(x(#1,add(#1,1))&&(#1<->2))",
                
                ArithmeticIntroduction.apply($.$("x(2,3)"), true, rng).toString());
    }
    @Test public void test2b() throws Narsese.NarseseException {
        assertEquals(
                "(x(#1,add(#1,1))&|(#1<->2))",
                
                ArithmeticIntroduction.apply($.$("x(2,3)"), false, rng).toString());

    }

    @Test public void testEqBackSubstitution() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.termVolumeMax.set(12);
        TestNAR t = new TestNAR(n);
        //t.log();
        t.mustBelieve(100, "((#1,4)&&(#1,3))", 1f, 0.9f);
        n.input("(&&,(#1,add(#2,1)),equal(#2,3),(#1,#2)).");
        t.test();
    }
    @Test public void testSimBackSubstitution() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.termVolumeMax.set(14);
        TestNAR t = new TestNAR(n);
        //t.mustBelieve(100, "((#1,4)&&(#1,3))", 1f, 0.9f);
        //n.input("(&&,(#1,add(#2,1)),(#1,#2),(#2 <-> 3)).");
        n.input("(&&,(#1,#2),(#2 <-> 3)).");
        t.mustBelieve(100, "(#1,3)", 1f, 0.81f);
        t.test();
    }

    @Test public void testSimBackSubstitution2() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.termVolumeMax.set(14);
        TestNAR t = new TestNAR(n);
        n.input("(&&,(#1,add(#2,1)),(#1,#2),(#2 <-> 3)).");
        t.mustBelieve(100, "((#1,4)&&(#1,3))", 1f, 0.81f);
        t.test();
    }

    @Test
    public void testCompleteAddInduction() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        new ArithmeticIntroduction(8, n);

        TestNAR t = new TestNAR(n);
        t.confTolerance(0.8f);
        n.termVolumeMax.set(12);
        //t.log();


//        t.believe("(a,1)");
        t.believe("(a,2)");
        t.believe("(a,3)");
        t.believe("(a,4)");
//        t.ask("(a,5)");
//        t.mustBelieve(1000,
//
//
//                "((a,add($1,1))==>(#1<->4))",
//                1f, 0.81f);
        t.mustBelieve(500, "(a,5)", 1f, 0.5f);
        t.test();
    }

}