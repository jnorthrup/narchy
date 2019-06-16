package nars.concept.dynamic;

import nars.$;
import nars.Narsese;
import nars.Task;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

class DynamicImplTest extends AbstractDynamicTaskTest {

    @Test void testTables() {
        assertDynamicTable("(x==>y)");
        assertDynamicTable("(y==>x)");
        assertDynamicTable("(--y==>x)");
        assertDynamicTable("(--x==>y)");
    }

    @Test
    void testEternalPosPos() throws Narsese.NarseseException {
        n.input("x.");
        n.input("y.");
        assertNull(n.beliefTruth("(--x==>y)", ETERNAL)); //no evidence
        assertEquals($.t(1, 0.45f), n.beliefTruth("(x==>y)", ETERNAL));
    }

    @Test
    void testEternalNegPos() throws Narsese.NarseseException {
        n.input("--x.");
        n.input("y.");
        assertEquals($.t(1, 0.45f), n.beliefTruth("(--x==>y)", ETERNAL));
        assertNull(n.beliefTruth("(x==>y)", ETERNAL)); //no evidence
    }

    @Test
    void testEternalPosNeg() throws Narsese.NarseseException {
        n.input("x.");
        n.input("--y.");
        assertEquals($.t(0, 0.45f), n.beliefTruth("(x==>y)", ETERNAL));
        assertNull(n.beliefTruth("(--x==>y)", ETERNAL)); //no evidence
    }
    @Test
    void testEternalNegNeg() throws Narsese.NarseseException {
        n.input("--x.");
        n.input("--y.");
        assertEquals($.t(0, 0.45f), n.beliefTruth("(--x==>y)", ETERNAL));
        assertNull(n.beliefTruth("(x==>y)", ETERNAL)); //no evidence
    }

    @Test
    void testEternalPosConjPosPos() throws Narsese.NarseseException {
        n.input("x1.");
        n.input("x2.");
        n.input("y.");
        assertEquals($.t(1, 0.42f), n.beliefTruth("((x1&&x2)==>y)", ETERNAL));
        assertEquals($.t(1, 0.42f), n.beliefTruth("(y==>(x1&&x2))", ETERNAL));
    }
    @Test
    void testEternalPosConjPosNeg() throws Narsese.NarseseException {
        n.input("x1.");
        n.input("--x2.");
        n.input("y.");
        assertEquals($.t(1, 0.42f), n.beliefTruth("((x1 && --x2)==>y)", ETERNAL));
        assertEquals($.t(1, 0.42f), n.beliefTruth("(y==>(x1 && --x2))", ETERNAL));
        assertEquals($.t(0, 0.42f), n.beliefTruth("(y==>(x1 &&   x2))", ETERNAL));
    }


    @Test
    void testTemporal1() throws Narsese.NarseseException {
        n.input("x. |");
        n.run(2);
        n.input("y. |");
        {
            Task t = n.belief($$("(x==>y)"), 1, 2);
            assertNotNull(t);
            System.out.println(t);
            assertEquals(1, t.start());
            assertEquals(2, t.end());
            assertEquals(2, t.stamp().length);
            assertEq("(x ==>+2 y)", t.term());
            assertEquals($.t(1, 0.41f), t.truth());
        }
        assertEquals("$.50 (x ==>+2 y). 0 %1.0;.45%", n.belief($$("(x==>y)"), 0, 0).toString());
        assertEquals("$.50 (x ==>+1 y). 0 %1.0;.34%", n.belief($$("(x ==>+1 y)"), 0, 0).toString());
    }
}
