package nars.concept.dynamic;

import nars.*;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicImplTest {
    @Test
    void testDynamicImplSubj() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("(x ==> a)", 1f, 0.9f);
        n.believe("(y ==> a)", 1f, 0.9f);
        //            n.believe("a:y", 1f, 0.9f);
        //            n.believe("a:(--,y)", 0f, 0.9f);
        n.believe("(z ==> a)", 0f, 0.9f);
        n.believe("(--z ==> a)", 0.75f, 0.9f);
        //            n.believe("a:(--,z)", 1f, 0.9f);
        //            n.believe("x:b", 1f, 0.9f);
        //            n.believe("y:b", 1f, 0.9f);
        //            n.believe("z:b", 0f, 0.9f);
        n.run(2);
        for (long now: new long[]{0, n.time() /* 2 */, ETERNAL}) {
            Term pp = $$("((x && y) ==> a)");
            Term pn = $$("((x && z) ==> a)");

            assertTrue(n.conceptualize(pp).beliefs() instanceof DynamicTruthBeliefTable);
            assertEquals($.t(1f, 0.81f), n.beliefTruth(pp, now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(pn, now));

            Term Npp = $$("(--(x && y) ==> a)");
            assertEquals(null, n.beliefTruth(Npp, now));

            Term pnn = $$("((x && --z) ==> a)");
            assertEquals($.t(0.75f, 0.81f), n.beliefTruth(pnn, now));


            //                assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x&z)-->a)")), now));
        }

        {
            Term pt_p = $$("((x &&+1 y) ==>+1 a)");
            Task pt_p_Task = n.match(pt_p, BELIEF, 0);
            assertEquals("((x &&+1 y) ==>+1 a)", pt_p_Task.term());
        }

        Term p_tp = $$("((x && y) ==>+1 a)");
        Term pttp = $$("((x &&+1 y) ==>+1 a)");


    }
}
