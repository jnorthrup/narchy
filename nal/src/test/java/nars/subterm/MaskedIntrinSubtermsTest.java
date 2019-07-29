package nars.subterm;

import nars.$;
import nars.term.anon.Intrin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MaskedIntrinSubtermsTest {

    @Test
    void test1() {
        IntrinSubterms a = new IntrinSubterms(Intrin.term(1), Intrin.term(2));
        MaskedIntrinSubterms.SubtermsMaskedIntrinSubterms ab = new MaskedIntrinSubterms.SubtermsMaskedIntrinSubterms(a, new ArrayTermVector($.the("a"), $.the("b")));
        MaskedIntrinSubterms.SubtermsMaskedIntrinSubterms xy = new MaskedIntrinSubterms.SubtermsMaskedIntrinSubterms(a, new ArrayTermVector($.the("x"), $.the("y")));
        assertEquals("(a,b)", ab.toString());
        assertEquals("(x,y)", xy.toString());
        assertEquals(ab, ab);
        assertNotEquals(ab, xy);
    }
}