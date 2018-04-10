package nars.term.compound.util;

import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageTest {

    @Test
    public void testNormlizeExt() {
        assertEquals(
                "reaction(acid,base)",
                Image.imageNormalize($$("(acid --> (reaction,/,base))")).toString()
        );
    }

    @Test
    public void testNormlizeInt() {
        assertEquals(
                "(neutralization-->(acid,base))",
                Image.imageNormalize($$("((neutralization,\\,base) --> acid)")).toString()
        );
    }

    @Test
    public void testCanNotNormlizeIntExt() {
        assertEquals(
                "((neutralization,\\,base)-->(reaction,/,base))",
                Image.imageNormalize($$("((neutralization,\\,base) --> (reaction,/,base))")).toString()
        );
    }

    @Test
    public void testNormlizeSubterms() {
        //implicit transformation to image normal form
        assertEquals(
                "(reaction(acid,base)<->x)",
                $$("(x <-> (acid --> (reaction,/,base)))").toString()
        );
    }

}