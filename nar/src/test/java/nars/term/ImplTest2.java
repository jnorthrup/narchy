package nars.term;

import nars.Narsese;
import org.junit.jupiter.api.Test;

import static nars.$.$;

public class ImplTest2 {
    @Test
    void testCoNegatedImpl() {
        TermTestMisc.assertValidTermValidConceptInvalidTaskContent(("(--x ==> x)."));
//        TermTestMisc.assertValidTermValidConceptInvalidTaskContent(("(--x =|> x)."));
    }

    @Test
    void testCoNegatedImplOK() throws Narsese.NarseseException {
        TermTestMisc.assertValid($("((--,(a)) ==>+1 (a))"));
        TermTestMisc.assertValid($("((--,a) ==>+1 a)"));
    }

}
