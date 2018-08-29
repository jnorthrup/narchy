package nars.concept.dynamic;

import nars.Narsese;
import nars.term.Term;
import nars.term.util.Image;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicImageTest extends AbstractDynamicTaskTest {

    @Test
    void testImageExtIdentity() throws Narsese.NarseseException {
        assertImageIdentity("((x,y)-->z)", "(y --> (z,x,/))", "(x --> (z,/,y))");
    }
    @Test
    void testImageIntIdentity() throws Narsese.NarseseException {
        assertImageIdentity("(z-->(x,y))", "((z,x,\\)-->y)", "((z,\\,y)-->x)");
    }

    private void assertImageIdentity(String x, String x1, String x2) throws Narsese.NarseseException {
        Term t = $$(x);

        Term i1 = $(x1);
        assertEquals(t, Image.imageNormalize(i1));
        Term i2 = $(x2);
        assertEquals(t, Image.imageNormalize(i2));

        n.believe(x, 0.75f, 0.50f);

        assertDynamicTable(i1);
        assertDynamicTable(i2);

        long when = n.time();
        assertEquals(
                "%.75;.50%", n.beliefTruth(i1, when).toString()
        );
        assertEquals(
                i1 + ". %.75;.50%", n.answer(i1, BELIEF, when).toStringWithoutBudget()
        );
        assertEquals(
                i2 + ". %.75;.50%", n.answer(i2, BELIEF, when).toStringWithoutBudget()
        );
    }
}
