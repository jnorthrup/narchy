package nars.concept.dynamic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.*;

class DynamicDiffTest extends AbstractDynamicTaskTest {

//    @Test
//    void testRawDifference() throws Narsese.NarseseException {
//        NAR n = NARS.shell();
//        n.believe("x", 0.75f, 0.50f);
//        n.believe("y", 0.25f, 0.50f);
//        n.run(1);
//        Term xMinY = $("(x ~ y)");
//        Term yMinX = $("(y ~ x)");
//        assertDynamicTable(xMinY);
//        assertDynamicTable(yMinX);
//        assertEquals(
//                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
//        );
//        assertEquals(
//                "%.06;.25%", n.beliefTruth(yMinX, n.time()).toString()
//        );
//
//    }

    @Test void testDiffUnion() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("c:x", 0.75f, 0.50f);
        n.believe("c:y", 0.25f, 0.50f);
        n.run(1);
        Term xMinY = $("c:(x ~ y)"), yMinX = $("c:(y ~ x)");
        assertNotNull(((BeliefTables) n.conceptualize(xMinY).beliefs()).tableFirst(DynamicTruthTable.class));
        assertNotNull(((BeliefTables) n.conceptualize(yMinX).beliefs()).tableFirst(DynamicTruthTable.class));
        assertEquals(
                "%.94;.25%", n.beliefTruth(xMinY, n.time()).toString()
        );
        assertEquals(
                "%.44;.25%", n.beliefTruth(yMinX, n.time()).toString()
        );
    }
    @Test void testDiffIntersection() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("c:x", 0.75f, 0.50f);
        n.believe("c:y", 0.25f, 0.50f);
        n.run(1);
        Term xMinY = $("c:(x - y)"), yMinX = $("c:(y - x)");
        assertNotNull(((BeliefTables) n.conceptualize(xMinY).beliefs()).tableFirst(DynamicTruthTable.class));
        assertNotNull(((BeliefTables) n.conceptualize(yMinX).beliefs()).tableFirst(DynamicTruthTable.class));
        assertEquals(
                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
        );
        assertEquals(
                "%.06;.25%", n.beliefTruth(yMinX, n.time()).toString()
        );
    }

    @Test
    void testEviDilution() {
        NAR n = NARS.shell();
        n.believe("c:x", 0.75f, 0.50f, 0, 0);
        n.believe("c:y", 0.25f, 0.50f, 1, 1);
        n.believe("c:z", 0.25f, 0.50f, 2, 2);

        Task a = n.answerBelief($$("c:(x~y)"), 0, 1);
        assertNotNull(a);
        Task b = n.answerBelief($$("c:(x~z)"), 0, 2);
        assertNotNull(b);
        assertEquals(b.freq(), a.freq());
        assertTrue(b.conf() < a.conf());
        assertTrue(b.pri() < a.pri());

//        assertEquals("", a.toStringWithoutBudget());
//        assertEquals("", b.toStringWithoutBudget());


    }
}