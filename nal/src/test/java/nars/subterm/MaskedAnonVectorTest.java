package nars.subterm;

import nars.$;
import nars.term.anon.Anom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MaskedAnonVectorTest {

    @Test
    void test1() {
        AnonVector a = new AnonVector(Anom.term(1), Anom.term(2));
        MaskedAnonVector.SubtermsMaskedAnonVector ab = new MaskedAnonVector.SubtermsMaskedAnonVector(a, new ArrayTermVector($.the("a"), $.the("b")));
        MaskedAnonVector.SubtermsMaskedAnonVector xy = new MaskedAnonVector.SubtermsMaskedAnonVector(a, new ArrayTermVector($.the("x"), $.the("y")));
        assertEquals("(a,b)", ab.toString());
        assertEquals("(x,y)", xy.toString());
        assertEquals(ab, ab);
        assertNotEquals(ab, xy);
    }
}