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
    void testImageIdentity() throws Narsese.NarseseException {
        String x = "((x,y)-->z)";
        Term t = $$(x);
        n.believe(x, 0.75f, 0.50f);
        n.run(1);


        Term i1 = $("(y --> (z,x,/))");
        assertEquals(t, Image.imageNormalize(i1));
        Term i2 = $("(x --> (z,/,y))");
        assertEquals(t, Image.imageNormalize(i2));

        assertDynamicTable(i1);
        assertDynamicTable(i2);

        assertEquals(
                "%.75;.50%", n.beliefTruth(i1, n.time()).toString()
        );
        assertEquals(
                "(y-->(z,x,/)). %.75;.50%", n.match(i1, BELIEF, n.time()).toStringWithoutBudget()
        );
        assertEquals(
                "(x-->(z,/,y)). %.75;.50%", n.match(i2, BELIEF, n.time()).toStringWithoutBudget()
        );
    }
}
