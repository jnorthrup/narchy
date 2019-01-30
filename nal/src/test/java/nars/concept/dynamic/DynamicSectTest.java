package nars.concept.dynamic;

import nars.*;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicSectTest {
    @Test
    void testDynamicIntersection() throws Narsese.NarseseException {

        assertEquals("|", $$("(x~y)").op().str);
        assertEquals("&", $$("(x-y)").op().str);

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


        {
            Task k = n.answer($("((x|y)-->a)"), BELIEF, 0);
            assertEquals("((x|y)-->a)", k.term().toString());
            assertEquals(1f, k.truth().freq());
        }

        {
            Task k = n.answer($("((x~y)-->a)"), BELIEF, 0);
            assertEquals("((x~y)-->a)", k.term().toString());
            assertEquals(0f, k.truth().freq());
        }

        {
            Task k = n.answer($("((x-y)-->a)"), BELIEF, 0);
            assertEquals("((x-y)-->a)", k.term().toString());
            assertEquals(1f, k.truth().freq());
        }


        for (long now: new long[]{0, n.time() /* 2 */, ETERNAL}) {

            assertTrue(((BeliefTables)n.conceptualize($("((x|y)-->a)")).beliefs())
                    .tableFirst(DynamicTruthTable.class)!=null);


            assertEquals($.t(1f, 0.81f), n.beliefTruth("((x|y)-->a)", now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("((x|z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x&z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b-->(x|y))")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b-->(x|z))")), now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("(b-->(x&z))")), now));

//            Concept xIntNegY = n.conceptualize($("((x-y)-->a)"));
//            assertTrue(((BeliefTables)xIntNegY.beliefs()).tableFirst(DynamicTruthTable.class)!=null);
//            assertTrue(((BeliefTables)xIntNegY.goals()).tableFirst(DynamicTruthTable.class)!=null);
//            assertEquals($.t(0f, 0.81f), n.beliefTruth(xIntNegY, now), now + " " + xIntNegY);
//            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x-z)-->a)")), now));

//            Task withNeg = n.answer($("((x|--y)-->a)"), BELIEF, 0);
//            assertEquals("((x-y)-->a)", withNeg.target().toString());
//            assertEquals(0f, withNeg.truth().freq());
        }
    }
}
