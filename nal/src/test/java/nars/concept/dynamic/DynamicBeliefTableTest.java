package nars.concept.dynamic;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Int;
import nars.truth.Truth;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 10/27/16.
 */
public class DynamicBeliefTableTest {

    @Test
    public void testDynamicConjunction2() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("b:x", 0f, 0.9f);
        n.run(1);
        long now = n.time();
        Concept axANDay = n.conceptualize($("(a:x && a:y)"));
        assertEquals($.t(1f, 0.81f), n.beliefTruth(axANDay, now));
        assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("(b:x && a:y)")), now));
        assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("(a:x && (--,a:y))")), now));
        assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((--,b:x) && a:y)")), now));
        assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("((--,b:x) && (--,a:y))")), now));
    }

    @Test
    public void testDynamicIntersection() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:(--,y)", 0f, 0.9f);
        n.believe("a:z", 0f, 0.9f);
        n.believe("a:(--,z)", 1f, 0.9f);
        n.believe("x:b", 1f, 0.9f);
        n.believe("y:b", 1f, 0.9f);
        n.believe("z:b", 0f, 0.9f);
        n.run(2);
        for (long now : new long[]{0, n.time() /* 2 */, ETERNAL}) {
            assertTrue(n.conceptualize($("((x|y)-->a)")).beliefs() instanceof DynamicBeliefTable);
            assertEquals($.t(1f, 0.81f), n.beliefTruth("((x|y)-->a)", now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("((x|z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x&z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b --> (x|y))")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b --> (x|z))")), now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("(b --> (x&z))")), now));

            Concept xIntNegY = n.conceptualize($("((x|--y)-->a)"));
            assertTrue(xIntNegY instanceof DynamicConcept);
            assertEquals($.t(0f, 0.81f), n.beliefTruth(xIntNegY, now), now + " " + xIntNegY);
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x|--z)-->a)")), now));
        }
    }

    @Test
    public void testDynamicIntRange() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.believe("x:1", 1f, 0.9f);
        n.believe("x:2", 0.5f, 0.9f);
        n.believe("x:3", 0f, 0.9f);
        n.run(1);

        Concept x12 = n.conceptualize($.inh(Int.range(1, 2), $.the("x")));
        Concept x23 = n.conceptualize($.inh(Int.range(2, 3), $.the("x")));
        Concept x123 = n.conceptualize($.inh(Int.range(1, 3), $.the("x")));
        assertEquals("%.50;.81%", n.beliefTruth(x12, ETERNAL).toString());
        assertEquals("%0.0;.81%", n.beliefTruth(x23, ETERNAL).toString());
        assertEquals("%0.0;.73%", n.beliefTruth(x123, ETERNAL).toString());
    }
    @Test
    public void testDynamicIntVectorRange() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.believe("x(1,1)", 1f, 0.9f);
        n.believe("x(1,2)", 0.5f, 0.9f);
        n.believe("x(1,3)", 0f, 0.9f);
        n.run(1);

        Term t12 = $.inh($.p(Int.the(1), Int.range(1, 2)), $.the("x"));
        assertEquals("x(1,1..2)", t12.toString());
        Concept x12 = n.conceptualize(t12);
        assertTrue(x12.beliefs() instanceof DynamicBeliefTable);

        Concept x23 = n.conceptualize($.inh($.p(Int.the(1), Int.range(2, 3)), $.the("x")));
        Concept x123 = n.conceptualize($.inh($.p(Int.the(1), Int.range(1, 3)), $.the("x")));
        assertEquals("%.50;.81%", n.beliefTruth(x12, ETERNAL).toString());
        assertEquals("%0.0;.81%", n.beliefTruth(x23, ETERNAL).toString());
        assertEquals("%0.0;.73%", n.beliefTruth(x123, ETERNAL).toString());
    }



    @Test
    public void testDynamicConjunction3() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 1f, 0.9f);
        n.run(1);

        BaseConcept cc = ((BaseConcept) n.conceptualize($("(&&, a:x, a:y, a:z)")));
        Truth now = n.beliefTruth(cc, n.time());
        assertNotNull(now);
        assertTrue($.t(1f, 0.73f).equals(now, 0.1f), now + " truth at " + n.time());
        //the truth values were provided despite the belief tables being empty:
        assertTrue(cc.beliefs().isEmpty());

        //test unknown:
        {
            BaseConcept ccn = (BaseConcept) n.conceptualize($("(&&, a:x, a:w)"));
            Truth nown = n.beliefTruth(ccn, n.time());
            assertNull(nown);
        }

        //test negation:
        Concept ccn = n.conceptualize($("(&&, a:x, (--, a:y), a:z)"));
        Truth nown = n.beliefTruth(ccn, n.time());
        assertTrue($.t(0f, 0.73f).equals(nown, 0.1f));

        n.clear();

        //test change after a component's revision:
        n.believe("a:y", 0, 0.95f);
        n.run(1);
        Truth now2 = n.beliefTruth(n.conceptualize($("(&&, a:x, a:y, a:z)")), n.time());
        assertTrue(now2.freq() < 0.4f);

    }

    @Test
    public void testDynamicConjunction2Temporal() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.believe($("(x)"), (long) 0, 1f, 0.9f);
        n.believe($("(y)"), (long) 4, 1f, 0.9f);
        n.run(2);
        n.time.dur(8);
        BaseConcept cc = (BaseConcept) n.conceptualize($("((x) && (y))"));

        DynamicBeliefTable xtable = (DynamicBeliefTable) (cc.beliefs());
        Compound template = $("((x) &&+4 (y))");

        DynTruth xt = xtable.truth(0, 0, template, true, n);
        assertNotNull(xt);
        assertTrue($.t(1f, 0.81f).equals(xt.truth(), 0.1f), xt.truth().toString());

        assertEquals(0.74f, xtable.generate($("((x) &&+6 (y))"), 0, 0, n).conf(), 0.05f);
        assertEquals(0.81f, xtable.generate($("((x) &&+4 (y))"), 0, 0, n).conf(), 0.05f); //best match to the input
        assertEquals(0.75f, xtable.generate($("((x) &&+2 (y))"), 0, 0, n).conf(), 0.05f);
        assertEquals(0.75f, xtable.generate($("((x) &&+0 (y))"), 0, 0, n).conf(), 0.05f);
        assertEquals(0.62f, xtable.generate($("((x) &&-32 (y))"), 0, 0, n).conf(), 0.1f);


    }


}