package nars.concept.dynamic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicDiffTest {
    @Test
    void testRawDifference() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("x", 0.75f, 0.50f);
        n.believe("y", 0.25f, 0.50f);
        n.run(1);
        Term xMinY = $("(x ~ y)");
        Term yMinX = $("(y ~ x)");
        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(xMinY).beliefs().getClass());
        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(yMinX).beliefs().getClass());
        assertEquals(
                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
        );
        assertEquals(
                "%.06;.25%", n.beliefTruth(yMinX, n.time()).toString()
        );

    }

    @Test
    void testDiffE() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("c:x", 0.75f, 0.50f);
        n.believe("c:y", 0.25f, 0.50f);
        n.run(1);
        Term xMinY = $("c:(x ~ y)");
        Term yMinX = $("c:(y ~ x)");
        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(xMinY).beliefs().getClass());
        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(yMinX).beliefs().getClass());
        assertEquals(
                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
        );
        assertEquals(
                "%.06;.25%", n.beliefTruth(yMinX, n.time()).toString()
        );

    }

}
