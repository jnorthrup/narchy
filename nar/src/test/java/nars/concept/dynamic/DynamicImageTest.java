package nars.concept.dynamic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.util.Image;
import nars.term.util.ImageBeliefTable;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicImageTest extends AbstractDynamicTaskTest {

    @Test
    void testImageExtIdentityEternal() throws Narsese.NarseseException {
        assertImageIdentity(ETERNAL, "((x,y)-->z)", "(y --> (z,x,/))", "(x --> (z,/,y))");
    }
    @Test
    void testImageExtIdentityTemporal() throws Narsese.NarseseException {
        assertImageIdentity(0, "((x,y)-->z)", "(y --> (z,x,/))", "(x --> (z,/,y))");
    }
    @Test
    void testImageIntIdentityEternal() throws Narsese.NarseseException {
        assertImageIdentity(ETERNAL, "(z-->(x,y))", "((z,x,\\)-->y)", "((z,\\,y)-->x)");
    }
    @Test
    void testImageIntIdentityTemporal() throws Narsese.NarseseException {
        assertImageIdentity(0,"(z-->(x,y))", "((z,x,\\)-->y)", "((z,\\,y)-->x)");
    }

    private void assertImageIdentity(long when, String x, String x1, String x2) throws Narsese.NarseseException {
        Term t = $$(x);

        Term i1 = $(x1);
        assertEquals(t, Image.imageNormalize(i1));
        Term i2 = $(x2);
        assertEquals(t, Image.imageNormalize(i2));

        n.believe($$(x), when,0.75f, 0.50f);


        assertEquals(
                "%.75;.50%", n.beliefTruth(i1, when).toString()
        );
        String tStr = when != ETERNAL ? " " + when : "";
        assertEquals(
                i1 + "." + tStr + " %.75;.50%", n.answer(i1, BELIEF, when).toStringWithoutBudget()
        );
        assertEquals(
                i2 + "." + tStr + " %.75;.50%", n.answer(i2, BELIEF, when).toStringWithoutBudget()
        );
    }

    @Test void testInternalImageTaskTermRepresentation() {
        NAR n = NARS.tmp(4);
        Term it = $$("(x --> (y,/))");
        n.believe(it);
        Concept i = n.conceptualize(it);
        Term pt = Image.imageNormalize(it);
        Concept p = n.concept(pt);
        assertTrue(i!=p);

        assertEquals(ImageBeliefTable.class, i.beliefs().getClass());

    }
}
