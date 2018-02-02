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

    final Random rng = new XoRoShiRo128PlusRandom(1);

    @Test
    public void test1() throws Narsese.NarseseException {
        assertEquals("((#1,add(#1,1))&&(#1<->2))",
                ArithmeticIntroduction.apply($.$("(2,3)"), rng).toString());
    }

    @Test public void test2() throws Narsese.NarseseException {
        assertEquals("(x(#1,add(#1,1))&&(#1<->2))",
                ArithmeticIntroduction.apply($.$("x(2,3)"), rng).toString());
    }

    @Test
    public void testAdd() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        assertEquals("2",
                $.$("add(1,1)").eval(t.terms).toString());
        assertEquals("1",
                $.$("add(2,-1)").eval(t.terms).toString());
    }
    @Test
    public void testAddCommutive() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        String fwd = $.$("add(#x,1)").eval(t.terms).toString();
        String rev = $.$("add(1,#x)").eval(t.terms).toString();
        assertEquals(
                "add(#1,1)",
                fwd.toString());
        assertEquals(
                fwd.toString(),
                rev.toString());

    }

    @Test
    public void testAddMulIdentity() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        assertEquals("#1",
                $.$("add(#1,0)").eval(t.terms).toString());
        assertEquals("#1",
                $.$("add(0,#1)").eval(t.terms).toString());
        assertEquals("#1",
                $.$("mul(1,#1)").eval(t.terms).toString());
        assertEquals("#1",
                $.$("mul(#1,1)").eval(t.terms).toString());

    }
    @Test
    public void testCompleteAddInduction() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        TestNAR t = new TestNAR(n);
        t.log();
//        t.believe("(x:1,x:2)");
//        t.believe("(x:2,x:3)");
//        t.believe("(x:3,x:4)");
//        t.believe("(x:4,x:5)");
//            t.ask("(x:5,?1)");
//        t.mustBelieve(1000, "(x:5,x:6)", 1f, 0.81f);
        t.believe("a:1");
        t.believe("a:2");
        t.believe("a:3");
        t.believe("a:4");
        t.ask("#x:5");
        t.mustBelieve(1000, "a:5", 1f, 0.81f);
        t.test(true);
    }

}