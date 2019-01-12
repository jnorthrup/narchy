package nars.term.util.conj;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.*;

class ConjDiffTest {

    @Test
    void testConjDiff_Eliminate() {

        ConjDiff d = ConjDiff.the(
                0, $$("(a &&+5 b)"), 5, $$("(b &&+5 c)"), false);

        Term newPred = d.term();
        assertEq("c", newPred);

    }
}