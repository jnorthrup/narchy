package nars.concept.dynamic;

import nars.*;
import nars.concept.Concept;
import nars.table.dynamic.DynamicTruthTable;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicSectTest {
    @Test
    void testDynamicIntersection() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:(--,y)", 0f, 0.9f);
        n.believe("a:z", 0f, 0.9f);
        n.believe("a:(--,z)", 1f, 0.9f);
        n.believe("x:b", 1f, 0.9f);
        n.believe("y:b", 1f, 0.9f);
        n.believe("z:b", 0f, 0.9f);
        n.run(2);


        Task k = n.match($("((x|y)-->a)"), BELIEF, 0);
        assertEquals("((x|y)-->a)", k.term().toString());
        assertEquals(1f, k.truth().freq());

        Task withNeg = n.match($("((x|--y)-->a)"), BELIEF, 0);
        assertEquals("(((--,y)|x)-->a)", withNeg.term().toString());
        assertEquals(0f, withNeg.truth().freq());

        for (long now: new long[]{0, n.time() /* 2 */, ETERNAL}) {

            assertTrue(n.conceptualize($("((x|y)-->a)")).beliefs()
                    .tableFirst(DynamicTruthTable.class)!=null);


            assertEquals($.t(1f, 0.81f), n.beliefTruth("((x|y)-->a)", now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("((x|z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x&z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b-->(x|y))")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b-->(x|z))")), now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("(b-->(x&z))")), now));

            Concept xIntNegY = n.conceptualize($("((x|--y)-->a)"));
            assertTrue(xIntNegY.beliefs().tableFirst(DynamicTruthTable.class)!=null);
            assertTrue(xIntNegY.goals().tableFirst(DynamicTruthTable.class)!=null);
            assertEquals($.t(0f, 0.81f), n.beliefTruth(xIntNegY, now), now + " " + xIntNegY);
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x|--z)-->a)")), now));
        }
    }
}
