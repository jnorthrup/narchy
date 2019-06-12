package nars.concept.dynamic;

import nars.$;
import nars.Narsese;
import org.junit.jupiter.api.Test;

import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DynamicImplTest extends AbstractDynamicTaskTest {

    @Test void testTables() throws Narsese.NarseseException {
        assertDynamicTable("(x==>y)");
        assertDynamicTable("(y==>x)");
        assertDynamicTable("(--y==>x)");
        assertDynamicTable("(--x==>y)");
    }

    @Test
    void testEternalPos() throws Narsese.NarseseException {
        n.input("x.");
        n.input("y.");
        assertNull(n.beliefTruth("(--x==>y)", ETERNAL)); //no evidence
        assertEquals($.t(1, 0.45f), n.beliefTruth("(x==>y)", ETERNAL));
    }

    @Test
    void testEternalNeg() throws Narsese.NarseseException {
        n.input("--x.");
        n.input("y.");
        assertEquals($.t(1, 0.45f), n.beliefTruth("(--x==>y)", ETERNAL));
        assertNull(n.beliefTruth("(x==>y)", ETERNAL)); //no evidence
    }

}
