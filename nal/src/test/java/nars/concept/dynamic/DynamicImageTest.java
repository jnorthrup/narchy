package nars.concept.dynamic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.table.dynamic.DynamicTruthBeliefTable;
import nars.term.Term;
import nars.term.compound.util.Image;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicImageTest {
    @Test
    void testImageIdentity() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        String x = "((x,y)-->z)";
        Term t = $$(x);
        n.believe(x, 0.75f, 0.50f);
        n.run(1);


        Term i1 = $("(y --> (z,x,/))");
        assertEquals(t, Image.imageNormalize(i1));
        Term i2 = $("(x --> (z,/,y))");
        assertEquals(t, Image.imageNormalize(i2));

        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(i1).beliefs().getClass());
        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(i2).beliefs().getClass());
        assertEquals(
                "%.75;.50%", n.beliefTruth(i1, n.time()).toString()
        );
        assertEquals(
                "$.50 (y-->(z,x,/)). %.75;.50%", n.match(i1, BELIEF, n.time()).toString()
        );
        assertEquals(
                "$.50 (x-->(z,/,y)). %.75;.50%", n.match(i2, BELIEF, n.time()).toString()
        );
    }
}
