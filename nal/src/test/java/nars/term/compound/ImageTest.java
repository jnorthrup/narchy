package nars.term.compound;

import nars.$;
import nars.term.Term;
import nars.term.atom.Int;
import nars.term.util.Image;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageTest {

    @Test
    void testNormlizeExt() {
        Assertions.assertEquals(
                "reaction(acid,base)",
                Image.imageNormalize($$("(acid --> (reaction,/,base))")).toString()
        );
    }

    @Test
    void testNormlizeInt() {
        assertEquals(
                "(neutralization-->(acid,base))",
                Image.imageNormalize($$("((neutralization,\\,base) --> acid)")).toString()
        );
    }

    @Test
    void testCanNotNormlizeIntExt() {
        assertEquals(
                "((neutralization,\\,base)-->(reaction,/,base))",
                Image.imageNormalize($$("((neutralization,\\,base) --> (reaction,/,base))")).toString()
        );
    }

    @Test
    void testNormalizeSubtermsSIM() {

        assertEquals(
                "(reaction(acid,base)<->x)",
                $$("(x <-> (acid --> (reaction,/,base)))").toString()
        );
    }

    @Test
    void testImageIntWithNumbers() {
        String x = "((0,1,0)-->bitmap)";
        Term xx = $$(x);

        assertEquals("(0-->(bitmap,/,1,/))",
                Image.imageExt(xx, Int.the(0)).toString());

        assertEquals($$("(0-->(bitmap,/,1,/))"),
                Image.imageExt(xx, Int.the(0)));

        assertEquals("(1-->(bitmap,0,/,0))",
                Image.imageExt(xx, Int.the(1)).toString());

        assertEquals(xx, Image.imageNormalize(Image.imageExt(xx, Int.the(1))));
        assertEquals(xx, Image.imageNormalize(Image.imageExt(xx, Int.the(0))));

    }

    @Test
    void testImagizeDepVar() {
        Term x = $$("reaction(acid,#1)");
        Term y = Image.imageExt(x, $.varDep(1));
        assertEquals("(#1-->(reaction,acid,/))", y.toString());
        assertEquals(x, Image.imageNormalize(y));
    }
}