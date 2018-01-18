package nars.term;

import nars.$;
import nars.Op;
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

    @Test public void testStatementTautologies() {
        for (Op o : new Op[] { INH, SIM, IMPL }) {
            assertEquals(True, o.the(True, True));
            assertEquals(True, o.the(False, False));
            assertEquals(Null, o.the(Null, Null));
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
            assertReduction(True, "(x" + diff + "(--,x))");

            //subj
            assertReduction(Null, "((x" + diff + "x)-->a)");
            assertReduction(Null, "((x" + diff + "(--,x))-->a)");
            assertReduction(Null, "(((--,x)" + diff + "x)-->a)");

            //pred
            assertReduction("(a-->Ⅎ)", "(a-->(x" + diff + "x))");
            assertReduction("(a-->†)", "(a-->(x" + diff + "(--,x)))");
            assertReduction("(a-->†)", "(a-->((--,x)" + diff + "x))");


            assertEquals(False, o.the(x,x));
            assertEquals(Null, o.the(x,False));
            assertEquals(Null, o.the(x,True));

        }
    }

    @Test
    public void testIntersectionTautologies() {
        //TODO
    }

    @Test
    public void testSetTautologies() {
        //TODO
    }

    static final Term x = $.$safe("x");

}
