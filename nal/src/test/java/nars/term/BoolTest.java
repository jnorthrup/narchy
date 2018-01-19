package nars.term;

import nars.*;
import org.junit.jupiter.api.Test;

import static nars.Op.*;
import static nars.term.TermReductionsTest.assertReduction;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Bool and Tautology tests */
public class BoolTest {


    @Test
    public void testNegationTautologies() {
        assertEquals(True, True.unneg());
        assertEquals(False, True.neg());
        assertEquals(True, False.unneg());
        assertEquals(True, False.neg());
        assertEquals(Null, Null.neg());
        assertEquals(Null, Null.unneg());
    }

    @Test public void testStatementTautologies()  {
        for (Op o : new Op[]{INH, SIM, IMPL}) {
            assertEquals(True, o.the(True, True));
            assertEquals(True, o.the(False, False));
            assertEquals(Null, o.the(Null, Null));
        }

        assertEquals("(x-->†)", INH.the(x, True).toString());
        assertEquals("((--,x)-->†)", INH.the(x.neg(), True).toString());
    }

    @Test public void testInheritanceTaskReduction() throws Narsese.NarseseException {
        {
            NAR n = NARS.shell(); //HACK separate NAR to prevent revision
            //HACK using "true:true" to produce True, since i forget if True/False has a parse
            Task aIsTrue = n.inputTask("(a-->true:true).");
            assertEquals("$.50 a. %1.0;.90%", aIsTrue.toString());
        }

        {
            NAR n = NARS.shell(); //HACK separate NAR to prevent revision
            Task aIsFalse = n.inputTask("(a --> --(true-->true)).");
            assertEquals("$.50 a. %0.0;.90%", aIsFalse.toString());
        }

        {
            NAR n = NARS.shell(); //HACK separate NAR to prevent revision
            Task notAIsFalse = n.inputTask("(--a --> --(true-->true)).");
            assertEquals("$.50 a. %1.0;.90%", notAIsFalse.toString());
        }

        {
            NAR n = NARS.shell(); //HACK separate NAR to prevent revision
            Task notAIsntFalse = n.inputTask("--(--a --> --(true-->true)).");
            assertEquals("$.50 a. %0.0;.90%", notAIsntFalse.toString());
        }
    }

    @Test
    public void testImplicationTautologies() {
        assertEquals("x", IMPL.the(True, x).toString());
        assertEquals("(--,x)", IMPL.the(False, x).toString());
        assertEquals(Null, IMPL.the(Null, x));
        assertEquals(Null, IMPL.the(x, True));
        assertEquals(Null, IMPL.the(x, False));
        assertEquals(Null, IMPL.the(x, Null));
    }

    @Test
    public void testConjTautologies() {
        assertEquals("x", CONJ.the(True, x).toString());
        assertEquals(False, CONJ.the(False, x));
        assertEquals(False, CONJ.the(False, True));
        assertEquals(True, CONJ.the(True, True));
        assertEquals(False, CONJ.the(False, False));
        assertEquals(Null, CONJ.the(Null, x));
        assertEquals(Null, CONJ.the(Null, Null));
    }


    @Test
    public void testDiffTautologies() {
        for (Op o : new Op[] { DIFFe, DIFFi } ) {

            String diff = o.str;

            //raw
            assertReduction(False, "(x" + diff + "x)");
            assertReduction("(x" + diff + "(--,x))", "(x" + diff + "(--,x))");  //unchanged

            //subj
            assertReduction(Null, "((x" + diff + "x)-->y)");
            assertReduction(Null, "(--(x" + diff + "x)-->y)");
            assertReduction("((x" + diff + "(--,x))-->y)", "((x" + diff + "(--,x))-->y)"); //unchanged
            assertReduction("(((--,x)" + diff + "x)-->y)", "(((--,x)" + diff + "x)-->y)"); //unchanged

            //pred
            assertReduction("(y-->Ⅎ)",  "(y --> (x" + diff + "x))");
            assertReduction("(y-->†)",  "(y --> --(x" + diff + "x))");
            assertReduction("(y-->(x" + diff + "(--,x)))", "(y-->(x" + diff + "(--,x)))"); //unchanged
            assertReduction("(y-->((--,x)" + diff + "x))", "(y-->((--,x)" + diff + "x))"); //unchanged


            assertEquals(False, o.the(x,x));
            assertEquals(Null, o.the(x,False));
            assertEquals(Null, o.the(x,True));

        }
    }

    @Test
    public void testIntersectionTautologies() {
        for (Op o : new Op[] { SECTe, SECTi } ) {

            String sect = o.str;

            //raw
            assertEquals(x, o.the(x, x));
            assertReduction("((--,x)" + sect + "x)", o.the(x, x.neg())); //unchanged

            assertEquals(x, o.the(x, True));
            assertEquals(False, o.the(x, False));
            assertEquals(Null, o.the(x, Null));
        }
    }

    @Test
    public void testSetTautologies() {
        //TODO
    }

    static final Term x = $.$safe("x");
    static final Term y = $.$safe("y");

}
